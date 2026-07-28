use std::{
    io,
    net::{Ipv4Addr, SocketAddrV4, TcpListener},
    num::NonZeroUsize,
    sync::{
        atomic::{AtomicBool, AtomicU64, Ordering},
        Arc, Mutex,
    },
};

use axum::{
    body::{boxed, Body},
    extract::{Path, State},
    http::{
        header::{ACCEPT_RANGES, CONTENT_LENGTH, CONTENT_RANGE, CONTENT_TYPE, RANGE},
        HeaderMap, HeaderValue, StatusCode,
    },
    response::{IntoResponse, Response},
    routing::get,
    Router,
};
use bytes::Bytes;
use lru::LruCache;
use rand::{rngs::OsRng, RngCore};
use storage_backend::{ByteRange, StorageBackend};
use tokio::sync::oneshot;

use crate::error::{BError, BResult};

const BLOCK_SIZE: u64 = 256 * 1024;
const CACHE_BLOCKS: usize = 32;

#[derive(Debug, Clone, Copy, Default, uniffi::Record)]
pub struct PlaybackRangeStats {
    pub remote_requests: u64,
    pub remote_bytes: u64,
}

#[derive(uniffi::Object)]
pub struct PlaybackSession {
    url: String,
    source: Arc<PlaybackSource>,
    shutdown: Mutex<Option<oneshot::Sender<()>>>,
}

#[uniffi::export]
impl PlaybackSession {
    pub fn url(&self) -> String {
        self.url.clone()
    }

    pub fn stats(&self) -> PlaybackRangeStats {
        PlaybackRangeStats {
            remote_requests: self.source.remote_requests.load(Ordering::Relaxed),
            remote_bytes: self.source.remote_bytes.load(Ordering::Relaxed),
        }
    }

    pub fn shutdown(&self) {
        self.close_internal();
    }
}

impl Drop for PlaybackSession {
    fn drop(&mut self) {
        self.close_internal();
    }
}

impl PlaybackSession {
    fn close_internal(&self) {
        if let Some(shutdown) = self.shutdown.lock().unwrap().take() {
            self.source.active.store(false, Ordering::Release);
            let _ = shutdown.send(());
            let source = self.source.clone();
            std::mem::drop(async_runtime::tokio_runtime().spawn(async move {
                if source.backend.release(source.path.clone()).await.is_err() {
                    tracing::debug!("failed to release playback source reader");
                }
            }));
        }
    }
}

struct PlaybackSource {
    backend: Arc<dyn StorageBackend + Send + Sync>,
    path: String,
    total_size: u64,
    content_type: String,
    token: String,
    active: AtomicBool,
    cache: Mutex<LruCache<u64, Bytes>>,
    remote_requests: AtomicU64,
    remote_bytes: AtomicU64,
}

impl PlaybackSource {
    async fn block(&self, block_start: u64) -> Result<Bytes, String> {
        if let Some(bytes) = self.cache.lock().unwrap().get(&block_start).cloned() {
            return Ok(bytes);
        }

        let block_end = block_start
            .saturating_add(BLOCK_SIZE - 1)
            .min(self.total_size - 1);
        let response = self
            .backend
            .get_range_response(
                self.path.clone(),
                ByteRange::new(block_start, block_end).map_err(|error| error.to_string())?,
            )
            .await
            .map_err(|error| error.to_string())?;
        if response.total_size != self.total_size {
            return Err(format!(
                "remote size changed from {} to {}",
                self.total_size, response.total_size
            ));
        }
        self.remote_requests.fetch_add(1, Ordering::Relaxed);
        self.remote_bytes
            .fetch_add(response.bytes.len() as u64, Ordering::Relaxed);
        tracing::info!(
            start = block_start,
            end = block_end,
            bytes = response.bytes.len(),
            "playback remote range"
        );
        self.cache
            .lock()
            .unwrap()
            .put(block_start, response.bytes.clone());
        Ok(response.bytes)
    }
}

pub async fn start_playback_gateway(
    backend: Arc<dyn StorageBackend + Send + Sync>,
    path: String,
) -> BResult<Arc<PlaybackSession>> {
    let probe = backend
        .get_range_response(path.clone(), ByteRange::new(0, 0)?)
        .await?;
    if probe.total_size == 0 {
        return Err(BError::AssetLoadFail(
            "playback source is empty".to_string(),
        ));
    }

    let token = random_token();
    let inferred_content_type = content_type_for_path(&path);
    let content_type = probe
        .content_type
        .filter(|value| !is_generic_content_type(value))
        .unwrap_or_else(|| inferred_content_type.to_string());
    let source = Arc::new(PlaybackSource {
        backend,
        path: path.clone(),
        total_size: probe.total_size,
        content_type,
        token: token.clone(),
        active: AtomicBool::new(true),
        cache: Mutex::new(LruCache::new(NonZeroUsize::new(CACHE_BLOCKS).unwrap())),
        remote_requests: AtomicU64::new(1),
        remote_bytes: AtomicU64::new(probe.bytes.len() as u64),
    });

    let server_source = source.clone();
    let (ready_tx, ready_rx) = oneshot::channel();
    let (shutdown_tx, shutdown_rx) = oneshot::channel();
    async_runtime::tokio_runtime().spawn(async move {
        let listener = match TcpListener::bind(SocketAddrV4::new(Ipv4Addr::LOCALHOST, 0)).and_then(
            |listener| {
                listener.set_nonblocking(true)?;
                Ok(listener)
            },
        ) {
            Ok(listener) => listener,
            Err(error) => {
                let _ = ready_tx.send(Err(error.to_string()));
                return;
            }
        };
        let address = match listener.local_addr() {
            Ok(address) => address,
            Err(error) => {
                let _ = ready_tx.send(Err(error.to_string()));
                return;
            }
        };
        let router = Router::new()
            .route("/media/:token/:file_name", get(get_media).head(head_media))
            .with_state(server_source);
        let server = match axum::Server::from_tcp(listener) {
            Ok(server) => server,
            Err(error) => {
                let _ = ready_tx.send(Err(error.to_string()));
                return;
            }
        }
        .serve(router.into_make_service())
        .with_graceful_shutdown(async move {
            let _ = shutdown_rx.await;
        });
        if ready_tx.send(Ok(address.port())).is_err() {
            return;
        }
        if let Err(error) = server.await {
            tracing::error!("playback gateway failed: {error}");
        }
    });
    let port = ready_rx
        .await
        .map_err(|error| BError::CustomError {
            message: format!("playback gateway stopped during startup: {error}"),
        })?
        .map_err(|message| BError::CustomError {
            message: format!("failed to create playback gateway: {message}"),
        })?;

    tracing::info!(
        total_size = source.total_size,
        content_type = %source.content_type,
        "playback gateway ready"
    );
    Ok(Arc::new(PlaybackSession {
        url: format!(
            "http://127.0.0.1:{port}/media/{token}/stream.{}",
            media_extension_for_path(&path)
        ),
        source,
        shutdown: Mutex::new(Some(shutdown_tx)),
    }))
}

async fn head_media(
    Path((token, _file_name)): Path<(String, String)>,
    State(source): State<Arc<PlaybackSource>>,
) -> Response {
    if token != source.token || !source.active.load(Ordering::Acquire) {
        return StatusCode::NOT_FOUND.into_response();
    }
    response_with_headers(
        StatusCode::OK,
        source.total_size,
        None,
        &source.content_type,
        Body::empty(),
    )
}

async fn get_media(
    Path((token, _file_name)): Path<(String, String)>,
    State(source): State<Arc<PlaybackSource>>,
    headers: HeaderMap,
) -> Response {
    if token != source.token || !source.active.load(Ordering::Acquire) {
        return StatusCode::NOT_FOUND.into_response();
    }
    let resolved = resolve_range(
        headers.get(RANGE).and_then(|value| value.to_str().ok()),
        source.total_size,
    );
    let (start, end_inclusive, partial) = match resolved {
        Ok(value) => value,
        Err(()) => {
            let mut response = StatusCode::RANGE_NOT_SATISFIABLE.into_response();
            response.headers_mut().insert(
                CONTENT_RANGE,
                HeaderValue::from_str(&format!("bytes */{}", source.total_size)).unwrap(),
            );
            return response;
        }
    };
    tracing::info!(start, end = end_inclusive, partial, "playback client range");

    let stream_source = source.clone();
    let stream = async_stream::stream! {
        let mut current = start;
        while current <= end_inclusive {
            let block_start = current / BLOCK_SIZE * BLOCK_SIZE;
            match stream_source.block(block_start).await {
                Ok(block) => {
                    let offset = (current - block_start) as usize;
                    if offset >= block.len() {
                        yield Err(io::Error::new(io::ErrorKind::UnexpectedEof, "range block too short"));
                        break;
                    }
                    let remaining = (end_inclusive - current + 1) as usize;
                    let count = remaining.min(block.len() - offset);
                    yield Ok::<Bytes, io::Error>(block.slice(offset..offset + count));
                    current += count as u64;
                }
                Err(error) => {
                    yield Err(io::Error::other(error));
                    break;
                }
            }
        }
    };
    let status = if partial {
        StatusCode::PARTIAL_CONTENT
    } else {
        StatusCode::OK
    };
    response_with_headers(
        status,
        end_inclusive - start + 1,
        partial.then_some((start, end_inclusive, source.total_size)),
        &source.content_type,
        Body::wrap_stream(stream),
    )
}

fn response_with_headers(
    status: StatusCode,
    content_length: u64,
    content_range: Option<(u64, u64, u64)>,
    content_type: &str,
    body: Body,
) -> Response {
    let mut response = Response::new(boxed(body));
    *response.status_mut() = status;
    let headers = response.headers_mut();
    headers.insert(ACCEPT_RANGES, HeaderValue::from_static("bytes"));
    headers.insert(
        CONTENT_LENGTH,
        HeaderValue::from_str(&content_length.to_string()).unwrap(),
    );
    headers.insert(
        CONTENT_TYPE,
        HeaderValue::from_str(content_type)
            .unwrap_or_else(|_| HeaderValue::from_static("application/octet-stream")),
    );
    if let Some((start, end_inclusive, total_size)) = content_range {
        headers.insert(
            CONTENT_RANGE,
            HeaderValue::from_str(&format!("bytes {start}-{end_inclusive}/{total_size}")).unwrap(),
        );
    }
    response
}

fn resolve_range(value: Option<&str>, total_size: u64) -> Result<(u64, u64, bool), ()> {
    let Some(value) = value else {
        return Ok((0, total_size - 1, false));
    };
    let value = value.strip_prefix("bytes=").ok_or(())?;
    if value.contains(',') {
        return Err(());
    }
    let (start, end) = value.split_once('-').ok_or(())?;
    if start.is_empty() {
        let suffix_len: u64 = end.parse().map_err(|_| ())?;
        if suffix_len == 0 {
            return Err(());
        }
        let start = total_size.saturating_sub(suffix_len);
        return Ok((start, total_size - 1, true));
    }
    let start: u64 = start.parse().map_err(|_| ())?;
    if start >= total_size {
        return Err(());
    }
    let end_inclusive = if end.is_empty() {
        total_size - 1
    } else {
        end.parse::<u64>().map_err(|_| ())?.min(total_size - 1)
    };
    if end_inclusive < start {
        return Err(());
    }
    Ok((start, end_inclusive, true))
}

fn random_token() -> String {
    let mut bytes = [0_u8; 16];
    OsRng.fill_bytes(&mut bytes);
    bytes.iter().map(|byte| format!("{byte:02x}")).collect()
}

fn content_type_for_path(path: &str) -> &'static str {
    match media_extension_for_path(path) {
        "flac" => "audio/flac",
        "mp3" => "audio/mpeg",
        "m4a" | "mp4" => "audio/mp4",
        "ogg" | "oga" => "audio/ogg",
        "opus" => "audio/opus",
        "wav" => "audio/wav",
        _ => "application/octet-stream",
    }
}

fn media_extension_for_path(path: &str) -> &'static str {
    match path
        .rsplit('.')
        .next()
        .map(str::to_ascii_lowercase)
        .as_deref()
    {
        Some("flac") => "flac",
        Some("mp3") => "mp3",
        Some("m4a") => "m4a",
        Some("mp4") => "mp4",
        Some("ogg") => "ogg",
        Some("oga") => "oga",
        Some("opus") => "opus",
        Some("wav") => "wav",
        _ => "bin",
    }
}

fn is_generic_content_type(value: &str) -> bool {
    matches!(
        value
            .split(';')
            .next()
            .unwrap_or_default()
            .trim()
            .to_ascii_lowercase()
            .as_str(),
        "application/octet-stream" | "binary/octet-stream"
    )
}

#[cfg(test)]
mod tests {
    use super::*;
    use futures_util::future::BoxFuture;
    use storage_backend::{
        Entry, LocalBackend, RangeResponse, StorageBackendError, StorageBackendResult, StreamFile,
    };

    #[derive(Default)]
    struct SizeChangingBackend {
        range_calls: AtomicU64,
        released: AtomicBool,
    }

    #[derive(Default)]
    struct ReleaseCountingBackend {
        released: AtomicU64,
    }

    impl StorageBackend for SizeChangingBackend {
        fn list(&self, _dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>> {
            Box::pin(async { Ok(Vec::new()) })
        }

        fn get(
            &self,
            _path: String,
            _byte_offset: u64,
        ) -> BoxFuture<'_, StorageBackendResult<StreamFile>> {
            Box::pin(async {
                Err(StorageBackendError::UnsupportedFeature(
                    "streaming is not used by this test".to_string(),
                ))
            })
        }

        fn get_range_response(
            &self,
            _path: String,
            range: ByteRange,
        ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>> {
            let total_size = if self.range_calls.fetch_add(1, Ordering::SeqCst) == 0 {
                10
            } else {
                11
            };
            Box::pin(async move {
                Ok(RangeResponse {
                    bytes: Bytes::from(vec![b'x'; range.len() as usize]),
                    total_size,
                    content_type: Some("audio/flac".to_string()),
                })
            })
        }

        fn release(&self, _path: String) -> BoxFuture<'_, StorageBackendResult<()>> {
            self.released.store(true, Ordering::SeqCst);
            Box::pin(async { Ok(()) })
        }
    }

    impl StorageBackend for ReleaseCountingBackend {
        fn list(&self, _dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>> {
            Box::pin(async { Ok(Vec::new()) })
        }

        fn get(
            &self,
            _path: String,
            _byte_offset: u64,
        ) -> BoxFuture<'_, StorageBackendResult<StreamFile>> {
            Box::pin(async {
                Err(StorageBackendError::UnsupportedFeature(
                    "streaming is not used by this test".to_string(),
                ))
            })
        }

        fn get_range_response(
            &self,
            _path: String,
            range: ByteRange,
        ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>> {
            Box::pin(async move {
                Ok(RangeResponse {
                    bytes: Bytes::from(vec![b'x'; range.len() as usize]),
                    total_size: 10,
                    content_type: Some("audio/flac".to_string()),
                })
            })
        }

        fn release(&self, _path: String) -> BoxFuture<'_, StorageBackendResult<()>> {
            self.released.fetch_add(1, Ordering::SeqCst);
            Box::pin(async { Ok(()) })
        }
    }

    #[test]
    fn parses_http_ranges() {
        assert_eq!(resolve_range(None, 100), Ok((0, 99, false)));
        assert_eq!(resolve_range(Some("bytes=10-19"), 100), Ok((10, 19, true)));
        assert_eq!(resolve_range(Some("bytes=90-"), 100), Ok((90, 99, true)));
        assert_eq!(resolve_range(Some("bytes=-10"), 100), Ok((90, 99, true)));
        assert!(resolve_range(Some("bytes=100-"), 100).is_err());
        assert!(resolve_range(Some("bytes=1-2,4-5"), 100).is_err());
    }

    #[tokio::test]
    async fn streams_bounded_ranges_over_loopback() {
        let path = std::env::temp_dir().join(format!("musicapp-range-{}.flac", random_token()));
        std::fs::write(&path, b"0123456789").unwrap();
        let session = start_playback_gateway(
            Arc::new(LocalBackend::new()),
            path.to_string_lossy().to_string(),
        )
        .await
        .unwrap();

        assert!(session.url().ends_with("/stream.flac"));
        let client = reqwest::Client::builder().no_proxy().build().unwrap();
        let head = client.head(session.url()).send().await.unwrap();
        assert_eq!(head.status(), StatusCode::OK);
        assert_eq!(head.headers()[CONTENT_LENGTH], "10");
        assert_eq!(head.headers()[CONTENT_TYPE], "audio/flac");
        assert_eq!(head.headers()[ACCEPT_RANGES], "bytes");

        let full = client.get(session.url()).send().await.unwrap();
        assert_eq!(full.status(), StatusCode::OK);
        assert_eq!(full.bytes().await.unwrap().as_ref(), b"0123456789");

        let response = client
            .get(session.url())
            .header(RANGE, "bytes=2-5")
            .send()
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::PARTIAL_CONTENT);
        assert_eq!(response.headers()[CONTENT_RANGE], "bytes 2-5/10");
        assert_eq!(response.bytes().await.unwrap().as_ref(), b"2345");

        let suffix = client
            .get(session.url())
            .header(RANGE, "bytes=-3")
            .send()
            .await
            .unwrap();
        assert_eq!(suffix.status(), StatusCode::PARTIAL_CONTENT);
        assert_eq!(suffix.bytes().await.unwrap().as_ref(), b"789");

        let forward_seek = client
            .get(session.url())
            .header(RANGE, "bytes=7-8")
            .send()
            .await
            .unwrap();
        assert_eq!(forward_seek.bytes().await.unwrap().as_ref(), b"78");
        let backward_seek = client
            .get(session.url())
            .header(RANGE, "bytes=1-2")
            .send()
            .await
            .unwrap();
        assert_eq!(backward_seek.bytes().await.unwrap().as_ref(), b"12");

        let invalid = client
            .get(session.url())
            .header(RANGE, "bytes=10-")
            .send()
            .await
            .unwrap();
        assert_eq!(invalid.status(), StatusCode::RANGE_NOT_SATISFIABLE);
        assert_eq!(invalid.headers()[CONTENT_RANGE], "bytes */10");
        assert_eq!(session.stats().remote_requests, 2);

        let url = session.url();
        session.shutdown();
        assert!(!session.source.active.load(Ordering::Acquire));
        let direct = get_media(
            Path((session.source.token.clone(), "stream.flac".to_string())),
            State(session.source.clone()),
            HeaderMap::new(),
        )
        .await;
        assert_eq!(direct.status(), StatusCode::NOT_FOUND);
        let stopped = match tokio::time::timeout(
            std::time::Duration::from_millis(500),
            client.get(&url).send(),
        )
        .await
        {
            Err(_) | Ok(Err(_)) => true,
            Ok(Ok(response)) => !response.status().is_success(),
        };
        assert!(stopped, "playback URL remained available after shutdown");
        std::fs::remove_file(path).unwrap();
    }

    #[tokio::test]
    async fn reports_remote_size_changes_and_releases_the_source() {
        let backend = Arc::new(SizeChangingBackend::default());
        let session = start_playback_gateway(backend.clone(), "/changed.flac".to_string())
            .await
            .unwrap();

        let error = session.source.block(0).await.unwrap_err();
        assert!(error.contains("remote size changed from 10 to 11"));

        session.shutdown();
        tokio::time::timeout(std::time::Duration::from_secs(2), async {
            while !backend.released.load(Ordering::SeqCst) {
                tokio::task::yield_now().await;
            }
        })
        .await
        .expect("playback source reader was not released");
    }

    #[tokio::test]
    async fn rapid_session_switching_releases_every_source_reader() {
        const SESSION_COUNT: u64 = 12;
        let backend = Arc::new(ReleaseCountingBackend::default());

        for index in 0..SESSION_COUNT {
            let session = start_playback_gateway(backend.clone(), format!("/track-{index}.flac"))
                .await
                .unwrap();
            session.shutdown();
        }

        tokio::time::timeout(std::time::Duration::from_secs(2), async {
            while backend.released.load(Ordering::SeqCst) != SESSION_COUNT {
                tokio::task::yield_now().await;
            }
        })
        .await
        .expect("rapid playback switching left source readers unreleased");
    }
}

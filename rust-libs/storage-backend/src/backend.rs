use std::io::ErrorKind;

use bytes::Bytes;
use futures_util::future::BoxFuture;
use reqwest::{header::CONTENT_RANGE, StatusCode};
use tidetunes_async_runtime::tokio_runtime;

#[derive(Debug, Clone)]
pub struct Entry {
    pub name: String,
    pub path: String,
    pub size: Option<usize>,
    pub is_dir: bool,
    pub remote_id: Option<String>,
    pub parent_remote_id: Option<String>,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub ctag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone)]
pub struct DeltaItem {
    pub remote_id: String,
    pub parent_remote_id: Option<String>,
    pub name: Option<String>,
    pub path: Option<String>,
    pub size: Option<usize>,
    pub is_dir: bool,
    pub deleted: bool,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub ctag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone)]
pub struct DeltaPage {
    pub items: Vec<DeltaItem>,
    pub next_link: Option<String>,
    pub delta_link: Option<String>,
}

#[derive(Debug, Clone)]
pub struct WebDavSyncItem {
    pub path: String,
    pub name: Option<String>,
    pub size: Option<usize>,
    pub is_dir: bool,
    pub deleted: bool,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone)]
pub struct WebDavSyncPage {
    pub items: Vec<WebDavSyncItem>,
    pub sync_token: String,
}

pub(crate) fn parse_remote_timestamp(value: &str) -> Option<i64> {
    chrono::DateTime::parse_from_rfc3339(value)
        .or_else(|_| chrono::DateTime::parse_from_rfc2822(value))
        .ok()
        .map(|value| value.timestamp_millis())
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ByteRange {
    pub start: u64,
    pub end_inclusive: u64,
}

#[derive(Debug, Clone)]
pub struct RangeResponse {
    pub bytes: Bytes,
    pub total_size: u64,
    pub content_type: Option<String>,
}

impl ByteRange {
    pub fn new(start: u64, end_inclusive: u64) -> StorageBackendResult<Self> {
        if end_inclusive < start {
            return Err(StorageBackendError::InvalidRange {
                start,
                end_inclusive,
            });
        }
        Ok(Self {
            start,
            end_inclusive,
        })
    }

    pub fn len(self) -> u64 {
        self.end_inclusive - self.start + 1
    }

    pub fn is_empty(self) -> bool {
        false
    }
}

enum StreamFileInner {
    Response(reqwest::Response),
    Total(bytes::Bytes),
}

pub struct StreamFile {
    inner: StreamFileInner,
    total: Option<usize>,
    content_type: Option<String>,
    name: String,
    byte_offset: u64,
}

#[derive(thiserror::Error, Debug)]
pub enum StorageBackendError {
    #[error(transparent)]
    RequestFail(#[from] reqwest::Error),
    #[error("Parse XML Fail")]
    ParseXMLFail,
    #[error(transparent)]
    TokioIO(#[from] tokio::io::Error),
    #[error(transparent)]
    TokioJoinError(#[from] tokio::task::JoinError),
    #[error("Url Parse Error")]
    UrlParseError(String),
    #[error("Serde Json Error: {0}")]
    SerdeJsonError(#[from] serde_json::Error),
    #[error("QuickXML De Error: {0}")]
    QuickXMLDeError(#[from] quick_xml::DeError),
    #[error("invalid byte range {start}-{end_inclusive}")]
    InvalidRange { start: u64, end_inclusive: u64 },
    #[error("server ignored byte range request and returned HTTP {status}")]
    RangeNotSupported { status: u16 },
    #[error("invalid Content-Range header: {0}")]
    InvalidContentRange(String),
    #[error("range response exceeded requested length: expected at most {expected}, got {actual}")]
    RangeBodyTooLarge { expected: u64, actual: u64 },
    #[error("storage backend does not support delta synchronization")]
    DeltaNotSupported,
    #[error("remote delta cursor requires a full resynchronization")]
    DeltaResyncRequired,
    #[error("remote request retries exhausted: {0}")]
    RetryExhausted(String),
    #[error("WebDAV {operation} failed with HTTP {status}")]
    WebDavWriteFailed { operation: String, status: u16 },
    #[error("OAuth token response did not contain a refresh token")]
    MissingOAuthRefreshToken,
}

#[derive(thiserror::Error, Debug)]
enum SendChunkError {
    #[error(transparent)]
    RequestFail(#[from] reqwest::Error),
    #[error("mpsc send error: {0}")]
    MpscSendError(#[from] async_channel::SendError<StorageBackendResult<Bytes>>),
}

pub type StorageBackendResult<T> = std::result::Result<T, StorageBackendError>;

impl StorageBackendError {
    pub fn is_timeout(&self) -> bool {
        if let StorageBackendError::RequestFail(e) = self {
            return e.is_timeout();
        }
        false
    }

    pub fn is_unauthorized(&self) -> bool {
        if let StorageBackendError::RequestFail(e) = self {
            return e.status() == Some(StatusCode::UNAUTHORIZED);
        }
        false
    }

    pub fn is_not_found(&self) -> bool {
        match self {
            StorageBackendError::RequestFail(e) => e.status() == Some(StatusCode::NOT_FOUND),
            StorageBackendError::TokioIO(e) => e.kind() == ErrorKind::NotFound,
            _ => false,
        }
    }

    pub fn is_delta_resync_required(&self) -> bool {
        matches!(self, StorageBackendError::DeltaResyncRequired)
    }

    pub fn is_retryable(&self) -> bool {
        match self {
            StorageBackendError::RequestFail(error) => {
                error.is_timeout()
                    || error.is_connect()
                    || error.status().is_some_and(|status| {
                        matches!(
                            status,
                            StatusCode::TOO_MANY_REQUESTS
                                | StatusCode::INTERNAL_SERVER_ERROR
                                | StatusCode::BAD_GATEWAY
                                | StatusCode::SERVICE_UNAVAILABLE
                                | StatusCode::GATEWAY_TIMEOUT
                        )
                    })
            }
            StorageBackendError::TokioIO(error) => matches!(
                error.kind(),
                ErrorKind::TimedOut
                    | ErrorKind::ConnectionAborted
                    | ErrorKind::ConnectionRefused
                    | ErrorKind::ConnectionReset
            ),
            _ => false,
        }
    }
}

pub trait StorageBackend: Send + Sync {
    fn list(&self, dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>>;
    fn get(&self, p: String, byte_offset: u64) -> BoxFuture<'_, StorageBackendResult<StreamFile>>;
    fn get_range_response(
        &self,
        p: String,
        range: ByteRange,
    ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>>;
    fn get_range(&self, p: String, range: ByteRange) -> BoxFuture<'_, StorageBackendResult<Bytes>> {
        Box::pin(async move { Ok(self.get_range_response(p, range).await?.bytes) })
    }
    fn delta(
        &self,
        _root_remote_id: String,
        _cursor: Option<String>,
        _latest_only: bool,
    ) -> BoxFuture<'_, StorageBackendResult<DeltaPage>> {
        Box::pin(async { Err(StorageBackendError::DeltaNotSupported) })
    }
    fn webdav_sync(
        &self,
        _root_path: String,
        _sync_token: Option<String>,
    ) -> BoxFuture<'_, StorageBackendResult<WebDavSyncPage>> {
        Box::pin(async { Err(StorageBackendError::DeltaNotSupported) })
    }
    fn current_refresh_token(&self) -> BoxFuture<'_, StorageBackendResult<Option<String>>> {
        Box::pin(async { Ok(None) })
    }
}

pub(crate) async fn read_range_response(
    response: reqwest::Response,
    requested: ByteRange,
) -> StorageBackendResult<RangeResponse> {
    if response.status() != StatusCode::PARTIAL_CONTENT {
        if response.status().is_success() {
            return Err(StorageBackendError::RangeNotSupported {
                status: response.status().as_u16(),
            });
        }
        return Err(response.error_for_status().unwrap_err().into());
    }

    let content_range = response
        .headers()
        .get(CONTENT_RANGE)
        .and_then(|value| value.to_str().ok())
        .ok_or_else(|| StorageBackendError::InvalidContentRange("missing".to_string()))?;
    let (start, end_inclusive, total_size) = parse_content_range(content_range)?;
    if start != requested.start || end_inclusive > requested.end_inclusive {
        return Err(StorageBackendError::InvalidContentRange(
            content_range.to_string(),
        ));
    }

    let content_type = response
        .headers()
        .get(reqwest::header::CONTENT_TYPE)
        .and_then(|value| value.to_str().ok())
        .map(str::to_owned);
    let bytes = response.bytes().await?;
    if bytes.len() as u64 > requested.len() {
        return Err(StorageBackendError::RangeBodyTooLarge {
            expected: requested.len(),
            actual: bytes.len() as u64,
        });
    }
    Ok(RangeResponse {
        bytes,
        total_size,
        content_type,
    })
}

fn parse_content_range(value: &str) -> StorageBackendResult<(u64, u64, u64)> {
    let (range, total_size) = value
        .strip_prefix("bytes ")
        .and_then(|value| value.split_once('/'))
        .ok_or_else(|| StorageBackendError::InvalidContentRange(value.to_string()))?;
    let (start, end_inclusive) = range
        .split_once('-')
        .ok_or_else(|| StorageBackendError::InvalidContentRange(value.to_string()))?;
    let start = start
        .parse()
        .map_err(|_| StorageBackendError::InvalidContentRange(value.to_string()))?;
    let end_inclusive = end_inclusive
        .parse()
        .map_err(|_| StorageBackendError::InvalidContentRange(value.to_string()))?;
    let total_size = total_size
        .parse()
        .map_err(|_| StorageBackendError::InvalidContentRange(value.to_string()))?;
    if end_inclusive < start || total_size == 0 || end_inclusive >= total_size {
        return Err(StorageBackendError::InvalidContentRange(value.to_string()));
    }
    Ok((start, end_inclusive, total_size))
}

impl StreamFile {
    pub fn new(resp: reqwest::Response, byte_offset: u64) -> Self {
        let url = resp.url().to_string();
        let name = url.split('/').next_back().unwrap_or_default();
        let header_map = resp.headers();
        let content_length = header_map
            .get(reqwest::header::CONTENT_LENGTH)
            .and_then(|v| v.to_str().ok())
            .and_then(|v| v.parse::<usize>().ok());
        let content_type = header_map
            .get(reqwest::header::CONTENT_TYPE)
            .and_then(|v| v.to_str().ok())
            .map(str::to_string);
        Self {
            inner: StreamFileInner::Response(resp),
            total: content_length,
            content_type,
            name: name.to_string(),
            byte_offset,
        }
    }
    pub fn new_from_bytes(buf: &[u8], name: &str, byte_offset: u64) -> Self {
        let total: usize = buf.len();
        let buf = bytes::Bytes::copy_from_slice(buf);
        Self {
            inner: StreamFileInner::Total(buf),
            total: Some(total),
            content_type: None,
            name: name.to_string(),
            byte_offset: byte_offset.min(total as u64),
        }
    }
    pub fn size(&self) -> Option<usize> {
        self.total.map(|total| total - self.byte_offset as usize)
    }
    pub fn content_type(&self) -> Option<&str> {
        self.content_type.as_deref()
    }
    pub fn name(&self) -> &str {
        self.name.as_str()
    }

    pub fn into_rx(self) -> async_channel::Receiver<StorageBackendResult<Bytes>> {
        let (tx, rx) = async_channel::bounded::<StorageBackendResult<Bytes>>(10);

        std::mem::drop(tokio_runtime().spawn(async move {
            let f = || async {
                match self.inner {
                    StreamFileInner::Response(mut response) => {
                        let mut remaining = self.byte_offset as usize;

                        while let Some(chunk) = response.chunk().await? {
                            if chunk.len() <= remaining {
                                remaining -= chunk.len();
                            } else if remaining > 0 {
                                let chunk = Bytes::copy_from_slice(&chunk[remaining..]);
                                remaining = 0;
                                tx.send(Ok(chunk)).await?;
                            } else {
                                tx.send(Ok(chunk)).await?;
                            }
                        }
                    }
                    StreamFileInner::Total(buf) => {
                        let offset = self.byte_offset as usize;
                        if offset == 0 {
                            tx.send(Ok(buf)).await?;
                        } else {
                            let buf = Bytes::copy_from_slice(&buf[offset..]);
                            tx.send(Ok(buf)).await?;
                        }
                    }
                }

                Ok(())
            };

            let res: Result<(), SendChunkError> = f().await;
            if let Err(e) = res {
                let e: Option<StorageBackendError> = match e {
                    SendChunkError::RequestFail(e) => Some(e.into()),
                    _ => None,
                };
                if let Some(e) = e {
                    let _ = tx.send(Err(e)).await;
                }
            }
            let _ = tx.close();
        }));

        rx
    }

    pub async fn bytes(self) -> StorageBackendResult<Bytes> {
        let buf = match self.inner {
            StreamFileInner::Response(response) => response.bytes().await?,
            StreamFileInner::Total(buf) => buf,
        };

        let offset = (self.byte_offset as usize).min(buf.len());
        if offset == 0 {
            Ok(buf)
        } else {
            let buf = Bytes::copy_from_slice(&buf[offset..]);
            Ok(buf)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::{parse_content_range, parse_remote_timestamp, ByteRange, StorageBackendError};

    #[test]
    fn validates_byte_ranges() {
        assert_eq!(ByteRange::new(10, 19).unwrap().len(), 10);
        assert!(matches!(
            ByteRange::new(5, 4),
            Err(StorageBackendError::InvalidRange { .. })
        ));
    }

    #[test]
    fn parses_content_range() {
        assert_eq!(
            parse_content_range("bytes 10-19/100").unwrap(),
            (10, 19, 100)
        );
        assert!(parse_content_range("10-19/100").is_err());
        assert!(parse_content_range("bytes 20-10/100").is_err());
        assert!(parse_content_range("bytes 10-100/100").is_err());
    }

    #[test]
    fn parses_webdav_and_graph_timestamps() {
        let webdav = parse_remote_timestamp("Tue, 26 May 2026 16:39:43 GMT");
        let graph = parse_remote_timestamp("2026-05-26T16:39:43Z");
        assert_eq!(webdav, graph);
        assert!(webdav.is_some());
        assert_eq!(parse_remote_timestamp("not a timestamp"), None);
    }
}

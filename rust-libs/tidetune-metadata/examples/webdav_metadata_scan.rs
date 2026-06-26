use std::{
    collections::{HashMap, VecDeque},
    env, fs,
    sync::{
        atomic::{AtomicU64, AtomicUsize, Ordering},
        mpsc, Arc, Mutex,
    },
    thread,
    time::{Duration, Instant},
};

use bytes::Bytes;
use serde::{Deserialize, Serialize};
use tidetune_metadata::{
    read_metadata, MetadataError, NormalizedMetadata, RangeSource, ReaderLimits, StorageRangeSource,
};
use tidetune_remote_storage::{BuildWebdavArg, ByteRange, Entry, StorageBackend, Webdav};
use tidetune_runtime::tokio_runtime;

#[derive(Serialize)]
struct ScanReport {
    start_index: usize,
    selected_files: usize,
    metadata_files: usize,
    block_size: u64,
    max_requests: usize,
    max_read_bytes: u64,
    file_timeout_secs: u64,
    concurrency: usize,
    listing_elapsed_ms: u128,
    total_elapsed_ms: u128,
    elapsed_ms: u128,
    total_requests: usize,
    total_fetched_bytes: u64,
    success: usize,
    success_partial: usize,
    skipped_unchanged: usize,
    timeout: usize,
    range_failure: usize,
    budget_exceeded: usize,
    unsupported_format: usize,
    parse_failure: usize,
    other_failure: usize,
    p50_ms: u128,
    p95_ms: u128,
    files: Vec<FileResult>,
}

#[derive(Deserialize)]
struct PreviousReport {
    files: Vec<PreviousFile>,
}

#[derive(Deserialize)]
struct PreviousFile {
    path: String,
    size: u64,
    etag: Option<String>,
    modified_at: Option<i64>,
}

#[derive(Serialize)]
struct FileResult {
    index: usize,
    name: String,
    path: String,
    size: u64,
    extension: String,
    mime_type: Option<String>,
    etag: Option<String>,
    created_at: Option<i64>,
    modified_at: Option<i64>,
    status: &'static str,
    elapsed_ms: u128,
    requests: usize,
    fetched_bytes: u64,
    title: Option<String>,
    artist: Option<String>,
    album_artist: Option<String>,
    album: Option<String>,
    genre: Option<String>,
    date: Option<String>,
    track_number: Option<u32>,
    track_total: Option<u32>,
    disc_number: Option<u32>,
    disc_total: Option<u32>,
    duration_ms: Option<u64>,
    sample_rate: Option<u32>,
    bit_depth: Option<u8>,
    channels: Option<u8>,
    overall_bitrate: Option<u32>,
    audio_bitrate: Option<u32>,
    error: Option<String>,
}

struct CountingSource {
    inner: StorageRangeSource,
    requests: Arc<AtomicUsize>,
    fetched_bytes: Arc<AtomicU64>,
}

impl RangeSource for CountingSource {
    fn len(&self) -> u64 {
        self.inner.len()
    }

    fn read_range(&self, range: ByteRange) -> Result<Bytes, MetadataError> {
        let bytes = self.inner.read_range(range)?;
        self.requests.fetch_add(1, Ordering::Relaxed);
        self.fetched_bytes
            .fetch_add(bytes.len() as u64, Ordering::Relaxed);
        Ok(bytes)
    }
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let total_started = Instant::now();
    let address = required_env("TIDETUNE_WEBDAV_ADDRESS")?;
    let username = required_env("TIDETUNE_WEBDAV_USERNAME")?;
    let password = required_env("TIDETUNE_WEBDAV_PASSWORD")?;
    let start_index = optional_env("TIDETUNE_SCAN_START", 0_usize)?;
    let limit = optional_env("TIDETUNE_SCAN_LIMIT", 50_usize)?;
    let timeout_secs = optional_env("TIDETUNE_SCAN_FILE_TIMEOUT_SECS", 30_u64)?;
    let concurrency = optional_env("TIDETUNE_SCAN_CONCURRENCY", 4_usize)?;
    if concurrency == 0 {
        return Err("TIDETUNE_SCAN_CONCURRENCY must be greater than zero".into());
    }
    let limits = ReaderLimits {
        block_size: optional_env(
            "TIDETUNE_SCAN_BLOCK_SIZE",
            ReaderLimits::default().block_size,
        )?,
        ..ReaderLimits::default()
    };
    let output = env::var("TIDETUNE_SCAN_OUTPUT").ok();
    let previous = load_previous_report()?;
    let backend: Arc<dyn StorageBackend + Send + Sync> = Arc::new(Webdav::new(BuildWebdavArg {
        addr: address,
        username,
        password,
        is_anonymous: false,
        connect_timeout: Duration::from_secs(10),
    }));

    let mut files: Vec<Entry> = tokio_runtime()
        .block_on(backend.list("/".to_string()))?
        .into_iter()
        .filter(is_audio_file)
        .collect();
    files.sort_by(|left, right| left.path.cmp(&right.path));
    let files: Vec<_> = files.into_iter().skip(start_index).take(limit).collect();
    let selected_count = files.len();
    let listing_elapsed = total_started.elapsed();

    let scan_started = Instant::now();
    let mut skipped_results = Vec::new();
    let mut metadata_jobs = Vec::new();
    for (index, entry) in files.into_iter().enumerate() {
        let index = start_index + index + 1;
        if previous
            .get(&entry.path)
            .is_some_and(|previous| is_unchanged(&entry, previous))
        {
            skipped_results.push(skipped_result(index, entry));
        } else {
            metadata_jobs.push((index, entry));
        }
    }
    let metadata_count = metadata_jobs.len();
    let jobs = Arc::new(Mutex::new(
        metadata_jobs.into_iter().collect::<VecDeque<_>>(),
    ));
    let completed = Arc::new(AtomicUsize::new(skipped_results.len()));
    let (result_sender, result_receiver) = mpsc::channel();
    let worker_count = concurrency.min(metadata_count);

    for _ in 0..worker_count {
        let jobs = jobs.clone();
        let completed = completed.clone();
        let result_sender = result_sender.clone();
        let backend = backend.clone();
        thread::spawn(move || loop {
            let job = jobs.lock().unwrap().pop_front();
            let Some((index, entry)) = job else {
                break;
            };
            let size = entry.size.unwrap_or_default() as u64;
            let result = scan_one(
                index,
                entry,
                size,
                backend.clone(),
                Duration::from_secs(timeout_secs),
                limits,
            );
            let progress = completed.fetch_add(1, Ordering::Relaxed) + 1;
            eprintln!(
                "[{progress}/{selected_count}] {} {} ms, {} requests, {} bytes",
                result.status, result.elapsed_ms, result.requests, result.fetched_bytes
            );
            if result_sender.send(result).is_err() {
                break;
            }
        });
    }
    drop(result_sender);

    let mut results = skipped_results;
    results.reserve(metadata_count);
    for result in result_receiver {
        results.push(result);
    }
    results.sort_by_key(|result| result.index);

    let report = summarize(SummaryInput {
        files: results,
        elapsed: scan_started.elapsed(),
        start_index,
        limits,
        file_timeout_secs: timeout_secs,
        concurrency,
        metadata_files: metadata_count,
        listing_elapsed,
        total_elapsed: total_started.elapsed(),
    });
    let json = serde_json::to_string_pretty(&report)?;
    if let Some(output) = output {
        fs::write(output, &json)?;
    }
    println!("{json}");
    Ok(())
}

fn load_previous_report() -> Result<HashMap<String, PreviousFile>, Box<dyn std::error::Error>> {
    let Some(path) = env::var("TIDETUNE_SCAN_PREVIOUS").ok() else {
        return Ok(HashMap::new());
    };
    let report: PreviousReport = serde_json::from_slice(&fs::read(path)?)?;
    Ok(report
        .files
        .into_iter()
        .map(|file| (file.path.clone(), file))
        .collect())
}

fn is_unchanged(entry: &Entry, previous: &PreviousFile) -> bool {
    if entry.size.map(|size| size as u64) != Some(previous.size) {
        return false;
    }
    if let Some(etag) = entry.etag.as_deref().filter(|etag| !etag.is_empty()) {
        return previous.etag.as_deref() == Some(etag);
    }
    entry.modified_at.is_some() && entry.modified_at == previous.modified_at
}

fn skipped_result(index: usize, entry: Entry) -> FileResult {
    let extension = extension(&entry.name);
    file_result(
        index,
        entry.name,
        entry.path,
        entry.size.unwrap_or_default() as u64,
        extension,
        entry.mime_type,
        entry.etag,
        entry.created_at,
        entry.modified_at,
        "skipped_unchanged",
        Duration::ZERO,
        0,
        0,
        None,
        None,
    )
}

fn scan_one(
    index: usize,
    entry: Entry,
    size: u64,
    backend: Arc<dyn StorageBackend + Send + Sync>,
    timeout: Duration,
    limits: ReaderLimits,
) -> FileResult {
    let name = entry.name;
    let path = entry.path;
    let mime_type = entry.mime_type;
    let etag = entry.etag;
    let created_at = entry.created_at;
    let modified_at = entry.modified_at;
    let extension = extension(&name);
    let requests = Arc::new(AtomicUsize::new(0));
    let fetched_bytes = Arc::new(AtomicU64::new(0));
    let thread_requests = requests.clone();
    let thread_fetched_bytes = fetched_bytes.clone();
    let thread_path = path.clone();
    let (sender, receiver) = mpsc::sync_channel(1);
    let started = Instant::now();

    thread::spawn(move || {
        let source = Arc::new(CountingSource {
            inner: StorageRangeSource::new(backend, thread_path, size),
            requests: thread_requests,
            fetched_bytes: thread_fetched_bytes,
        });
        let result = read_metadata(source, limits);
        let _ = sender.send(result);
    });

    let (status, metadata, error) = match receiver.recv_timeout(timeout) {
        Ok(Ok(metadata)) => {
            let partial = metadata.title.is_none()
                || metadata.artist.is_none()
                || metadata.album.is_none()
                || metadata.duration_ms == 0
                || metadata.sample_rate.is_none()
                || metadata.bit_depth.is_none()
                || metadata.channels.is_none();
            (
                if partial {
                    "success_partial"
                } else {
                    "success"
                },
                Some(metadata),
                None,
            )
        }
        Ok(Err(error)) => {
            let status = classify_error(&error);
            (status, None, Some(error.to_string()))
        }
        Err(mpsc::RecvTimeoutError::Timeout) => (
            "timeout",
            None,
            Some(format!(
                "metadata scan exceeded {} seconds",
                timeout.as_secs()
            )),
        ),
        Err(mpsc::RecvTimeoutError::Disconnected) => (
            "other_failure",
            None,
            Some("metadata scan worker disconnected".to_string()),
        ),
    };
    file_result(
        index,
        name,
        path,
        size,
        extension,
        mime_type,
        etag,
        created_at,
        modified_at,
        status,
        started.elapsed(),
        requests.load(Ordering::Relaxed),
        fetched_bytes.load(Ordering::Relaxed),
        metadata,
        error,
    )
}

#[allow(clippy::too_many_arguments)]
fn file_result(
    index: usize,
    name: String,
    path: String,
    size: u64,
    extension: String,
    mime_type: Option<String>,
    etag: Option<String>,
    created_at: Option<i64>,
    modified_at: Option<i64>,
    status: &'static str,
    elapsed: Duration,
    requests: usize,
    fetched_bytes: u64,
    metadata: Option<NormalizedMetadata>,
    error: Option<String>,
) -> FileResult {
    FileResult {
        index,
        name,
        path,
        size,
        extension,
        mime_type,
        etag,
        created_at,
        modified_at,
        status,
        elapsed_ms: elapsed.as_millis(),
        requests,
        fetched_bytes,
        title: metadata.as_ref().and_then(|value| value.title.clone()),
        artist: metadata.as_ref().and_then(|value| value.artist.clone()),
        album_artist: metadata
            .as_ref()
            .and_then(|value| value.album_artist.clone()),
        album: metadata.as_ref().and_then(|value| value.album.clone()),
        genre: metadata.as_ref().and_then(|value| value.genre.clone()),
        date: metadata.as_ref().and_then(|value| value.date.clone()),
        track_number: metadata.as_ref().and_then(|value| value.track_number),
        track_total: metadata.as_ref().and_then(|value| value.track_total),
        disc_number: metadata.as_ref().and_then(|value| value.disc_number),
        disc_total: metadata.as_ref().and_then(|value| value.disc_total),
        duration_ms: metadata.as_ref().map(|value| value.duration_ms),
        sample_rate: metadata.as_ref().and_then(|value| value.sample_rate),
        bit_depth: metadata.as_ref().and_then(|value| value.bit_depth),
        channels: metadata.as_ref().and_then(|value| value.channels),
        overall_bitrate: metadata.as_ref().and_then(|value| value.overall_bitrate),
        audio_bitrate: metadata.and_then(|value| value.audio_bitrate),
        error,
    }
}

fn classify_error(error: &MetadataError) -> &'static str {
    match error {
        MetadataError::RequestBudgetExceeded(_)
        | MetadataError::ByteBudgetExceeded(_)
        | MetadataError::TextTagValueTooLarge(_)
        | MetadataError::TextTagBudgetExceeded(_)
        | MetadataError::TextTagEntryBudgetExceeded(_) => "budget_exceeded",
        MetadataError::Source(message) => {
            let message = message.to_ascii_lowercase();
            if message.contains("timed out") || message.contains("timeout") {
                "timeout"
            } else {
                "range_failure"
            }
        }
        MetadataError::Lofty(error) => {
            let message = error.to_string().to_ascii_lowercase();
            if message.contains("unknown format")
                || message.contains("unsupported")
                || message.contains("file type")
            {
                "unsupported_format"
            } else {
                "parse_failure"
            }
        }
        MetadataError::Io(error) => {
            let message = error.to_string().to_ascii_lowercase();
            if message.contains("budget") {
                "budget_exceeded"
            } else if message.contains("range source") {
                "range_failure"
            } else {
                "parse_failure"
            }
        }
        MetadataError::InvalidLimits => "other_failure",
    }
}

struct SummaryInput {
    files: Vec<FileResult>,
    elapsed: Duration,
    start_index: usize,
    limits: ReaderLimits,
    file_timeout_secs: u64,
    concurrency: usize,
    metadata_files: usize,
    listing_elapsed: Duration,
    total_elapsed: Duration,
}

fn summarize(input: SummaryInput) -> ScanReport {
    let SummaryInput {
        files,
        elapsed,
        start_index,
        limits,
        file_timeout_secs,
        concurrency,
        metadata_files,
        listing_elapsed,
        total_elapsed,
    } = input;
    let mut timings: Vec<u128> = files.iter().map(|file| file.elapsed_ms).collect();
    timings.sort_unstable();
    ScanReport {
        start_index,
        selected_files: files.len(),
        metadata_files,
        block_size: limits.block_size,
        max_requests: limits.max_requests,
        max_read_bytes: limits.max_read_bytes,
        file_timeout_secs,
        concurrency,
        listing_elapsed_ms: listing_elapsed.as_millis(),
        total_elapsed_ms: total_elapsed.as_millis(),
        elapsed_ms: elapsed.as_millis(),
        total_requests: files.iter().map(|file| file.requests).sum(),
        total_fetched_bytes: files.iter().map(|file| file.fetched_bytes).sum(),
        success: count(&files, "success"),
        success_partial: count(&files, "success_partial"),
        skipped_unchanged: count(&files, "skipped_unchanged"),
        timeout: count(&files, "timeout"),
        range_failure: count(&files, "range_failure"),
        budget_exceeded: count(&files, "budget_exceeded"),
        unsupported_format: count(&files, "unsupported_format"),
        parse_failure: count(&files, "parse_failure"),
        other_failure: count(&files, "other_failure"),
        p50_ms: percentile(&timings, 50),
        p95_ms: percentile(&timings, 95),
        files,
    }
}

fn count(files: &[FileResult], status: &str) -> usize {
    files.iter().filter(|file| file.status == status).count()
}

fn percentile(values: &[u128], percentile: usize) -> u128 {
    if values.is_empty() {
        return 0;
    }
    let index = (values.len() - 1) * percentile / 100;
    values[index]
}

fn is_audio_file(entry: &Entry) -> bool {
    !entry.is_dir
        && entry.size.is_some()
        && matches!(
            extension(&entry.name).as_str(),
            "aac"
                | "aif"
                | "aiff"
                | "ape"
                | "flac"
                | "m4a"
                | "mp3"
                | "mp4"
                | "mpc"
                | "oga"
                | "ogg"
                | "opus"
                | "tta"
                | "wav"
                | "wave"
                | "wv"
        )
}

fn extension(name: &str) -> String {
    name.rsplit('.')
        .next()
        .unwrap_or_default()
        .to_ascii_lowercase()
}

fn required_env(name: &str) -> Result<String, Box<dyn std::error::Error>> {
    env::var(name).map_err(|_| format!("missing environment variable {name}").into())
}

fn optional_env<T>(name: &str, default: T) -> Result<T, Box<dyn std::error::Error>>
where
    T: std::str::FromStr,
    T::Err: std::error::Error + 'static,
{
    match env::var(name) {
        Ok(value) => Ok(value.parse()?),
        Err(_) => Ok(default),
    }
}

use std::{env, sync::Arc, time::Duration};

use tidetunes_async_runtime::tokio_runtime;
use tidetunes_audio_metadata::{read_metadata, ReaderLimits, StorageRangeSource};
use tidetunes_storage_backend::{BuildWebdavArg, ByteRange, StorageBackend, Webdav};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let address = required_env("TIDETUNES_WEBDAV_ADDRESS")?;
    let username = required_env("TIDETUNES_WEBDAV_USERNAME")?;
    let password = required_env("TIDETUNES_WEBDAV_PASSWORD")?;
    let backend: Arc<dyn StorageBackend + Send + Sync> = Arc::new(Webdav::new(BuildWebdavArg {
        addr: address,
        username,
        password,
        is_anonymous: false,
        connect_timeout: Duration::from_secs(10),
    }));

    let entries = tokio_runtime().block_on(backend.list("/".to_string()))?;
    let audio = entries
        .iter()
        .find(|entry| {
            !entry.is_dir
                && entry.size.is_some()
                && matches!(
                    entry
                        .name
                        .rsplit('.')
                        .next()
                        .map(str::to_ascii_lowercase)
                        .as_deref(),
                    Some("flac" | "mp3" | "m4a" | "ogg" | "opus" | "wav")
                )
        })
        .ok_or("no supported audio file found at the WebDAV root")?;
    let size = audio.size.ok_or("selected audio file has no size")? as u64;
    let range_end = size.saturating_sub(1).min(31);
    let prefix = tokio_runtime()
        .block_on(backend.get_range(audio.path.clone(), ByteRange::new(0, range_end)?))?;
    let metadata = read_metadata(
        Arc::new(StorageRangeSource::new(backend, audio.path.clone(), size)),
        ReaderLimits::default(),
    )?;

    println!("entries={}", entries.len());
    println!("file={}", audio.name);
    println!("size={size}");
    println!("bounded_range_bytes={}", prefix.len());
    println!("title={}", metadata.title.as_deref().unwrap_or(""));
    println!("artist={}", metadata.artist.as_deref().unwrap_or(""));
    println!("album={}", metadata.album.as_deref().unwrap_or(""));
    println!("duration_ms={}", metadata.duration_ms);
    println!("sample_rate={}", metadata.sample_rate.unwrap_or_default());
    println!("bit_depth={}", metadata.bit_depth.unwrap_or_default());
    println!("channels={}", metadata.channels.unwrap_or_default());
    Ok(())
}

fn required_env(name: &str) -> Result<String, Box<dyn std::error::Error>> {
    env::var(name).map_err(|_| format!("missing environment variable {name}").into())
}

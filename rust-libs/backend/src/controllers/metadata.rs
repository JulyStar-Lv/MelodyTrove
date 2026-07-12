use std::{fs, path::PathBuf, sync::Arc};

use futures_util::{stream, StreamExt};
use sha2::{Digest, Sha256};
use tidetunes_audio_metadata::{read_metadata, EmbeddedArtwork, ReaderLimits, StorageRangeSource};

use crate::{
    ctx::BackendContext,
    error::{BError, BResult},
    objects::{
        RemoteArtwork, RemoteEmbeddedLyrics, RemoteMetadata, RemoteMetadataRequest,
        RemoteMetadataResult, RemoteRawMetadataEntry, Storage,
    },
    schema::StorageEntryLoc,
    services::build_storage_backend,
    Backend,
};

#[uniffi::export]
pub async fn ct_read_remote_metadata(
    backend: Arc<Backend>,
    storage: Storage,
    entry: StorageEntryLoc,
    size: u64,
) -> BResult<RemoteMetadata> {
    if size == 0 {
        return Err(BError::CustomError {
            message: "metadata source size must be greater than zero".to_string(),
        });
    }
    let storage_backend = build_storage_backend(backend.get_context(), storage)?;
    let source = Arc::new(StorageRangeSource::new(storage_backend, entry.path, size));
    let metadata = tidetunes_async_runtime::tokio_runtime()
        .spawn_blocking(move || read_metadata(source, ReaderLimits::default()))
        .await
        .map_err(|error| BError::CustomError {
            message: format!("metadata task failed: {error}"),
        })??;
    let artwork = match metadata.artwork {
        Some(artwork) => match cache_remote_artwork(backend.get_context(), artwork) {
            Ok(value) => value,
            Err(error) => {
                tracing::warn!("failed to cache embedded artwork: {error}");
                None
            }
        },
        None => None,
    };

    Ok(RemoteMetadata {
        title: metadata.title,
        artist: metadata.artist,
        artists: metadata.artists,
        album_artist: metadata.album_artist,
        album: metadata.album,
        composer: metadata.composer,
        lyricist: metadata.lyricist,
        conductor: metadata.conductor,
        genre: metadata.genre,
        grouping: metadata.grouping,
        comment: metadata.comment,
        copyright: metadata.copyright,
        publisher: metadata.publisher,
        date: metadata.date,
        original_release_date: metadata.original_release_date,
        track_number: metadata.track_number,
        track_total: metadata.track_total,
        disc_number: metadata.disc_number,
        disc_total: metadata.disc_total,
        bpm: metadata.bpm,
        musical_key: metadata.musical_key,
        isrc: metadata.isrc,
        musicbrainz_recording_id: metadata.musicbrainz_recording_id,
        musicbrainz_track_id: metadata.musicbrainz_track_id,
        musicbrainz_release_id: metadata.musicbrainz_release_id,
        musicbrainz_release_group_id: metadata.musicbrainz_release_group_id,
        musicbrainz_artist_id: metadata.musicbrainz_artist_id,
        musicbrainz_release_artist_id: metadata.musicbrainz_release_artist_id,
        musicbrainz_work_id: metadata.musicbrainz_work_id,
        replay_gain_track_gain: metadata.replay_gain_track_gain,
        replay_gain_track_peak: metadata.replay_gain_track_peak,
        replay_gain_album_gain: metadata.replay_gain_album_gain,
        replay_gain_album_peak: metadata.replay_gain_album_peak,
        lyrics: metadata.lyrics.map(|lyrics| RemoteEmbeddedLyrics {
            content: lyrics.content,
            synchronized: lyrics.synchronized,
            language: lyrics.language,
            description: lyrics.description,
        }),
        artwork,
        raw_metadata: metadata
            .raw_metadata
            .into_iter()
            .map(|entry| RemoteRawMetadataEntry {
                key: entry.key,
                value: entry.value,
                locale: entry.locale,
                description: entry.description,
            })
            .collect(),
        duration_ms: metadata.duration_ms,
        sample_rate: metadata.sample_rate,
        bit_depth: metadata.bit_depth,
        channels: metadata.channels,
        channel_layout: metadata.channel_layout,
        overall_bitrate: metadata.overall_bitrate,
        audio_bitrate: metadata.audio_bitrate,
        codec: metadata.codec,
        container: metadata.container,
        lossless: metadata.lossless,
    })
}

#[uniffi::export]
pub async fn ct_read_remote_metadata_batch(
    backend: Arc<Backend>,
    storage: Storage,
    requests: Vec<RemoteMetadataRequest>,
    concurrency: u32,
) -> BResult<Vec<RemoteMetadataResult>> {
    if !(1..=16).contains(&concurrency) {
        return Err(BError::CustomError {
            message: "metadata concurrency must be between 1 and 16".to_string(),
        });
    }

    let mut results = stream::iter(requests.into_iter().enumerate())
        .map(|(index, request)| {
            let backend = backend.clone();
            let storage = storage.clone();
            async move {
                let entry = request.entry;
                match ct_read_remote_metadata(backend, storage, entry.clone(), request.size).await {
                    Ok(metadata) => RemoteMetadataResult {
                        request_index: index as u64,
                        entry,
                        metadata: Some(metadata),
                        error: None,
                    },
                    Err(error) => RemoteMetadataResult {
                        request_index: index as u64,
                        entry,
                        metadata: None,
                        error: Some(error.to_string()),
                    },
                }
            }
        })
        .buffer_unordered(concurrency as usize)
        .collect::<Vec<_>>()
        .await;
    results.sort_by_key(|result| result.request_index);
    Ok(results)
}

fn cache_remote_artwork(
    cx: &BackendContext,
    artwork: EmbeddedArtwork,
) -> BResult<Option<RemoteArtwork>> {
    if artwork.data.is_empty() {
        return Ok(None);
    }
    let cache_dir = cx.get_app_cache_dir();
    if cache_dir.trim().is_empty() {
        return Ok(None);
    }

    let content_hash = format!("{:x}", Sha256::digest(&artwork.data));
    let extension = artwork_extension(artwork.mime_type.as_deref());
    let artwork_dir = PathBuf::from(cache_dir).join("artwork");
    fs::create_dir_all(&artwork_dir).map_err(|error| BError::CustomError {
        message: format!("failed to create artwork cache directory: {error}"),
    })?;
    let local_path = artwork_dir.join(format!("{content_hash}.{extension}"));
    if !local_path.exists() {
        fs::write(&local_path, &artwork.data).map_err(|error| BError::CustomError {
            message: format!("failed to write artwork cache file: {error}"),
        })?;
    }

    Ok(Some(RemoteArtwork {
        content_hash,
        local_path: local_path.to_string_lossy().into_owned(),
        thumbnail_path: None,
        width: artwork.width,
        height: artwork.height,
        mime_type: artwork.mime_type,
        picture_type: Some(artwork.picture_type),
    }))
}

fn artwork_extension(mime_type: Option<&str>) -> &'static str {
    match mime_type {
        Some("image/jpeg") | Some("image/jpg") => "jpg",
        Some("image/png") => "png",
        Some("image/gif") => "gif",
        Some("image/bmp") => "bmp",
        Some("image/tiff") => "tif",
        _ => "bin",
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::{SystemTime, UNIX_EPOCH};

    #[test]
    fn caches_remote_artwork_under_app_cache_dir() {
        let cx = BackendContext::new();
        let cache_dir = unique_temp_dir("tidetunes-artwork-cache");
        cx.set_app_cache_dir(cache_dir.to_str().unwrap());
        let data = vec![1, 2, 3, 4];

        let artwork = cache_remote_artwork(
            &cx,
            EmbeddedArtwork {
                data: data.clone(),
                mime_type: Some("image/png".to_string()),
                picture_type: "CoverFront".to_string(),
                width: Some(128),
                height: Some(256),
            },
        )
        .unwrap()
        .expect("artwork should be cached");

        let expected_hash = format!("{:x}", Sha256::digest(&data));
        assert_eq!(expected_hash, artwork.content_hash);
        assert!(artwork
            .local_path
            .ends_with(&format!("{expected_hash}.png")));
        assert_eq!(Some(128), artwork.width);
        assert_eq!(Some(256), artwork.height);
        assert_eq!(Some("image/png".to_string()), artwork.mime_type);
        assert_eq!(Some("CoverFront".to_string()), artwork.picture_type);
        assert_eq!(data, fs::read(&artwork.local_path).unwrap());

        let _ = fs::remove_dir_all(cache_dir);
    }

    #[test]
    fn skips_remote_artwork_when_cache_dir_is_unavailable() {
        let cx = BackendContext::new();

        let artwork = cache_remote_artwork(
            &cx,
            EmbeddedArtwork {
                data: vec![1, 2, 3, 4],
                mime_type: Some("image/png".to_string()),
                picture_type: "CoverFront".to_string(),
                width: None,
                height: None,
            },
        )
        .unwrap();

        assert!(artwork.is_none());
    }

    fn unique_temp_dir(prefix: &str) -> PathBuf {
        let nanos = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        std::env::temp_dir().join(format!("{prefix}-{}-{nanos}", std::process::id()))
    }
}

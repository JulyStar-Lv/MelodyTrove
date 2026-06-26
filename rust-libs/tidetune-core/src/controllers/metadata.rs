use std::sync::Arc;

use futures_util::{stream, StreamExt};
use tidetune_metadata::{read_metadata, ReaderLimits, StorageRangeSource};

use crate::{
    error::{BError, BResult},
    objects::{
        RemoteEmbeddedLyrics, RemoteMetadata, RemoteMetadataRequest, RemoteMetadataResult,
        RemoteRawMetadataEntry, Storage,
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
    let metadata = tidetune_runtime::tokio_runtime()
        .spawn_blocking(move || read_metadata(source, ReaderLimits::default()))
        .await
        .map_err(|error| BError::CustomError {
            message: format!("metadata task failed: {error}"),
        })??;

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

package io.github.julystar.musicapp.domain.importing

import io.github.julystar.musicapp.core.domain.model.MetadataScanOptions
import io.github.julystar.musicapp.database.ArtworkEntity
import io.github.julystar.musicapp.database.MetadataDao
import uniffi.app_backend.RemoteMetadata

internal data class OptionalMetadataUpdate(
    val trackId: Long,
    val albumId: Long?,
    val metadata: RemoteMetadata,
)

internal suspend fun MetadataDao.updateOptionalMetadata(
    updates: List<OptionalMetadataUpdate>,
    options: MetadataScanOptions,
    now: Long,
) {
    if (updates.isEmpty()) return
    val trackIds = updates.map(OptionalMetadataUpdate::trackId).distinct()
    if (options.readArtwork) {
        val artwork = updates.mapNotNull { update ->
            val embedded = update.metadata.artwork ?: return@mapNotNull null
            buildArtworkEntity(
                trackId = update.trackId,
                albumId = update.albumId,
                artwork = embedded,
            )
        }.distinctBy { it.contentHash }
        val artworkToUpsert = mutableListOf<ArtworkEntity>()
        for (candidate in artwork) {
            val existing = getArtworkByContentHash(candidate.contentHash)
            artworkToUpsert += existing?.withRefreshedCacheMetadata(candidate) ?: candidate
        }
        if (artworkToUpsert.isNotEmpty()) upsertArtwork(artworkToUpsert)
    }
    if (options.readLyrics) {
        deleteLyricsForTracksBySource(trackIds, "Embedded")
        val lyrics = updates.mapNotNull { update ->
            buildLyricsEntity(update.trackId, update.metadata, now)
        }
        if (lyrics.isNotEmpty()) upsertLyrics(lyrics)
    }
    if (options.readRawMetadata) {
        deleteRawMetadataForTracks(trackIds)
        val rawMetadata = updates.flatMap { update ->
            buildRawMetadataEntities(update.trackId, update.metadata)
        }
        if (rawMetadata.isNotEmpty()) upsertRawMetadata(rawMetadata)
    }
}

internal fun ArtworkEntity.withRefreshedCacheMetadata(candidate: ArtworkEntity): ArtworkEntity {
    return copy(
        localPath = candidate.localPath,
        thumbnailPath = candidate.thumbnailPath,
        width = candidate.width,
        height = candidate.height,
        mimeType = candidate.mimeType,
        pictureType = candidate.pictureType,
    )
}

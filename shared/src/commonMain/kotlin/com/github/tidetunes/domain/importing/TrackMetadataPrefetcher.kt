package com.github.tidetunes.domain.importing

import com.github.tidetunes.core.domain.model.MetadataRefreshTarget
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshController
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshRequest
import com.github.tidetunes.service.librarysync.domain.MetadataRefreshScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path.Companion.toPath

class TrackMetadataPrefetcher(
    private val metadataDao: MetadataDao,
    private val trackDao: TrackDao,
    private val metadataRefreshController: MetadataRefreshController,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<Long, CompletableDeferred<Boolean>>()

    suspend fun prefetch(trackId: Long): Boolean {
        val acquisition = inFlightMutex.withLock {
            val existing = inFlight[trackId]
            if (existing != null) {
                InFlightAcquisition(existing, owner = false)
            } else {
                val created = CompletableDeferred<Boolean>()
                inFlight[trackId] = created
                InFlightAcquisition(created, owner = true)
            }
        }
        if (!acquisition.owner) return acquisition.result.await()

        return try {
            val refreshed = refreshMissingMetadata(trackId)
            acquisition.result.complete(refreshed)
            refreshed
        } catch (error: Throwable) {
            acquisition.result.completeExceptionally(error)
            throw error
        } finally {
            inFlightMutex.withLock {
                if (inFlight[trackId] === acquisition.result) {
                    inFlight.remove(trackId)
                }
            }
        }
    }

    private suspend fun refreshMissingMetadata(trackId: Long): Boolean {
        val target = missingTarget(trackId) ?: return false
        val result = metadataRefreshController.refresh(
            MetadataRefreshRequest(
                scope = MetadataRefreshScope.Track(trackId),
                target = target,
            )
        )
        return result.refreshedCount > 0
    }

    private suspend fun missingTarget(trackId: Long): MetadataRefreshTarget? {
        val track = trackDao.get(trackId) ?: return null
        val artwork = metadataDao.getArtworkForTrack(trackId)
            ?: track.albumId?.let { albumId -> metadataDao.getArtworkForAlbum(albumId) }
        val artworkMissing = artwork == null ||
            listOfNotNull(artwork.localPath, artwork.thumbnailPath).none { path ->
                fileSystem.metadataOrNull(path.toPath())?.isRegularFile == true
            }
        val lyricsMissing = metadataDao.getLyrics(trackId) == null
        return when {
            artworkMissing && lyricsMissing -> MetadataRefreshTarget.ArtworkAndLyrics
            artworkMissing -> MetadataRefreshTarget.Artwork
            lyricsMissing -> MetadataRefreshTarget.Lyrics
            else -> null
        }
    }
}

private data class InFlightAcquisition(
    val result: CompletableDeferred<Boolean>,
    val owner: Boolean,
)

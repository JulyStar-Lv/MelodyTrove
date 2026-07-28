package io.github.julystar.musicapp.domain.importing

import io.github.julystar.musicapp.core.domain.model.MetadataRefreshTarget
import io.github.julystar.musicapp.database.MetadataDao
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.service.librarysync.domain.MetadataRefreshController
import io.github.julystar.musicapp.service.librarysync.domain.MetadataRefreshRequest
import io.github.julystar.musicapp.service.librarysync.domain.MetadataRefreshScope
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

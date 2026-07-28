package io.github.julystar.musicapp.feature.home.data

import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.repository.LibraryRepository
import io.github.julystar.musicapp.database.TrackDao
import io.github.julystar.musicapp.database.TrackEntity
import io.github.julystar.musicapp.feature.home.domain.HistoryPlayItem
import io.github.julystar.musicapp.feature.home.domain.HomeHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class RoomHomeHistoryRepository(
    private val trackDao: TrackDao,
    libraryRepository: LibraryRepository,
    scope: CoroutineScope,
) : HomeHistoryRepository {

    override val recentPlays: StateFlow<List<HistoryPlayItem>> =
        libraryRepository.tracks
            .mapLatest { tracks ->
                val trackMap = tracks.associateBy { it.id }
                val recentEntities = trackDao.findRecentlyPlayed(limit = 20)
                recentEntities.mapNotNull { entity ->
                    val libTrack = trackMap[entity.id] ?: entity.toLibraryTrackItem()
                    if (entity.lastPlayedAt == null) null
                    else HistoryPlayItem(
                        trackId = entity.id,
                        title = libTrack.title,
                        artist = libTrack.artist,
                        durationMs = libTrack.durationMs,
                        mediaId = libTrack.mediaId,
                        playedAtEpochMs = entity.lastPlayedAt,
                        artworkIndex = indexFor(entity.id),
                    )
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override suspend fun recordPlay(trackId: Long, playedAtEpochMs: Long) {
        trackDao.updateLastPlayedAt(trackId, playedAtEpochMs, playedAtEpochMs)
    }

    companion object {
        private fun indexFor(id: Long): Int = ((id % 8L + 8L) % 8L).toInt() + 1

        private fun TrackEntity.toLibraryTrackItem(): LibraryTrackItem = LibraryTrackItem(
            id = id,
            title = title,
            artist = artist?.takeIf { it.isNotBlank() }
                ?: albumArtist?.takeIf { it.isNotBlank() },
            durationMs = durationMs,
            mediaId = null,
        )
    }
}

package com.github.tidetunes.feature.home.data

import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.feature.home.domain.HistoryPlayItem
import com.github.tidetunes.feature.home.domain.HomeHistoryRepository
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

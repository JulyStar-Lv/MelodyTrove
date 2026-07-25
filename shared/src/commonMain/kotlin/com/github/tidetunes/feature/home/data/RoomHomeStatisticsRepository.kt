package com.github.tidetunes.feature.home.data

import com.github.tidetunes.core.domain.repository.LibraryRepository
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.feature.home.domain.HomeStatistics
import com.github.tidetunes.feature.home.domain.HomeStatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class RoomHomeStatisticsRepository(
    private val trackDao: TrackDao,
    libraryRepository: LibraryRepository,
    scope: CoroutineScope,
) : HomeStatisticsRepository {

    override val statistics: StateFlow<HomeStatistics> =
        libraryRepository.tracks
            .mapLatest { tracks ->
                val recentEntityIds = trackDao.findRecentlyPlayed(limit = 50)
                    .map { it.id }
                    .toSet()

                val tracksWithHistory = tracks.filter { it.id in recentEntityIds }
                val totalDurationMs = tracksWithHistory.sumOf { it.durationMs ?: 0L }

                HomeStatistics(
                    totalTracksEverPlayed = recentEntityIds.size,
                    totalListeningDurationMs = totalDurationMs,
                    tracksPlayedToday = estimatePlayedToday(recentEntityIds.size),
                    mostPlayedTrackIds = recentEntityIds.take(5).toList(),
                )
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), HomeStatistics(
                totalTracksEverPlayed = 0,
                totalListeningDurationMs = 0L,
                tracksPlayedToday = 0,
                mostPlayedTrackIds = emptyList(),
            ))

    companion object {
        private fun estimatePlayedToday(totalPlayed: Int): Int =
            (totalPlayed * 0.15).toInt().coerceAtMost(totalPlayed)
    }
}

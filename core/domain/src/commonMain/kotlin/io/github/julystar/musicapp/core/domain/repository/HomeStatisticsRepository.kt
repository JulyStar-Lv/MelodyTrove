package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.home.HomeStatistics
import io.github.julystar.musicapp.core.domain.home.ListeningPlaybackTrack
import io.github.julystar.musicapp.core.domain.home.ListeningStatisticsSnapshot
import kotlinx.coroutines.flow.StateFlow

interface HomeStatisticsRepository {
    val statistics: StateFlow<HomeStatistics>
    val listeningStatistics: StateFlow<ListeningStatisticsSnapshot>

    suspend fun recordPlay(
        track: ListeningPlaybackTrack,
        listenedMs: Long,
        playedAtEpochMs: Long,
    ): Long

    suspend fun addListenTime(historyEntryId: Long, listenedMs: Long)

    suspend fun removeHistoryEntry(historyEntryId: Long)
}

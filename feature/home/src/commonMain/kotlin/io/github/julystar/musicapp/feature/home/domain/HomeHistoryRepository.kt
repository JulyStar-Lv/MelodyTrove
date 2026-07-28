package io.github.julystar.musicapp.feature.home.domain

import kotlinx.coroutines.flow.StateFlow

interface HomeHistoryRepository {
    val recentPlays: StateFlow<List<HistoryPlayItem>>

    suspend fun recordPlay(trackId: Long, playedAtEpochMs: Long)
}

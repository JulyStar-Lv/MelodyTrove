package com.github.tidetunes.service.playback.data

import com.github.tidetunes.feature.home.domain.HomeStatisticsRepository
import com.github.tidetunes.feature.home.domain.ListeningPlaybackTrack

/**
 * Mirrors Halcyon's listening-statistics rules: a play counts after 20 seconds, then actual
 * listening time is persisted in small batches and flushed on pause or track change.
 */
internal class PlayerPlaybackStatisticsTracker(
    private val repository: HomeStatisticsRepository,
    private val minimumCountedListenMs: Long = 20_000L,
    private val flushIntervalMs: Long = 5_000L,
) {
    private var currentTrack: ListeningPlaybackTrack? = null
    private var countedHistoryEntryId: Long? = null
    private var pendingListenMs = 0L
    private var lastTickMs: Long? = null

    suspend fun update(
        monotonicNowMs: Long,
        wallClockNowMs: Long,
        track: ListeningPlaybackTrack?,
        isPlaying: Boolean,
    ) {
        if (track?.trackId != currentTrack?.trackId) {
            flush()
            currentTrack = track
            countedHistoryEntryId = null
            lastTickMs = monotonicNowMs
            return
        }

        if (track != null && isPlaying) {
            lastTickMs?.let { previousTickMs ->
                pendingListenMs += (monotonicNowMs - previousTickMs).coerceIn(0L, 1_500L)
            }
            if (countedHistoryEntryId == null && pendingListenMs >= minimumCountedListenMs) {
                countedHistoryEntryId = repository.recordPlay(
                    track = track,
                    listenedMs = pendingListenMs,
                    playedAtEpochMs = wallClockNowMs,
                )
                pendingListenMs = 0L
            } else if (countedHistoryEntryId != null && pendingListenMs >= flushIntervalMs) {
                flush()
            }
        } else {
            flush()
        }
        lastTickMs = monotonicNowMs
    }

    private suspend fun flush() {
        val historyEntryId = countedHistoryEntryId
        if (historyEntryId != null && pendingListenMs > 0L) {
            repository.addListenTime(historyEntryId, pendingListenMs)
        }
        pendingListenMs = 0L
    }
}

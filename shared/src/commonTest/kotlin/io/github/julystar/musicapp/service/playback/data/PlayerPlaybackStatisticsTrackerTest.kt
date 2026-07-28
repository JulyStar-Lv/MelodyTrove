package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.feature.home.domain.HomeStatistics
import io.github.julystar.musicapp.feature.home.domain.HomeStatisticsRepository
import io.github.julystar.musicapp.feature.home.domain.ListeningPlaybackTrack
import io.github.julystar.musicapp.feature.home.domain.ListeningStatisticsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerPlaybackStatisticsTrackerTest {
    @Test
    fun playIsCountedAfterTwentySecondsAndTimeIsFlushedInBatches() = runTest {
        val repository = RecordingStatisticsRepository()
        val tracker = PlayerPlaybackStatisticsTracker(repository)
        val track = ListeningPlaybackTrack(7L, "Moon", 180_000L)

        tracker.update(0L, 1_000_000L, track, isPlaying = true)
        for (second in 1L..19L) {
            tracker.update(second * 1_000L, 1_000_000L + second * 1_000L, track, true)
        }
        assertEquals(emptyList(), repository.recorded)

        tracker.update(20_000L, 1_020_000L, track, isPlaying = true)
        assertEquals(listOf(20_000L), repository.recorded.map(RecordedPlay::listenedMs))

        for (second in 21L..26L) {
            tracker.update(second * 1_000L, 1_000_000L + second * 1_000L, track, true)
        }
        assertEquals(listOf(5_000L), repository.addedTimes)

        tracker.update(27_000L, 1_027_000L, track, isPlaying = false)
        assertEquals(listOf(5_000L, 1_000L), repository.addedTimes)
    }

    @Test
    fun shortListenIsDiscardedWhenTrackChanges() = runTest {
        val repository = RecordingStatisticsRepository()
        val tracker = PlayerPlaybackStatisticsTracker(repository)
        val first = ListeningPlaybackTrack(1L, "One", 180_000L)
        val second = ListeningPlaybackTrack(2L, "Two", 180_000L)

        tracker.update(0L, 1_000_000L, first, true)
        for (secondIndex in 1L..10L) {
            tracker.update(secondIndex * 1_000L, 1_000_000L + secondIndex * 1_000L, first, true)
        }
        tracker.update(11_000L, 1_011_000L, second, true)

        assertEquals(emptyList(), repository.recorded)
        assertEquals(emptyList(), repository.addedTimes)
    }
}

private data class RecordedPlay(
    val track: ListeningPlaybackTrack,
    val listenedMs: Long,
    val playedAtEpochMs: Long,
)

private class RecordingStatisticsRepository : HomeStatisticsRepository {
    override val statistics = MutableStateFlow(
        HomeStatistics(0, 0L, 0, emptyList()),
    )
    override val listeningStatistics = MutableStateFlow(ListeningStatisticsSnapshot())
    val recorded = mutableListOf<RecordedPlay>()
    val addedTimes = mutableListOf<Long>()

    override suspend fun recordPlay(
        track: ListeningPlaybackTrack,
        listenedMs: Long,
        playedAtEpochMs: Long,
    ): Long {
        recorded += RecordedPlay(track, listenedMs, playedAtEpochMs)
        return recorded.size.toLong()
    }

    override suspend fun addListenTime(historyEntryId: Long, listenedMs: Long) {
        addedTimes += listenedMs
    }

    override suspend fun removeHistoryEntry(historyEntryId: Long) = Unit
}

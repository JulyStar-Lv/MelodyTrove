package com.github.tidetunes.service.playback.presentation.nowplaying

import com.github.tidetunes.core.domain.model.LyricLine
import com.github.tidetunes.core.domain.model.LyricWord
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NowPlayingLyricsAdapterTest {
    @Test
    fun convertsTimestampLinesIntoContinuousTimeline() {
        val lyrics = listOf(
            LyricLine(duration = 1.seconds, text = "First"),
            LyricLine(duration = 3.seconds, text = "Second"),
        ).toSyncedLyrics(trackTitle = "Song", trackDurationMs = 5_000)

        assertEquals("Song", lyrics.title)
        assertEquals(2, lyrics.lines.size)
        assertEquals(1_000, lyrics.lines[0].start)
        assertEquals(3_000, lyrics.lines[0].end)
        assertEquals(3_000, lyrics.lines[1].start)
        assertEquals(5_000, lyrics.lines[1].end)
        assertIs<SyncedLine>(lyrics.lines[0])
    }

    @Test
    fun preservesWordTimingAsAbsoluteKaraokeSyllables() {
        val lyrics = listOf(
            LyricLine(
                duration = 2.seconds,
                text = "Hello world",
                words = persistentListOf(
                    LyricWord("Hello", 0.milliseconds, 400.milliseconds),
                    LyricWord("world", 500.milliseconds, 500.milliseconds),
                ),
            ),
        ).toSyncedLyrics(trackTitle = "Song", trackDurationMs = 4_000)

        val line = assertIs<KaraokeLine.MainKaraokeLine>(lyrics.lines.single())
        assertEquals("Hello ", line.syllables[0].content)
        assertEquals(2_000, line.syllables[0].start)
        assertEquals(2_400, line.syllables[0].end)
        assertEquals(2_500, line.syllables[1].start)
        assertEquals(3_000, line.syllables[1].end)
    }
}

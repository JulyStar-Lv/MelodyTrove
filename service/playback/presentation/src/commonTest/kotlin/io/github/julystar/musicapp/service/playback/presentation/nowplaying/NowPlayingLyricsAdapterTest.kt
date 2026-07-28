package io.github.julystar.musicapp.service.playback.presentation.nowplaying

import io.github.julystar.musicapp.core.domain.model.LyricLine
import io.github.julystar.musicapp.core.domain.model.LyricDisplaySettings
import io.github.julystar.musicapp.core.domain.model.LyricWord
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
    fun filtersHeaderTagsAndConfiguredBlacklist() {
        val lines = listOf(
            LyricLine(0.milliseconds, "[ar:Artist]"),
            LyricLine(1_000.milliseconds, "Instrumental"),
            LyricLine(2_000.milliseconds, "Keep me"),
        )

        val visible = lines.filterVisibleLyrics(
            LyricDisplaySettings.Default.copy(lineBlacklist = listOf("Instrumental")),
        )

        assertEquals(listOf("Keep me"), visible.map(LyricLine::text))
    }

    @Test
    fun splitsUnsynchronisedBlocksBeforeFiltering() {
        val lines = listOf(LyricLine(0.milliseconds, "[ti:Song]\nFirst\nSecond"))

        val visible = lines.filterVisibleLyrics(LyricDisplaySettings.Default)

        assertEquals(listOf("First", "Second"), visible.map(LyricLine::text))
    }
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
    fun preservesSecondaryTextAsTranslationForTimedLyrics() {
        val lyrics = listOf(
            LyricLine(duration = 1.seconds, text = "Hello\n你好"),
            LyricLine(duration = 3.seconds, text = "World\n世界"),
        ).toSyncedLyrics(trackTitle = "Song", trackDurationMs = 5_000)

        val first = assertIs<SyncedLine>(lyrics.lines.first())
        assertEquals("Hello", first.content)
        assertEquals("你好", first.translation)
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

    @Test
    fun doesNotDuplicateSpacesAlreadyContainedInTimedWords() {
        val lyrics = listOf(
            LyricLine(
                duration = 2.seconds,
                text = "Hello world",
                words = persistentListOf(
                    LyricWord("Hello", 0.milliseconds, 400.milliseconds),
                    LyricWord(" world", 500.milliseconds, 500.milliseconds),
                ),
            ),
        ).toSyncedLyrics(trackTitle = "Song", trackDurationMs = 4_000)

        val line = assertIs<KaraokeLine.MainKaraokeLine>(lyrics.lines.single())
        assertEquals("Hello", line.syllables[0].content)
        assertEquals(" world", line.syllables[1].content)
    }
}

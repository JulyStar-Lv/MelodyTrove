package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.database.LyricsEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistedLyricsTest {
    @Test
    fun restoresEnhancedLrcAsWordTimedLyrics() {
        val lyrics = LyricsEntity(
            trackId = 1,
            format = "LRC",
            language = null,
            synchronized = true,
            content = "[00:02.00]<00:02.000>Hello<00:02.500> world<00:03.000>",
            sourcePath = null,
            updatedAt = 2,
        ).toPlaybackLyrics()

        assertEquals(LyricsLoadState.Loaded, lyrics.loadState)
        val line = lyrics.lines.single()
        assertEquals(2_000, line.duration.inWholeMilliseconds)
        assertEquals("Hello world", line.text)
        assertEquals(2, line.words.size)
        assertEquals(0, line.words[0].startOffset.inWholeMilliseconds)
        assertEquals(500, line.words[0].duration.inWholeMilliseconds)
        assertEquals(500, line.words[1].startOffset.inWholeMilliseconds)
        assertEquals(500, line.words[1].duration.inWholeMilliseconds)
    }

    @Test
    fun keepsPlainLrcAsLineTimedLyrics() {
        val lyrics = LyricsEntity(
            trackId = 1,
            format = "LRC",
            language = null,
            synchronized = true,
            content = "[00:01.00]First\n[00:03.00]Second",
            sourcePath = null,
            updatedAt = 2,
        ).toPlaybackLyrics()

        assertEquals(listOf("First", "Second"), lyrics.lines.map { line -> line.text })
        assertEquals(listOf(1_000L, 3_000L), lyrics.lines.map { line -> line.duration.inWholeMilliseconds })
        assertEquals(true, lyrics.lines.all { line -> line.words.isEmpty() })
    }

    @Test
    fun fallsBackForLegacySingleDigitMinuteTags() {
        val lyrics = LyricsEntity(
            trackId = 1,
            format = "LRC",
            language = null,
            synchronized = true,
            content = "[1:02.00]Legacy",
            sourcePath = null,
            updatedAt = 2,
        ).toPlaybackLyrics()

        assertEquals("Legacy", lyrics.lines.single().text)
        assertEquals(62_000, lyrics.lines.single().duration.inWholeMilliseconds)
    }
}

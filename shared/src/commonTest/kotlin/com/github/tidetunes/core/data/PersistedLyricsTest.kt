package com.github.tidetunes.core.data

import com.github.tidetunes.core.domain.model.LyricsLoadState
import com.github.tidetunes.core.domain.model.LyricDisplaySettings
import com.github.tidetunes.core.domain.model.LyricSourceKind
import com.github.tidetunes.core.domain.model.LyricSourceMode
import com.github.tidetunes.database.LyricsEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistedLyricsTest {
    @Test
    fun selectsConfiguredSourcePriorityAndMode() {
        val embedded = lyricEntity("EmbeddedPlain", 1)
        val externalTtml = lyricEntity("ExternalTtml", 2)
        val candidates = listOf(embedded, externalTtml)

        val automatic = candidates.selectLyrics(
            LyricDisplaySettings.Default.copy(
                sourcePriority = listOf(
                    LyricSourceKind.ExternalTtml,
                    LyricSourceKind.EmbeddedPlain,
                    LyricSourceKind.EmbeddedTtml,
                    LyricSourceKind.ExternalPlain,
                ),
            ),
        )
        val embeddedOnly = candidates.selectLyrics(
            LyricDisplaySettings.Default.copy(sourceMode = LyricSourceMode.Embedded),
        )

        assertEquals("ExternalTtml", automatic?.sourceKind)
        assertEquals("EmbeddedPlain", embeddedOnly?.sourceKind)
    }

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

    private fun lyricEntity(sourceKind: String, updatedAt: Long) = LyricsEntity(
        trackId = 1,
        format = if (sourceKind.endsWith("Ttml")) "TTML" else "LRC",
        language = null,
        synchronized = true,
        content = "[00:01.00]Line",
        sourcePath = if (sourceKind.startsWith("External")) "external:test" else "embedded",
        updatedAt = updatedAt,
        sourceKind = sourceKind,
    )
}

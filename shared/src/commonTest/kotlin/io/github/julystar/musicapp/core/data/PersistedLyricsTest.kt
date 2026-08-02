package io.github.julystar.musicapp.core.data

import io.github.julystar.musicapp.core.domain.model.LyricsLoadState
import io.github.julystar.musicapp.core.domain.model.LyricDisplaySettings
import io.github.julystar.musicapp.core.domain.model.LyricSourceKind
import io.github.julystar.musicapp.core.domain.model.LyricSourceMode
import io.github.julystar.musicapp.database.LyricsEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        val entity = LyricsEntity(
            trackId = 1,
            format = "LRC",
            language = null,
            synchronized = true,
            content = "[00:02.00]<00:02.000>Hello<00:02.500> world<00:03.000>",
            sourcePath = null,
            updatedAt = 2,
        )
        val lyrics = entity.toPlaybackLyrics()

        assertEquals(LyricsLoadState.Loaded, lyrics.loadState)
        assertEquals(LyricSourceKind.EmbeddedWordTimed, entity.resolvedSourceKind())
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
    fun onlyRequestsPluginWhenExternalQualityPrecedesPlainFallback() {
        val embeddedPlain = listOf(lyricEntity("EmbeddedPlain", 1))

        assertFalse(
            embeddedPlain.shouldLookupPreferredExternalLyrics(LyricDisplaySettings.Default),
        )
        assertTrue(
            embeddedPlain.shouldLookupPreferredExternalLyrics(
                LyricDisplaySettings.Default.copy(
                    sourcePriority = listOf(
                        LyricSourceKind.ExternalWordTimed,
                        LyricSourceKind.ExternalTtml,
                        LyricSourceKind.EmbeddedPlain,
                        LyricSourceKind.ExternalPlain,
                        LyricSourceKind.EmbeddedTtml,
                        LyricSourceKind.EmbeddedWordTimed,
                    ),
                ),
            ),
        )
        assertFalse(
            listOf(lyricEntity("ExternalWordTimed", 2)).shouldLookupPreferredExternalLyrics(
                LyricDisplaySettings.Default.copy(sourceMode = LyricSourceMode.External),
            ),
        )
    }

    @Test
    fun parsesLegacyTtmlEvenWhenStoredAsUnsynchronized() {
        val lyrics = LyricsEntity(
            trackId = 1,
            format = "TTML",
            language = null,
            synchronized = false,
            content = """
                <tt xmlns="http://www.w3.org/ns/ttml"><body><div>
                    <p begin="00:01.000" end="00:02.000">
                        <span begin="00:01.000" end="00:02.000">Line</span>
                    </p>
                </div></body></tt>
            """.trimIndent(),
            sourcePath = "embedded",
            updatedAt = 2,
            sourceKind = "EmbeddedTtml",
        ).toPlaybackLyrics()

        assertEquals("Line", lyrics.lines.single().text)
        assertTrue(lyrics.lines.single().words.isNotEmpty())
    }

    @Test
    fun preservesTtmlTranslationForPlayback() {
        val lyrics = LyricsEntity(
            trackId = 1,
            format = "TTML",
            language = null,
            synchronized = true,
            content = """
                <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                    <body><div>
                        <p begin="00:01.000" end="00:02.000">
                            <span begin="00:01.000" end="00:02.000">Hello</span>
                            <span ttm:role="x-translation">你好</span>
                        </p>
                    </div></body>
                </tt>
            """.trimIndent(),
            sourcePath = "external:test",
            updatedAt = 2,
            sourceKind = "ExternalTtml",
        ).toPlaybackLyrics()

        assertEquals("Hello\n你好", lyrics.lines.single().text)
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

package io.github.julystar.musicapp.core.domain.model

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class LyricFilteringTest {
    @Test
    fun ignoresCommonHeaderVariantsWhenEnabled() {
        val content = """
            ﻿[ar:Artist]
            [ encoding : UTF-8]
            [provider：Example]
            [bg:Backing vocal]
            Keep me
        """.trimIndent()

        assertEquals(
            listOf("[bg:Backing vocal]", "Keep me"),
            LyricDisplaySettings.Default.filterLyricTextBlock(content),
        )
    }

    @Test
    fun keepsHeaderTagsWhenFilteringIsDisabled() {
        val settings = LyricDisplaySettings.Default.copy(ignoreHeaderTags = false)

        assertEquals(
            listOf("[ar:Artist]", "Keep me"),
            settings.filterLyricTextBlock("[ar:Artist]\nKeep me"),
        )
    }

    @Test
    fun filtersHeadersInsideUnsynchronisedLyricsForPlatformOutput() {
        val lyrics = Lyrics(
            lines = persistentListOf(
                LyricLine(0.milliseconds, "[ti:Song]\n[provider:Example]\nFirst\nSecond"),
            ),
            loadState = LyricsLoadState.Loaded,
        )

        val filtered = lyrics.filteredForDisplay(LyricDisplaySettings.Default)

        assertEquals("First\nSecond", filtered.lines.single().text)
    }
}

package io.github.julystar.musicapp.feature.settings.presentation

import io.github.julystar.musicapp.core.domain.model.DEFAULT_LYRIC_SOURCE_PRIORITY
import io.github.julystar.musicapp.core.domain.model.LyricSourceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LyricSourcePriorityTest {
    @Test
    fun `moves a lyric source to a new priority`() {
        val updated = DEFAULT_LYRIC_SOURCE_PRIORITY.moveLyricSource(4, 1)

        assertEquals(
            listOf(
                LyricSourceKind.EmbeddedTtml,
                LyricSourceKind.ExternalWordTimed,
                LyricSourceKind.EmbeddedWordTimed,
                LyricSourceKind.EmbeddedPlain,
                LyricSourceKind.ExternalTtml,
                LyricSourceKind.ExternalPlain,
            ),
            updated,
        )
    }

    @Test
    fun `keeps the same priority for invalid or unchanged moves`() {
        assertSame(
            DEFAULT_LYRIC_SOURCE_PRIORITY,
            DEFAULT_LYRIC_SOURCE_PRIORITY.moveLyricSource(0, 0),
        )
        assertSame(
            DEFAULT_LYRIC_SOURCE_PRIORITY,
            DEFAULT_LYRIC_SOURCE_PRIORITY.moveLyricSource(-1, 2),
        )
    }
}

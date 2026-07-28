package io.github.julystar.musicapp.core.lyrics.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class InterpolatedPlaybackPositionTest {
    @Test
    fun ignoresSmallClockJitter() {
        assertEquals(
            expected = 1_000.0,
            actual = correctInterpolatedPlaybackPosition(
                externalPositionMs = 980.0,
                renderedPositionMs = 1_000.0,
            ),
        )
    }

    @Test
    fun easesMediumClockDrift() {
        assertEquals(
            expected = 1_025.0,
            actual = correctInterpolatedPlaybackPosition(
                externalPositionMs = 1_100.0,
                renderedPositionMs = 1_000.0,
            ),
        )
    }

    @Test
    fun snapsAfterSeekOrLargeDrift() {
        assertEquals(
            expected = 2_000.0,
            actual = correctInterpolatedPlaybackPosition(
                externalPositionMs = 2_000.0,
                renderedPositionMs = 1_000.0,
            ),
        )
    }

    @Test
    fun snapsAfterBackwardSeek() {
        assertEquals(
            expected = 1_000.0,
            actual = correctInterpolatedPlaybackPosition(
                externalPositionMs = 1_000.0,
                renderedPositionMs = 20_000.0,
            ),
        )
    }

    @Test
    fun snapsLyricsScrollAfterSeekAcrossLines() {
        assertEquals(true, shouldSnapLyricsScroll(previousIndex = 8, currentIndex = 21))
        assertEquals(true, shouldSnapLyricsScroll(previousIndex = 21, currentIndex = 4))
    }

    @Test
    fun keepsAnimationForNormalAdjacentLineChanges() {
        assertEquals(false, shouldSnapLyricsScroll(previousIndex = 8, currentIndex = 9))
    }
}

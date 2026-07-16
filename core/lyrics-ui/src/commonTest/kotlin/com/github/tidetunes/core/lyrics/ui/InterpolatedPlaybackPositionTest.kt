package com.github.tidetunes.core.lyrics.ui

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
}

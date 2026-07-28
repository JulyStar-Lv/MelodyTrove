package io.github.julystar.musicapp.core.presentation.layout

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizeClassTest {

    @Test
    fun `uses bottom navigation below 600dp`() {
        assertEquals(WindowSizeClass.Compact, windowSizeClassFor(390.dp))
        assertEquals(WindowSizeClass.Compact, windowSizeClassFor(599.dp))
    }

    @Test
    fun `uses rail from 600dp through 1279dp`() {
        assertEquals(WindowSizeClass.Medium, windowSizeClassFor(600.dp))
        assertEquals(WindowSizeClass.Expanded, windowSizeClassFor(840.dp))
        assertEquals(WindowSizeClass.Expanded, windowSizeClassFor(1279.dp))
    }

    @Test
    fun `uses desktop sidebar from 1280dp`() {
        assertEquals(WindowSizeClass.Large, windowSizeClassFor(1280.dp))
    }
}

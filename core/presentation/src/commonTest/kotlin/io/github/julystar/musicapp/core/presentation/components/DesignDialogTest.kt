package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DesignDialogTest {

    @Test
    fun `dialog max height preserves a smaller caller limit`() {
        assertEquals(480.dp, resolveDialogMaxHeight(480.dp, 900.dp))
    }

    @Test
    fun `dialog max height caps a larger caller limit to the standard`() {
        assertEquals(640.dp, resolveDialogMaxHeight(720.dp, 900.dp))
    }

    @Test
    fun `dialog max height is capped to the viewport`() {
        assertEquals(516.dp, resolveDialogMaxHeight(720.dp, 600.dp))
    }

    @Test
    fun `dialog max height falls back to the standard limit when viewport is unavailable`() {
        assertEquals(640.dp, resolveDialogMaxHeight(null, Dp.Unspecified))
    }

    @Test
    fun `compact dialog layout is used only below 600 dp`() {
        assertEquals(true, DesignDialogDefaults.isCompactWindow(599.dp))
        assertEquals(false, DesignDialogDefaults.isCompactWindow(600.dp))
        assertEquals(false, DesignDialogDefaults.isCompactWindow(Dp.Unspecified))
    }
}

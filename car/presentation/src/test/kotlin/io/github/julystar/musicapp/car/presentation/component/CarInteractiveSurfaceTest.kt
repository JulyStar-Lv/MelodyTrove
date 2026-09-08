package io.github.julystar.musicapp.car.presentation.component

import kotlin.test.Test
import kotlin.test.assertEquals

class CarInteractiveSurfaceTest {
    @Test
    fun statePriorityMatchesAutomotiveContract() {
        assertEquals(CarSurfaceBaseState.Disabled, resolveCarSurfaceState(false, true, true, true, true).base)
        assertEquals(CarSurfaceBaseState.Pressed, resolveCarSurfaceState(true, true, true, true, true).base)
        assertEquals(CarSurfaceBaseState.Selected, resolveCarSurfaceState(true, false, true, true, true).base)
        assertEquals(CarSurfaceBaseState.Playing, resolveCarSurfaceState(true, false, false, false, true).base)
        assertEquals(CarSurfaceBaseState.Default, resolveCarSurfaceState(true, false, false, false, false).base)
    }

    @Test
    fun focusBorderRemainsVisibleForSelectedAndPlayingItems() {
        assertEquals(true, resolveCarSurfaceState(true, false, true, true, false).showFocusBorder)
        assertEquals(true, resolveCarSurfaceState(true, false, true, false, true).showFocusBorder)
        assertEquals(false, resolveCarSurfaceState(false, false, true, true, false).showFocusBorder)
    }
}

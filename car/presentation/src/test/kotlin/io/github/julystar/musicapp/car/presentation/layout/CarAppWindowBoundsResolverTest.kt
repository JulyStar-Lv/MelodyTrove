package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarAppWindowBoundsResolverTest {
    @Test
    fun expandedMatchesFileManagerPanelOnDriverCanvas() {
        val bounds = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(2560, 1440),
            requestedHint = CarLayoutProfileHint.Expanded,
        )

        assertEquals(64, bounds.left)
        assertEquals(192, bounds.top)
        assertEquals(2496, bounds.width)
        assertEquals(1080, bounds.height)
        assertEquals(CarLayoutProfileHint.Expanded, bounds.profileHint)
        assertTrue(bounds.embeddedInCockpit)
    }

    @Test
    fun vehiclePanelMatchesFileManagerPanelOnFullCockpitCanvas() {
        val bounds = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(5120, 1440),
            requestedHint = CarLayoutProfileHint.VehiclePanel,
        )

        assertEquals(832, bounds.left)
        assertEquals(192, bounds.top)
        assertEquals(1728, bounds.width)
        assertEquals(1080, bounds.height)
        assertEquals(CarLayoutProfileHint.VehiclePanel, bounds.profileHint)
        assertTrue(bounds.embeddedInCockpit)
    }

    @Test
    fun missingOemSignalDefaultsToFileManagerExpandedState() {
        val bounds = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(2560, 1440),
            requestedHint = CarLayoutProfileHint.Automatic,
        )

        assertEquals(64, bounds.left)
        assertEquals(2496, bounds.width)
        assertEquals(CarLayoutProfileHint.Expanded, bounds.profileHint)
    }

    @Test
    fun standaloneReferenceWindowsKeepTheirEntireSurface() {
        val expanded = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(2496, 1080),
            requestedHint = CarLayoutProfileHint.Expanded,
        )
        val vehiclePanel = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(1728, 1080),
            requestedHint = CarLayoutProfileHint.VehiclePanel,
        )

        assertEquals(CarAppWindowBounds(0, 0, 2496, 1080, false, CarLayoutProfileHint.Automatic), expanded)
        assertEquals(CarAppWindowBounds(0, 0, 1728, 1080, false, CarLayoutProfileHint.Automatic), vehiclePanel)
        assertFalse(expanded.embeddedInCockpit)
        assertFalse(vehiclePanel.embeddedInCockpit)
    }
}

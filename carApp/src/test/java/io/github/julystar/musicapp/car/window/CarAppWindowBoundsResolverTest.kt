package io.github.julystar.musicapp.car.window

import androidx.compose.ui.unit.IntSize
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint
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
    fun fullscreenUsesTheWholeCockpitWidthAndLeavesTheVehicleDockVisible() {
        val bounds = CarAppWindowBoundsResolver.resolve(
            hostSizePx = IntSize(5120, 1440),
            requestedHint = CarLayoutProfileHint.FullscreenCockpit,
        )

        assertEquals(CarAppWindowBounds(0, 0, 5120, 1304, true, CarLayoutProfileHint.FullscreenCockpit), bounds)
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

    @Test
    fun oemStateMappingKeepsAutomaticForMissingSignal() {
        assertEquals(CarLayoutProfileHint.Expanded, carLayoutHintFromOemState(0, 0))
        assertEquals(CarLayoutProfileHint.VehiclePanel, carLayoutHintFromOemState(1, 0))
        assertEquals(CarLayoutProfileHint.VehiclePanel, carLayoutHintFromOemState(0, 1))
        assertEquals(CarLayoutProfileHint.Automatic, carLayoutHintFromOemState(null, null))
        assertEquals(CarLayoutProfileHint.Expanded, carLayoutHintFromOemState(0, null))
        assertEquals(CarLayoutProfileHint.VehiclePanel, carLayoutHintFromOemState(1, null))
        assertEquals(CarLayoutProfileHint.VehiclePanel, carLayoutHintFromOemState(null, 1))
    }
}

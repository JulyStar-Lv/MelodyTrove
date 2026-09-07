package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarLayoutProfileResolverTest {
    private val usableSize = DpSize(1000.dp, 600.dp)

    @Test
    fun automaticResolutionUsesExpandedUntilEvidenceIsInjected() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize,
            insets = CarLayoutInsets(left = 32.dp, top = 10.dp, right = 8.dp, bottom = 20.dp),
        )

        assertEquals(CarLayoutProfile.Expanded, metrics.profile)
        assertTrue(metrics.metricsAvailable)
        assertEquals(960.dp, metrics.contentSize.width)
        assertEquals(570.dp, metrics.contentSize.height)
        assertEquals(570.dp * (24f / 1080f), metrics.shellVerticalPadding)
        assertTrue(metrics.libraryGap > 0.dp)
        assertTrue(metrics.shellHorizontalPadding > 0.dp)
    }

    @Test
    fun explicitVehiclePanelIsADeferredBoundary() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize = usableSize,
            hint = CarLayoutProfileHint.VehiclePanel,
        )

        assertEquals(CarLayoutProfile.VehiclePanel, metrics.profile)
        assertFalse(metrics.metricsAvailable)
        assertEquals(Dp.Unspecified, metrics.shellVerticalPadding)
        assertEquals(Dp.Unspecified, metrics.headerHeight)
        assertEquals(Dp.Unspecified, metrics.libraryGap)
        assertEquals(Dp.Unspecified, metrics.primaryTouchTarget)
    }

    @Test
    fun invalidConstraintsFailWithoutGuessing() {
        assertFailsWith<IllegalArgumentException> {
            CarLayoutProfileResolver().resolve(DpSize(0.dp, 600.dp))
        }
        assertFailsWith<IllegalArgumentException> {
            CarLayoutProfileResolver().resolve(
                usableSize = usableSize,
                insets = CarLayoutInsets(left = 1001.dp),
            )
        }
    }

    @Test
    fun injectedStrategyControlsAutomaticProfile() {
        val metrics = CarLayoutProfileResolver(
            strategy = CarLayoutProfileStrategy { _, _ -> CarLayoutProfile.VehiclePanel },
        ).resolve(usableSize)

        assertEquals(CarLayoutProfile.VehiclePanel, metrics.profile)
        assertFalse(metrics.metricsAvailable)
    }

    @Test
    fun expandedTouchTargetNeverDropsBelowAccessibilityMinimum() {
        val metrics = CarLayoutProfileResolver().resolve(DpSize(1.dp, 1.dp))

        assertEquals(48.dp, metrics.primaryTouchTarget)
    }

    @Test
    fun oemStateMappingIsPureAndKeepsAutomaticForMissingSignal() {
        assertEquals(
            CarLayoutProfileHint.Expanded,
            carLayoutHintFromOemState(keyScreenShow = 0, keyVpaCuiShowLeft = 0),
        )
        assertEquals(
            CarLayoutProfileHint.VehiclePanel,
            carLayoutHintFromOemState(keyScreenShow = 1, keyVpaCuiShowLeft = 0),
        )
        assertEquals(
            CarLayoutProfileHint.VehiclePanel,
            carLayoutHintFromOemState(keyScreenShow = 0, keyVpaCuiShowLeft = 1),
        )
        assertEquals(
            CarLayoutProfileHint.Automatic,
            carLayoutHintFromOemState(keyScreenShow = null, keyVpaCuiShowLeft = null),
        )
        assertEquals(
            CarLayoutProfileHint.Expanded,
            carLayoutHintFromOemState(keyScreenShow = 0, keyVpaCuiShowLeft = null),
        )
        assertEquals(
            CarLayoutProfileHint.VehiclePanel,
            carLayoutHintFromOemState(keyScreenShow = 1, keyVpaCuiShowLeft = null),
        )
        assertEquals(
            CarLayoutProfileHint.VehiclePanel,
            carLayoutHintFromOemState(keyScreenShow = null, keyVpaCuiShowLeft = 1),
        )
    }
}

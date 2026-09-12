package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CarLayoutProfileResolverTest {
    private val usableSize = DpSize(1500.dp, 600.dp)

    @Test
    fun automaticResolutionUsesExpandedUntilEvidenceIsInjected() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize,
            insets = CarLayoutInsets(left = 32.dp, top = 10.dp, right = 8.dp, bottom = 20.dp),
        )

        assertEquals(CarLayoutProfile.Expanded, metrics.profile)
        assertTrue(metrics.metricsAvailable)
        assertEquals(1460.dp, metrics.contentSize.width)
        assertEquals(570.dp, metrics.contentSize.height)
        assertEquals(570.dp * (24f / 1080f), metrics.shellVerticalPadding)
        assertTrue(metrics.libraryGap > 0.dp)
        assertTrue(metrics.shellHorizontalPadding > 0.dp)
    }

    @Test
    fun explicitVehiclePanelUsesTheCompleteCompactMetrics() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize = usableSize,
            hint = CarLayoutProfileHint.VehiclePanel,
        )

        assertEquals(CarLayoutProfile.VehiclePanel, metrics.profile)
        assertTrue(metrics.metricsAvailable)
        assertEquals(3, metrics.mediaGridColumns)
        assertTrue(metrics.navigationRailWidth > 0.dp)
        assertTrue(metrics.detailHeroWidth > 0.dp)
    }

    @Test
    fun expandedReferenceKeepsFigmaHomeAndGridGeometry() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize = DpSize(2496.dp, 1080.dp),
            hint = CarLayoutProfileHint.Expanded,
        )

        assertDpEquals(576.dp, metrics.homeQuickCardWidth)
        assertDpEquals(164.dp, metrics.homeSearchCardWidth)
        assertDpEquals(640.dp, metrics.homeRecentFeatureWidth)
        assertEquals(3, metrics.homeRecentColumns)
        assertDpEquals(332.dp, metrics.homeRecommendationCardWidth)
        assertDpEquals(32.dp, metrics.libraryPanePadding)
        assertDpEquals(24.dp, metrics.mediaGridPadding)
        assertDpEquals(16.dp, metrics.mediaGridHorizontalGap)
        assertDpEquals(40.dp, metrics.detailContentEndMargin)
        assertDpEquals(0.dp, metrics.nowPlayingContentStartOffset)
        assertDpEquals(136.dp, metrics.playlistSelectedCardHeight)
        assertDpEquals(240.dp, metrics.playlistCardWidth)
        assertDpEquals(216.dp, metrics.playlistArtworkSize)
    }

    @Test
    fun vehicleReferenceKeepsFigmaAsymmetricPlayerAndCompactGeometry() {
        val metrics = CarLayoutProfileResolver().resolve(
            usableSize = DpSize(1728.dp, 1080.dp),
            hint = CarLayoutProfileHint.VehiclePanel,
        )

        assertDpEquals(380.dp, metrics.homeQuickCardWidth)
        assertDpEquals(184.dp, metrics.homeSearchCardWidth)
        assertDpEquals(480.dp, metrics.homeRecentFeatureWidth)
        assertEquals(2, metrics.homeRecentColumns)
        assertDpEquals(440.dp, metrics.homeRecommendationCardWidth)
        assertDpEquals(32.dp, metrics.mediaGridPadding)
        assertDpEquals(12.dp, metrics.mediaGridHorizontalGap)
        assertDpEquals(80.dp, metrics.nowPlayingContentStartOffset)
        assertEquals(4, metrics.artistVisibleTrackCount)
        assertEquals(1, metrics.playlistGridColumns)
        assertDpEquals(136.dp, metrics.playlistSelectedCardHeight)
        assertDpEquals(240.dp, metrics.playlistCardWidth)
        assertDpEquals(216.dp, metrics.playlistArtworkSize)
    }

    @Test
    fun invalidConstraintsFailWithoutGuessing() {
        assertFailsWith<IllegalArgumentException> {
            CarLayoutProfileResolver().resolve(DpSize(0.dp, 600.dp))
        }
        assertFailsWith<IllegalArgumentException> {
            CarLayoutProfileResolver().resolve(
                usableSize = usableSize,
                insets = CarLayoutInsets(left = 1501.dp),
            )
        }
    }

    @Test
    fun injectedStrategyControlsAutomaticProfile() {
        val metrics = CarLayoutProfileResolver(
            strategy = CarLayoutProfileStrategy { _, _ -> CarLayoutProfile.VehiclePanel },
        ).resolve(usableSize)

        assertEquals(CarLayoutProfile.VehiclePanel, metrics.profile)
        assertTrue(metrics.metricsAvailable)
    }

    @Test
    fun expandedTouchTargetNeverDropsBelowAccessibilityMinimum() {
        val metrics = CarLayoutProfileResolver().resolve(DpSize(1.dp, 1.dp), hint = CarLayoutProfileHint.Expanded)

        assertEquals(48.dp, metrics.primaryTouchTarget)
    }

    @Test
    fun automaticResolutionMapsAllObservedWindowShapesWithoutExactPixels() {
        val resolver = CarLayoutProfileResolver()

        assertEquals(CarLayoutProfile.Expanded, resolver.resolve(DpSize(2496.dp, 908.dp)).profile)
        assertEquals(CarLayoutProfile.VehiclePanel, resolver.resolve(DpSize(1728.dp, 908.dp)).profile)
        assertEquals(CarLayoutProfile.FullscreenCockpit, resolver.resolve(DpSize(5120.dp, 1304.dp)).profile)
        assertEquals(4, resolver.resolve(DpSize(2400.dp, 900.dp)).mediaGridColumns)
        assertEquals(3, resolver.resolve(DpSize(1700.dp, 900.dp)).mediaGridColumns)
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

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertEquals(expected.value, actual.value, 0.001f)
    }
}

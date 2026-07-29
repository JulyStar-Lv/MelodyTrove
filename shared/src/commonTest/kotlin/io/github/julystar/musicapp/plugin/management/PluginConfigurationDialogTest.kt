package io.github.julystar.musicapp.plugin.management

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginConfigurationDialogTest {
    @Test
    fun usesBottomSheetOnlyForCompactWindows() {
        assertTrue(isCompactPluginConfigurationDialog(599.dp))
        assertFalse(isCompactPluginConfigurationDialog(600.dp))
        assertFalse(isCompactPluginConfigurationDialog(1_008.dp))
    }

    @Test
    fun placesSelectMenuWhereItFitsInCompactWindows() {
        assertFalse(
            shouldPlacePluginConfigMenuAbove(
                anchorTop = 120.dp,
                anchorBottom = 188.dp,
                windowHeight = 800.dp,
                menuHeight = 184.dp,
            ),
        )
        assertTrue(
            shouldPlacePluginConfigMenuAbove(
                anchorTop = 500.dp,
                anchorBottom = 568.dp,
                windowHeight = 800.dp,
                menuHeight = 184.dp,
            ),
        )
        assertTrue(
            shouldPlacePluginConfigMenuAbove(
                anchorTop = 350.dp,
                anchorBottom = 418.dp,
                windowHeight = 600.dp,
                menuHeight = 184.dp,
            ),
        )
    }

    @Test
    fun dismissesCompactSheetAfterEnoughDistanceOrVelocity() {
        assertFalse(
            shouldDismissPluginConfigurationSheet(
                dragOffsetPx = 71f,
                velocityPxPerSecond = 899f,
                distanceThresholdPx = 72f,
                velocityThresholdPxPerSecond = 900f,
            ),
        )
        assertTrue(
            shouldDismissPluginConfigurationSheet(
                dragOffsetPx = 72f,
                velocityPxPerSecond = 0f,
                distanceThresholdPx = 72f,
                velocityThresholdPxPerSecond = 900f,
            ),
        )
        assertTrue(
            shouldDismissPluginConfigurationSheet(
                dragOffsetPx = 12f,
                velocityPxPerSecond = 900f,
                distanceThresholdPx = 72f,
                velocityThresholdPxPerSecond = 900f,
            ),
        )
    }
}

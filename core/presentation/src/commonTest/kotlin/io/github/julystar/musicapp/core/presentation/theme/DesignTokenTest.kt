package io.github.julystar.musicapp.core.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class DesignTokenTest {

    @Test
    fun `compact navigation preserves the design dimensions`() {
        val navigation = DesignNavigation()

        assertEquals(62.dp, navigation.compactBarHeight)
        assertEquals(1.dp, navigation.compactBarDividerHeight)
        assertEquals(48.dp, navigation.compactSelectedIndicatorWidth)
        assertEquals(28.dp, navigation.compactSelectedIndicatorHeight)
        assertEquals(20.dp, navigation.compactIconSize)
        assertEquals(10.sp, navigation.compactLabelSize)
    }

    @Test
    fun `adaptive and player dimensions preserve compact requirements`() {
        val adaptive = DesignAdaptive()
        val player = DesignPlayer()

        assertEquals(48.dp, adaptive.minimumTouchTarget)
        assertEquals(48.dp, adaptive.compactHeaderCollapseDistance)
        assertEquals(58.dp, adaptive.compactHeaderHeight)
        assertEquals(72.dp, player.miniBarHeight)
        assertEquals(76.dp, player.compactMiniBarHeight)
    }

    @Test
    fun `theme color tokens match the product design system`() {
        val picker = DesignColorPicker()
        val motion = DesignMotion()

        assertEquals(48.dp, picker.swatchSize)
        assertEquals(760.dp, picker.dialogMaxWidth)
        assertEquals(640.dp, picker.mediumDialogMaxWidth)
        assertEquals(720.dp, picker.contentMaxHeight)
        assertEquals(180.dp, picker.saturationValueHeight)
        assertEquals(20.dp, picker.indicatorSize)
        assertEquals(32.dp, picker.hueVisualHeight)
        assertEquals(12.dp, picker.gridGap)
        assertEquals(20.dp, picker.sectionGap)
        assertEquals(400, motion.themeMillis)
    }
}

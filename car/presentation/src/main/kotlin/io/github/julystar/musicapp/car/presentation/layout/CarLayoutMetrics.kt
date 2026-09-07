package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Immutable
data class CarLayoutInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

@Immutable
data class CarLayoutMetrics(
    val profile: CarLayoutProfile,
    val usableSize: DpSize,
    val contentSize: DpSize,
    val insets: CarLayoutInsets,
    val metricsAvailable: Boolean,
    val shellHorizontalPadding: Dp,
    val shellVerticalPadding: Dp,
    val headerHeight: Dp,
    val navigationRailWidth: Dp,
    val contentPaneGap: Dp,
    val libraryGap: Dp,
    val navigationItemHeight: Dp,
    val miniPlayerHeight: Dp,
    val primaryTouchTarget: Dp,
) {
    companion object {
        fun deferred(
            profile: CarLayoutProfile,
            usableSize: DpSize,
            insets: CarLayoutInsets,
            contentSize: DpSize,
        ) = CarLayoutMetrics(
            profile = profile,
            usableSize = usableSize,
            contentSize = contentSize,
            insets = insets,
            metricsAvailable = false,
            shellHorizontalPadding = Dp.Unspecified,
            shellVerticalPadding = Dp.Unspecified,
            headerHeight = Dp.Unspecified,
            navigationRailWidth = Dp.Unspecified,
            contentPaneGap = Dp.Unspecified,
            libraryGap = Dp.Unspecified,
            navigationItemHeight = Dp.Unspecified,
            miniPlayerHeight = Dp.Unspecified,
            primaryTouchTarget = Dp.Unspecified,
        )
    }
}

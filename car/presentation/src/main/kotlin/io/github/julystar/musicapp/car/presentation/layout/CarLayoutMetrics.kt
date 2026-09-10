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
    val shellWidth: Dp,
    val headerStart: Dp,
    val headerTop: Dp,
    val headerHeight: Dp,
    val navigationRailStart: Dp,
    val navigationRailTop: Dp,
    val navigationRailBottom: Dp,
    val navigationRailWidth: Dp,
    val navigationRailInnerPadding: Dp,
    val navigationPrimaryTop: Dp,
    val navigationItemInterval: Dp,
    val navigationLibraryLabelTop: Dp,
    val navigationLibraryTop: Dp,
    val miniPlayerBottom: Dp,
    val contentPaneGap: Dp,
    val contentHorizontalPadding: Dp,
    val contentTop: Dp,
    val libraryGap: Dp,
    val navigationItemHeight: Dp,
    val miniPlayerHeight: Dp,
    val iconSize: Dp,
    val albumCardHeight: Dp,
    val compactCardHeight: Dp,
    val quickActionHeight: Dp,
    val recommendationCardWidth: Dp,
    val recommendationCardHeight: Dp,
    val artistCardHeight: Dp,
    val cardGap: Dp,
    val nowPlayingHorizontalMargin: Dp,
    val nowPlayingVerticalMargin: Dp,
    val nowPlayingPlayerPaneWidth: Dp,
    val nowPlayingPaneGap: Dp,
    val nowPlayingArtworkSize: Dp,
    val nowPlayingInnerPadding: Dp,
    val progressTrackHeight: Dp,
    val detailContentMargin: Dp,
    val detailTopBarHeight: Dp,
    val detailPaneTop: Dp,
    val detailHeroWidth: Dp,
    val detailPaneGap: Dp,
    val detailRowHeight: Dp,
    val primaryTouchTarget: Dp,
    val mediaGridColumns: Int,
)

package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.theme.CarTouchTargets

/** Resolves a car shell from measured usable bounds; it never infers a profile from pixel thresholds. */
class CarLayoutProfileResolver(
    private val profileHint: CarLayoutProfileHint = CarLayoutProfileHint.Automatic,
    private val strategy: CarLayoutProfileStrategy = CarLayoutProfileStrategy { _, _ -> CarLayoutProfile.Expanded },
) {
    fun resolve(
        usableSize: DpSize,
        insets: CarLayoutInsets = CarLayoutInsets(),
        hint: CarLayoutProfileHint = profileHint,
    ): CarLayoutMetrics {
        require(usableSize.width.value.isFinite() && usableSize.height.value.isFinite()) {
            "usableSize must contain finite dimensions"
        }
        require(usableSize.width > 0.dp && usableSize.height > 0.dp) {
            "usableSize must be positive"
        }
        require(listOf(insets.left, insets.top, insets.right, insets.bottom).all { it.value.isFinite() && it >= 0.dp }) {
            "insets must be finite and non-negative"
        }

        val contentSize = DpSize(
            width = usableSize.width - insets.left - insets.right,
            height = usableSize.height - insets.top - insets.bottom,
        )
        require(contentSize.width > 0.dp && contentSize.height > 0.dp) {
            "insets must leave a positive content size"
        }

        val profile = when (hint) {
            CarLayoutProfileHint.Automatic -> strategy.select(usableSize, insets)
            CarLayoutProfileHint.Expanded -> CarLayoutProfile.Expanded
            CarLayoutProfileHint.VehiclePanel -> CarLayoutProfile.VehiclePanel
        }
        if (profile == CarLayoutProfile.VehiclePanel) {
            return CarLayoutMetrics.deferred(profile, usableSize, insets, contentSize)
        }

        // Unitless proportions are traced to the Expanded mapping's named design relationships.
        val width = contentSize.width
        val height = contentSize.height
        val minAxis = minOf(width, height)
        return CarLayoutMetrics(
            profile = CarLayoutProfile.Expanded,
            usableSize = usableSize,
            contentSize = contentSize,
            insets = insets,
            metricsAvailable = true,
            shellHorizontalPadding = width * ExpandedDesignRatios.shellHorizontalPadding,
            shellVerticalPadding = height * ExpandedDesignRatios.shellVerticalPadding,
            shellWidth = width * ExpandedDesignRatios.shellWidth,
            headerStart = width * ExpandedDesignRatios.headerStart,
            headerTop = height * ExpandedDesignRatios.headerTop,
            headerHeight = height * ExpandedDesignRatios.headerHeight,
            navigationRailStart = width * ExpandedDesignRatios.navigationRailStart,
            navigationRailTop = height * ExpandedDesignRatios.navigationRailTop,
            navigationRailBottom = height * ExpandedDesignRatios.navigationRailBottom,
            navigationRailWidth = width * ExpandedDesignRatios.navigationRail,
            navigationRailInnerPadding = width * ExpandedDesignRatios.navigationRailInnerPadding,
            navigationPrimaryTop = height * ExpandedDesignRatios.navigationPrimaryTop,
            navigationItemInterval = height * ExpandedDesignRatios.navigationItemInterval,
            navigationLibraryLabelTop = height * ExpandedDesignRatios.navigationLibraryLabelTop,
            navigationLibraryTop = height * ExpandedDesignRatios.navigationLibraryTop,
            miniPlayerBottom = height * ExpandedDesignRatios.miniPlayerBottom,
            contentPaneGap = width * ExpandedDesignRatios.railToPaneGap,
            contentHorizontalPadding = width * ExpandedDesignRatios.contentHorizontalPadding,
            contentTop = height * ExpandedDesignRatios.contentTop,
            libraryGap = height * ExpandedDesignRatios.libraryGap,
            navigationItemHeight = height * ExpandedDesignRatios.navigationItem,
            miniPlayerHeight = height * ExpandedDesignRatios.miniPlayer,
            iconSize = maxOf(CarTouchTargets.Minimum, height * ExpandedDesignRatios.iconSize),
            albumCardHeight = height * ExpandedDesignRatios.albumCardHeight,
            compactCardHeight = height * ExpandedDesignRatios.compactCardHeight,
            quickActionHeight = height * ExpandedDesignRatios.quickActionHeight,
            recommendationCardWidth = width * ExpandedDesignRatios.recommendationCardWidth,
            recommendationCardHeight = height * ExpandedDesignRatios.recommendationCardHeight,
            artistCardHeight = height * ExpandedDesignRatios.artistCardHeight,
            cardGap = width * ExpandedDesignRatios.cardGap,
            nowPlayingHorizontalMargin = width * ExpandedDesignRatios.nowPlayingHorizontalMargin,
            nowPlayingVerticalMargin = height * ExpandedDesignRatios.nowPlayingVerticalMargin,
            nowPlayingPlayerPaneWidth = width * ExpandedDesignRatios.nowPlayingPlayerPaneWidth,
            nowPlayingPaneGap = width * ExpandedDesignRatios.nowPlayingPaneGap,
            nowPlayingArtworkSize = minAxis * ExpandedDesignRatios.nowPlayingArtworkSize,
            nowPlayingInnerPadding = width * ExpandedDesignRatios.nowPlayingInnerPadding,
            progressTrackHeight = maxOf(4.dp, height * ExpandedDesignRatios.progressTrackHeight),
            detailContentMargin = width * ExpandedDesignRatios.detailContentMargin,
            detailTopBarHeight = height * ExpandedDesignRatios.detailTopBarHeight,
            detailPaneTop = height * ExpandedDesignRatios.detailPaneTop,
            detailHeroWidth = width * ExpandedDesignRatios.detailHeroWidth,
            detailPaneGap = width * ExpandedDesignRatios.detailPaneGap,
            detailRowHeight = height * ExpandedDesignRatios.detailRowHeight,
            primaryTouchTarget = maxOf(
                CarTouchTargets.Minimum,
                minAxis * ExpandedDesignRatios.primaryTouchTarget,
            ),
        )
    }
}

private object ExpandedDesignRatios {
    // Named relationships from the Expanded mapping. These are ratios, never dp or
    // exact display thresholds; the resolver applies them to measured content bounds.
    const val shellHorizontalPadding: Float = ExpandedReference.shellPadding / ExpandedReference.contentWidth
    const val shellVerticalPadding: Float = ExpandedReference.shellPadding / ExpandedReference.contentHeight
    const val shellWidth: Float = ExpandedReference.shellWidth / ExpandedReference.contentWidth
    const val headerStart: Float = ExpandedReference.headerStart / ExpandedReference.contentWidth
    const val headerTop: Float = ExpandedReference.headerTop / ExpandedReference.contentHeight
    const val headerHeight: Float = ExpandedReference.header / ExpandedReference.contentHeight
    const val navigationRailStart: Float = ExpandedReference.navigationRailStart / ExpandedReference.contentWidth
    const val navigationRailTop: Float = ExpandedReference.navigationRailTop / ExpandedReference.contentHeight
    const val navigationRailBottom: Float = ExpandedReference.navigationRailBottom / ExpandedReference.contentHeight
    const val navigationRail: Float = ExpandedReference.navigationRail / ExpandedReference.contentWidth
    const val navigationRailInnerPadding: Float = ExpandedReference.navigationRailInnerPadding / ExpandedReference.contentWidth
    const val navigationPrimaryTop: Float = ExpandedReference.navigationPrimaryTop / ExpandedReference.contentHeight
    const val navigationItemInterval: Float = ExpandedReference.navigationItemInterval / ExpandedReference.contentHeight
    const val navigationLibraryLabelTop: Float = ExpandedReference.navigationLibraryLabelTop / ExpandedReference.contentHeight
    const val navigationLibraryTop: Float = ExpandedReference.navigationLibraryTop / ExpandedReference.contentHeight
    const val miniPlayerBottom: Float = ExpandedReference.miniPlayerBottom / ExpandedReference.contentHeight
    const val railToPaneGap: Float = ExpandedReference.railToPaneGap / ExpandedReference.contentWidth
    const val contentHorizontalPadding: Float = ExpandedReference.contentHorizontalPadding / ExpandedReference.contentWidth
    const val contentTop: Float = ExpandedReference.contentTop / ExpandedReference.contentHeight
    const val libraryGap: Float = ExpandedReference.libraryGap / ExpandedReference.contentHeight
    const val navigationItem: Float = ExpandedReference.navigationItem / ExpandedReference.contentHeight
    const val miniPlayer: Float = ExpandedReference.miniPlayer / ExpandedReference.contentHeight
    const val iconSize: Float = ExpandedReference.iconSize / ExpandedReference.contentHeight
    const val albumCardHeight: Float = ExpandedReference.albumCardHeight / ExpandedReference.contentHeight
    const val compactCardHeight: Float = ExpandedReference.compactCardHeight / ExpandedReference.contentHeight
    const val quickActionHeight: Float = ExpandedReference.quickActionHeight / ExpandedReference.contentHeight
    const val recommendationCardWidth: Float = ExpandedReference.recommendationCardWidth / ExpandedReference.contentWidth
    const val recommendationCardHeight: Float = ExpandedReference.recommendationCardHeight / ExpandedReference.contentHeight
    const val artistCardHeight: Float = ExpandedReference.artistCardHeight / ExpandedReference.contentHeight
    const val cardGap: Float = ExpandedReference.cardGap / ExpandedReference.contentWidth
    const val nowPlayingHorizontalMargin: Float = ExpandedReference.nowPlayingHorizontalMargin / ExpandedReference.contentWidth
    const val nowPlayingVerticalMargin: Float = ExpandedReference.nowPlayingVerticalMargin / ExpandedReference.contentHeight
    const val nowPlayingPlayerPaneWidth: Float = ExpandedReference.nowPlayingPlayerPaneWidth / ExpandedReference.contentWidth
    const val nowPlayingPaneGap: Float = ExpandedReference.nowPlayingPaneGap / ExpandedReference.contentWidth
    const val nowPlayingArtworkSize: Float = ExpandedReference.nowPlayingArtworkSize / ExpandedReference.contentHeight
    const val nowPlayingInnerPadding: Float = ExpandedReference.nowPlayingInnerPadding / ExpandedReference.contentWidth
    const val progressTrackHeight: Float = ExpandedReference.progressTrackHeight / ExpandedReference.contentHeight
    const val detailContentMargin: Float = ExpandedReference.detailContentMargin / ExpandedReference.contentWidth
    const val detailTopBarHeight: Float = ExpandedReference.detailTopBarHeight / ExpandedReference.contentHeight
    const val detailPaneTop: Float = ExpandedReference.detailPaneTop / ExpandedReference.contentHeight
    const val detailHeroWidth: Float = ExpandedReference.detailHeroWidth / ExpandedReference.contentWidth
    const val detailPaneGap: Float = ExpandedReference.detailPaneGap / ExpandedReference.contentWidth
    const val detailRowHeight: Float = ExpandedReference.detailRowHeight / ExpandedReference.contentHeight
    const val primaryTouchTarget: Float = ExpandedReference.primaryTouchTarget / ExpandedReference.contentHeight
}

private object ExpandedReference {
    // Reference design relationships from FIGMA_EXPANDED_MAPPING and the car spacing contract.
    const val contentWidth: Float = 2496f
    const val contentHeight: Float = 1080f
    const val shellPadding: Float = 24f
    const val shellWidth: Float = 416f
    const val headerStart: Float = 24f
    const val headerTop: Float = 24f
    const val header: Float = 72f
    const val navigationRailStart: Float = 40f
    const val navigationRailTop: Float = 112f
    const val navigationRailBottom: Float = 48f
    const val navigationRail: Float = 352f
    const val navigationRailInnerPadding: Float = 16f
    const val navigationPrimaryTop: Float = 48f
    const val navigationItemInterval: Float = 100f
    const val navigationLibraryLabelTop: Float = 356f
    const val navigationLibraryTop: Float = 390f
    const val miniPlayerBottom: Float = 24f
    const val railToPaneGap: Float = 24f
    const val contentHorizontalPadding: Float = 24f
    const val contentTop: Float = 112f
    const val libraryGap: Float = 24f
    const val navigationItem: Float = 84f
    const val miniPlayer: Float = 164f
    const val iconSize: Float = 56f
    const val albumCardHeight: Float = 248f
    const val compactCardHeight: Float = 112f
    const val quickActionHeight: Float = 160f
    const val recommendationCardWidth: Float = 332f
    const val recommendationCardHeight: Float = 400f
    const val artistCardHeight: Float = 336f
    const val cardGap: Float = 24f
    const val nowPlayingHorizontalMargin: Float = 56f
    const val nowPlayingVerticalMargin: Float = 48f
    const val nowPlayingPlayerPaneWidth: Float = 1000f
    const val nowPlayingPaneGap: Float = 56f
    const val nowPlayingArtworkSize: Float = 640f
    const val nowPlayingInnerPadding: Float = 48f
    const val progressTrackHeight: Float = 14f
    const val detailContentMargin: Float = 16f
    const val detailTopBarHeight: Float = 72f
    const val detailPaneTop: Float = 96f
    const val detailHeroWidth: Float = 560f
    const val detailPaneGap: Float = 40f
    const val detailRowHeight: Float = 76f
    const val primaryTouchTarget: Float = 128f
}

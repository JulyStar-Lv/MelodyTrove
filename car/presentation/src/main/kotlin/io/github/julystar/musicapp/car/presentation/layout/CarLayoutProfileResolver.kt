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
            headerHeight = height * ExpandedDesignRatios.headerHeight,
            navigationRailWidth = width * ExpandedDesignRatios.navigationRail,
            contentPaneGap = width * ExpandedDesignRatios.railToPaneGap,
            libraryGap = height * ExpandedDesignRatios.libraryGap,
            navigationItemHeight = height * ExpandedDesignRatios.navigationItem,
            miniPlayerHeight = height * ExpandedDesignRatios.miniPlayer,
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
    const val headerHeight: Float = ExpandedReference.header / ExpandedReference.contentHeight
    const val navigationRail: Float = ExpandedReference.navigationRail / ExpandedReference.contentWidth
    const val railToPaneGap: Float = ExpandedReference.railToPaneGap / ExpandedReference.contentWidth
    const val libraryGap: Float = ExpandedReference.libraryGap / ExpandedReference.contentHeight
    const val navigationItem: Float = ExpandedReference.navigationItem / ExpandedReference.contentHeight
    const val miniPlayer: Float = ExpandedReference.miniPlayer / ExpandedReference.contentHeight
    const val primaryTouchTarget: Float = ExpandedReference.primaryTouchTarget / ExpandedReference.contentHeight
}

private object ExpandedReference {
    // Reference design relationships from FIGMA_EXPANDED_MAPPING and the car spacing contract.
    const val contentWidth: Float = 2496f
    const val contentHeight: Float = 1080f
    const val shellPadding: Float = 24f
    const val header: Float = 64f
    const val navigationRail: Float = 352f
    const val railToPaneGap: Float = 40f
    const val libraryGap: Float = 24f
    const val navigationItem: Float = 84f
    const val miniPlayer: Float = 164f
    const val primaryTouchTarget: Float = 128f
}

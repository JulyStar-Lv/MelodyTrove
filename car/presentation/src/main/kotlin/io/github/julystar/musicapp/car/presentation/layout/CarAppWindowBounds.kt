package io.github.julystar.musicapp.car.presentation.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize

/** Pixel bounds of the visible app panel inside the activity's drawing surface. */
@Immutable
data class CarAppWindowBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val embeddedInCockpit: Boolean,
    val profileHint: CarLayoutProfileHint,
)

/**
 * Reproduces the panel geometry used by the reference FileManager application.
 *
 * On the target cockpit, the activity draws over a 2560x1440 driver canvas (or the
 * left half of the 5120x1440 display). The visible app panel occupies 2496x1080 at
 * (64, 192), then moves its left edge to x=832 when the vehicle panel is present.
 * Standalone app-sized windows are left untouched.
 */
object CarAppWindowBoundsResolver {
    private const val ReferenceHeightPx = 1440
    private const val ReferenceDriverWidthPx = 2560
    private const val HeightTolerancePx = 72

    fun resolve(
        hostSizePx: IntSize,
        requestedHint: CarLayoutProfileHint,
    ): CarAppWindowBounds {
        require(hostSizePx.width > 0 && hostSizePx.height > 0)

        if (!isReferenceCockpitHost(hostSizePx)) {
            return CarAppWindowBounds(
                left = 0,
                top = 0,
                width = hostSizePx.width,
                height = hostSizePx.height,
                embeddedInCockpit = false,
                profileHint = CarLayoutProfileHint.Automatic,
            )
        }

        val profileHint = when (requestedHint) {
            CarLayoutProfileHint.VehiclePanel -> CarLayoutProfileHint.VehiclePanel
            else -> CarLayoutProfileHint.Expanded
        }
        val left = when (profileHint) {
            CarLayoutProfileHint.VehiclePanel -> 832
            else -> 64
        }
        return CarAppWindowBounds(
            left = left,
            top = 192,
            width = ReferenceDriverWidthPx - left,
            height = 1080,
            embeddedInCockpit = true,
            profileHint = profileHint,
        )
    }

    private fun isReferenceCockpitHost(hostSizePx: IntSize): Boolean {
        val hasReferenceHeight = kotlin.math.abs(hostSizePx.height - ReferenceHeightPx) <= HeightTolerancePx
        if (!hasReferenceHeight || hostSizePx.width < ReferenceDriverWidthPx) return false

        val aspectRatio = hostSizePx.width.toFloat() / hostSizePx.height
        val isDriverCanvas = aspectRatio in 1.70f..1.85f
        val isFullCockpitCanvas = aspectRatio >= 3.40f
        return isDriverCanvas || isFullCockpitCanvas
    }
}

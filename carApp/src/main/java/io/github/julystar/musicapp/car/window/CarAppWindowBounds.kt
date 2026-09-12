package io.github.julystar.musicapp.car.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint

/** Pixel bounds of the visible app panel inside the Automotive activity drawing surface. */
@Immutable
data class CarAppWindowBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val embeddedInCockpit: Boolean,
    val profileHint: CarLayoutProfileHint,
)

@Immutable
data class CarWindowInsetsPx(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    init {
        require(left >= 0 && top >= 0 && right >= 0 && bottom >= 0)
    }
}

/** Resolves OEM host pixels into the presentation-level layout hint and app panel bounds. */
object CarAppWindowBoundsResolver {
    private const val ReferenceHeightPx = 1440
    private const val ReferenceDriverWidthPx = 2560
    private const val HeightTolerancePx = 72
    private const val FullscreenContentHeightPx = 1304

    fun resolve(
        hostSizePx: IntSize,
        requestedHint: CarLayoutProfileHint,
        safeDrawingInsetsPx: CarWindowInsetsPx = CarWindowInsetsPx(),
    ): CarAppWindowBounds {
        require(hostSizePx.width > 0 && hostSizePx.height > 0)

        if (!isReferenceCockpitHost(hostSizePx)) {
            val left = safeDrawingInsetsPx.left.coerceAtMost(hostSizePx.width - 1)
            val top = safeDrawingInsetsPx.top.coerceAtMost(hostSizePx.height - 1)
            val right = safeDrawingInsetsPx.right.coerceAtMost(hostSizePx.width - left - 1)
            val bottom = safeDrawingInsetsPx.bottom.coerceAtMost(hostSizePx.height - top - 1)
            return CarAppWindowBounds(
                left = left,
                top = top,
                width = hostSizePx.width - left - right,
                height = hostSizePx.height - top - bottom,
                embeddedInCockpit = false,
                profileHint = when (requestedHint) {
                    CarLayoutProfileHint.FullscreenCockpit -> CarLayoutProfileHint.FullscreenCockpit
                    else -> CarLayoutProfileHint.Automatic
                },
            )
        }

        if (requestedHint == CarLayoutProfileHint.FullscreenCockpit) {
            return CarAppWindowBounds(
                left = 0,
                top = 0,
                width = hostSizePx.width,
                height = minOf(hostSizePx.height, FullscreenContentHeightPx),
                embeddedInCockpit = true,
                profileHint = CarLayoutProfileHint.FullscreenCockpit,
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

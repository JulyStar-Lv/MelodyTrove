package io.github.julystar.musicapp.car.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics

val LocalCarColors = staticCompositionLocalOf { DarkCarColors }
val LocalCarTypography = staticCompositionLocalOf { DefaultCarTypography }
val LocalCarShapes = staticCompositionLocalOf { DefaultCarShapes }
val LocalCarSpacing = staticCompositionLocalOf { DefaultCarSpacing }
val LocalCarDimensions = staticCompositionLocalOf { CarDimensions() }
val LocalCarTouchTargets = staticCompositionLocalOf { CarTouchTargets() }
val LocalCarFocusVisuals = staticCompositionLocalOf { CarFocusVisuals() }
val LocalCarLayoutMetrics = staticCompositionLocalOf<CarLayoutMetrics?> { null }

@Composable
fun CarTheme(
    darkTheme: Boolean = true,
    layoutMetrics: CarLayoutMetrics? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCarColors provides if (darkTheme) DarkCarColors else LightCarColors,
        LocalCarTypography provides DefaultCarTypography,
        LocalCarShapes provides DefaultCarShapes,
        LocalCarSpacing provides DefaultCarSpacing,
        LocalCarDimensions provides CarDimensions(),
        LocalCarTouchTargets provides CarTouchTargets(),
        LocalCarFocusVisuals provides CarFocusVisuals(),
        LocalCarLayoutMetrics provides layoutMetrics,
        content = content,
    )
}

private val LightCarColors = DarkCarColors.copy(
    backgroundBase = androidx.compose.ui.graphics.Color.White,
    backgroundSubtle = androidx.compose.ui.graphics.Color(0xFFF7F7F7),
    panel = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    surfaceSelected = androidx.compose.ui.graphics.Color(0xFFEDF0F2),
    surfacePressed = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    textPrimary = androidx.compose.ui.graphics.Color.Black,
    textSecondary = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
    textSummary = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
    textActions = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
    textDisabled = androidx.compose.ui.graphics.Color(0xFFB2B2B2),
    accentSubtle = androidx.compose.ui.graphics.Color(0xFFFEE8EA),
    controlTrack = androidx.compose.ui.graphics.Color(0xFFE6E6E6),
    controlTrackDisabled = androidx.compose.ui.graphics.Color(0xFFF0F0F0),
    borderDefault = androidx.compose.ui.graphics.Color(0xFFD9D9D9),
    borderSubtle = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    error = androidx.compose.ui.graphics.Color(0xFFE94634),
)

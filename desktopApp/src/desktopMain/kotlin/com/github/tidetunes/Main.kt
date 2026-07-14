package com.github.tidetunes

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import kotlin.math.roundToInt

private const val MinWindowWidth = 840
private const val MinWindowHeight = 520
private const val MaxWindowWidth = 1200
private const val MaxWindowHeight = 800
private const val WindowWidthRatio = 0.70
private const val WindowHeightRatio = 0.72

fun main() = application {
    val koinApp = initKoin()

    val koin = koinApp.koin
    AppInitializer.initializeBridge(koin)
    AppInitializer.reloadRepositories(koin, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    val initialWindowSize = remember { calculateInitialWindowSize() }
    val windowState = rememberWindowState(size = initialWindowSize)

    Window(
        onCloseRequest = {
            koinApp.close()
            exitApplication()
        },
        title = "TideTunes",
        state = windowState,
    ) {
        DisposableEffect(window) {
            val availableSize = calculateAvailableScreenSize(window.graphicsConfiguration)
            window.minimumSize = Dimension(
                minOf(MinWindowWidth, availableSize.width),
                minOf(MinWindowHeight, availableSize.height),
            )
            onDispose {}
        }
        Root()
    }
}

private fun calculateInitialWindowSize(): DpSize {
    val configuration = GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
    val availableSize = calculateAvailableScreenSize(configuration)

    return DpSize(
        calculateWindowDimension(
            available = availableSize.width,
            ratio = WindowWidthRatio,
            minimum = MinWindowWidth,
            maximum = MaxWindowWidth,
        ).dp,
        calculateWindowDimension(
            available = availableSize.height,
            ratio = WindowHeightRatio,
            minimum = MinWindowHeight,
            maximum = MaxWindowHeight,
        ).dp,
    )
}

private fun calculateAvailableScreenSize(configuration: GraphicsConfiguration): Dimension {
    val bounds = configuration.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
    return Dimension(
        (bounds.width - insets.left - insets.right).coerceAtLeast(1),
        (bounds.height - insets.top - insets.bottom).coerceAtLeast(1),
    )
}

private fun calculateWindowDimension(
    available: Int,
    ratio: Double,
    minimum: Int,
    maximum: Int,
): Int {
    return (available * ratio)
        .roundToInt()
        .coerceIn(minOf(minimum, available), minOf(maximum, available))
}

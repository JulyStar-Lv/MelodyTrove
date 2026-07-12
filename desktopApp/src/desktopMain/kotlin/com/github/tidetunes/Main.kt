package com.github.tidetunes

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
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

private const val MinWindowWidth = 960
private const val MinWindowHeight = 600
private const val DefaultWindowWidth = 960
private const val DefaultWindowHeight = 600

fun main() = application {
    val koinApp = initKoin()

    val koin = koinApp.koin
    AppInitializer.initializeBridge(koin)
    AppInitializer.reloadRepositories(koin, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    val windowState = rememberWindowState(
        width = DefaultWindowWidth.dp,
        height = DefaultWindowHeight.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "TideTunes",
        state = windowState,
    ) {
        val density = LocalDensity.current

        DisposableEffect(window, density) {
            val minimumSize = with(density) {
                Dimension(MinWindowWidth.dp.roundToPx(), MinWindowHeight.dp.roundToPx())
            }
            window.minimumSize = minimumSize
            onDispose {}
        }

        LaunchedEffect(Unit) {
            windowState.size = DpSize(
                maxOf(windowState.size.width, MinWindowWidth.dp),
                maxOf(windowState.size.height, MinWindowHeight.dp),
            )
        }
        Root()
    }
}

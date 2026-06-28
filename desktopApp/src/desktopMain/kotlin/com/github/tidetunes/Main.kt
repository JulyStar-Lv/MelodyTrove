package com.github.tidetunes

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val koinApp = initKoin()

    val koin = koinApp.koin
    AppInitializer.initializeBridge(koin)
    AppInitializer.reloadRepositories(koin, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    Window(
        onCloseRequest = ::exitApplication,
        title = "TideTunes",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
    ) {
        Root()
    }
}

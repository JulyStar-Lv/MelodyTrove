package com.github.tidetunes

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.service.playback.domain.PlaybackController
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.math.roundToInt

private const val MinWindowWidth = 840
private const val MinWindowHeight = 520
private const val MaxWindowWidth = 1200
private const val MaxWindowHeight = 800
private const val WindowWidthRatio = 0.70
private const val WindowHeightRatio = 0.72

fun main() {
    FileKit.init(appId = "TideTunes")
    application {
        val koinApp = initKoin()

        val koin = koinApp.koin
        AppInitializer.initializeBridge(koin)
        AppInitializer.reloadRepositories(koin, CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val playbackController = remember(koin) { koin.get<PlaybackController>() }
        val settingsRepository = remember(koin) { koin.get<SettingsRepository>() }
        val appSettings by settingsRepository.settings.collectAsState(AppSettings.Default)

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
            DisposableEffect(window, appSettings.playerInteraction.desktopShortcutsEnabled) {
                if (appSettings.playerInteraction.desktopShortcutsEnabled) {
                    installPlaybackShortcuts(window.rootPane, playbackController)
                }
                onDispose { removePlaybackShortcuts(window.rootPane) }
            }
            Root()
        }
    }
}

private val playbackShortcutBindings = listOf(
    KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK) to "tidetunes.playPause",
    KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK) to "tidetunes.previous",
    KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK) to "tidetunes.next",
)

private fun installPlaybackShortcuts(
    rootPane: JComponent,
    playbackController: PlaybackController,
) {
    val inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val actionMap = rootPane.actionMap
    playbackShortcutBindings.forEach { (keyStroke, actionKey) -> inputMap.put(keyStroke, actionKey) }
    actionMap.put("tidetunes.playPause", object : AbstractAction() {
        override fun actionPerformed(event: ActionEvent?) = playbackController.togglePlayPause()
    })
    actionMap.put("tidetunes.previous", object : AbstractAction() {
        override fun actionPerformed(event: ActionEvent?) = playbackController.skipPrevious()
    })
    actionMap.put("tidetunes.next", object : AbstractAction() {
        override fun actionPerformed(event: ActionEvent?) = playbackController.skipNext()
    })
}

private fun removePlaybackShortcuts(rootPane: JComponent) {
    val inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val actionMap = rootPane.actionMap
    playbackShortcutBindings.forEach { (keyStroke, actionKey) ->
        inputMap.remove(keyStroke)
        actionMap.remove(actionKey)
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

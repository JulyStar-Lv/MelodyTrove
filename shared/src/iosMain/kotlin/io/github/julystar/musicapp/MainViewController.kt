package io.github.julystar.musicapp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.julystar.musicapp.di.AppInitializer
import io.github.julystar.musicapp.di.initKoin
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrap
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrapState
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.recordKotlinUncaughtException
import io.github.julystar.musicapp.core.domain.recovery.StartupMode
import io.github.julystar.musicapp.core.domain.recovery.StartupPlan
import io.github.julystar.musicapp.core.domain.recovery.allowsNormalApplicationInitialization
import io.github.julystar.musicapp.core.data.StorageRepositoryImpl
import io.github.julystar.musicapp.core.presentation.platform.dispatchPlatformBack
import io.github.julystar.musicapp.service.download.data.scheduler.IosUrlSessionDownloadScheduler
import io.github.julystar.musicapp.service.download.domain.DownloadTaskScheduler
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.RepeatMode
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import platform.UIKit.UIViewController
import kotlin.native.getUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlinx.coroutines.runBlocking

private var applicationInitialized = false
private var applicationKoin: Koin? = null
private var diagnosticsInitialized = false
private lateinit var initialDiagnosticsState: DiagnosticsBootstrapState
private var initialRecoveryIncidentIds: List<String> = emptyList()

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
private fun initializeDiagnostics() {
    if (diagnosticsInitialized) return
    DiagnosticsBootstrap.initialize()
    val previous = getUnhandledExceptionHook()
    setUnhandledExceptionHook { throwable ->
        recordKotlinUncaughtException("kotlin-native", throwable)
        previous?.invoke(throwable)
    }
    initialDiagnosticsState = DiagnosticsBootstrap.finishPlatformExitCollection()
    initialRecoveryIncidentIds = initialDiagnosticsState.beginAutomaticDegradedRecovery()
    diagnosticsInitialized = true
}

private fun initializeApplication(disabledComponents: Set<String> = emptySet()) {
    if (applicationInitialized) return

    val koin = initKoin().koin
    try {
        AppInitializer.initializeBridge(koin, disabledComponents)
        runBlocking {
            AppInitializer.reloadRepositories(koin, disabledComponents)
        }
        applicationKoin = koin
        applicationInitialized = true
    } catch (error: Throwable) {
        stopKoin()
        throw error
    }
}

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    initializeDiagnostics()
    if (initialDiagnosticsState.startupPlan.allowsNormalApplicationInitialization()) {
        initializeApplication(initialDiagnosticsState.startupPlan.disabledComponents)
    }
    return ComposeUIViewController {
        var diagnosticsState by remember {
            mutableStateOf<DiagnosticsBootstrapState>(initialDiagnosticsState)
        }
        var recoveryIncidentIds by remember {
            mutableStateOf(initialRecoveryIncidentIds)
        }
        Root(
            diagnosticsState = diagnosticsState,
            onTryNormalStartup = { disabledComponents ->
                val incidentIds = diagnosticsState.recoveryIncidentIds()
                runCatching {
                    RustDiagnosticsRepository.beginRecovery(disabledComponents)
                    incidentIds.forEach { incidentId ->
                        RustDiagnosticsRepository.markRecoveryAttempted(
                            incidentId,
                            disabledComponents,
                        )
                    }
                    recoveryIncidentIds = incidentIds
                    initializeApplication(disabledComponents)
                    diagnosticsState = diagnosticsState.copy(
                        snapshot = RustDiagnosticsRepository.snapshot(),
                        startupPlan = StartupPlan(StartupMode.NormalStartup),
                    )
                }
            },
            onStartupStable = {
                if (recoveryIncidentIds.isNotEmpty()) {
                    RustDiagnosticsRepository.completeRecovery(recoveryIncidentIds)
                    io.github.julystar.musicapp.diagnostics.SafeModeRecoveryStore.clear()
                    recoveryIncidentIds = emptyList()
                    initialRecoveryIncidentIds = emptyList()
                }
            },
        )
    }
}

fun handleOneDriveOAuthRedirect(code: String, state: String) {
    initializeDiagnostics()
    if (initialDiagnosticsState.safeMode && !applicationInitialized) return
    initializeApplication(initialDiagnosticsState.startupPlan.disabledComponents)
    applicationKoin
        ?.get<StorageRepositoryImpl>()
        ?.receiveOneDriveOAuthRedirect(code, state)
}

fun handleEventsForBackgroundURLSession(
    identifier: String,
    completionHandler: () -> Unit,
) {
    initializeDiagnostics()
    if (initialDiagnosticsState.safeMode && !applicationInitialized) {
        completionHandler()
        return
    }
    initializeApplication(initialDiagnosticsState.startupPlan.disabledComponents)
    val scheduler = applicationKoin
        ?.get<DownloadTaskScheduler>() as? IosUrlSessionDownloadScheduler
    scheduler?.setBackgroundCompletionHandler(identifier, completionHandler)
        ?: completionHandler()
}

fun handlePlaybackPlayCommand(): Boolean = withPlaybackController { controller ->
    controller.play()
}

fun handlePlaybackPauseCommand(): Boolean = withPlaybackController { controller ->
    controller.pause()
}

// Keep STOP resumable across lock-screen, headset, Bluetooth, and CarPlay controls.
// It pauses without clearing the current track, queue, or position.
fun handlePlaybackStopCommand(): Boolean = withPlaybackController { controller ->
    controller.pause()
}

fun handlePlaybackToggleCommand(): Boolean = withPlaybackController { controller ->
    controller.togglePlayPause()
}

fun handlePlaybackNextCommand(): Boolean = withPlaybackController { controller ->
    controller.skipNext()
}

fun handlePlaybackPreviousCommand(): Boolean = withPlaybackController { controller ->
    controller.skipPrevious()
}

fun handlePlaybackSeekByCommand(deltaMs: Long): Boolean = withPlaybackController { controller ->
    val position = controller.position.value
    val maximum = position.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
    controller.seekTo((position.positionMs + deltaMs).coerceIn(0L, maximum))
}

fun handlePlaybackSeekToCommand(positionMs: Long): Boolean = withPlaybackController { controller ->
    val maximum = controller.position.value.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
    controller.seekTo(positionMs.coerceIn(0L, maximum))
}

fun handlePlaybackRestartCommand(): Boolean = withPlaybackController { controller ->
    controller.seekTo(0L)
}

fun handlePlaybackToggleShuffleCommand(): Boolean = withPlaybackController { controller ->
    controller.setShuffle(!controller.state.value.shuffleEnabled)
}

fun handlePlaybackCycleRepeatCommand(): Boolean = withPlaybackController { controller ->
    val nextMode = when (controller.state.value.repeatMode) {
        RepeatMode.Off -> RepeatMode.All
        RepeatMode.All -> RepeatMode.One
        RepeatMode.One -> RepeatMode.Off
    }
    controller.setRepeatMode(nextMode)
}

fun handlePlaybackBackCommand(): Boolean = dispatchPlatformBack()

private inline fun withPlaybackController(action: (PlaybackController) -> Unit): Boolean {
    val controller = playbackControllerOrNull() ?: return false
    action(controller)
    return true
}

private fun playbackControllerOrNull(): PlaybackController? {
    initializeDiagnostics()
    if (!applicationInitialized) {
        if (initialDiagnosticsState.safeMode) return null
        initializeApplication(initialDiagnosticsState.startupPlan.disabledComponents)
    }
    return applicationKoin?.get<PlaybackController>()
}

fun shutdownApplication() {
    if (applicationInitialized) {
        stopKoin()
        applicationKoin = null
        applicationInitialized = false
    }
    if (diagnosticsInitialized) {
        runCatching { RustDiagnosticsRepository.shutdown() }
        diagnosticsInitialized = false
        initialRecoveryIncidentIds = emptyList()
    }
}

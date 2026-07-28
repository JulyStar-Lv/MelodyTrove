package com.github.tidetunes

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import com.github.tidetunes.diagnostics.DiagnosticsBootstrap
import com.github.tidetunes.diagnostics.DiagnosticsBootstrapState
import com.github.tidetunes.diagnostics.RustDiagnosticsRepository
import com.github.tidetunes.diagnostics.recordKotlinUncaughtException
import com.github.tidetunes.core.domain.recovery.StartupMode
import com.github.tidetunes.core.domain.recovery.StartupPlan
import com.github.tidetunes.core.domain.recovery.allowsNormalApplicationInitialization
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.service.download.data.scheduler.IosUrlSessionDownloadScheduler
import com.github.tidetunes.service.download.domain.DownloadTaskScheduler
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
                    com.github.tidetunes.diagnostics.SafeModeRecoveryStore.clear()
                    recoveryIncidentIds = emptyList()
                    initialRecoveryIncidentIds = emptyList()
                }
            },
        )
    }
}

fun handleOneDriveOAuthRedirect(code: String, state: String) {
    initializeDiagnostics()
    if (initialDiagnosticsState.safeMode) return
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
    if (initialDiagnosticsState.safeMode) {
        completionHandler()
        return
    }
    initializeApplication(initialDiagnosticsState.startupPlan.disabledComponents)
    val scheduler = applicationKoin
        ?.get<DownloadTaskScheduler>() as? IosUrlSessionDownloadScheduler
    scheduler?.setBackgroundCompletionHandler(identifier, completionHandler)
        ?: completionHandler()
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

package io.github.julystar.musicapp.car

import android.app.Application
import io.github.julystar.musicapp.car.di.carPlatformModule
import io.github.julystar.musicapp.car.presentation.di.carPresentationModule
import io.github.julystar.musicapp.core.domain.model.DiagnosticStartupStage
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.SafeModeRecoveryStore
import io.github.julystar.musicapp.runtime.AndroidRuntimeBootstrap
import io.github.julystar.musicapp.runtime.AndroidRuntimePreparation
import io.github.julystar.musicapp.runtime.AndroidRuntimeSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CarStartupState {
    data object Initializing : CarStartupState
    data object Ready : CarStartupState
    data object RecoveryRequired : CarStartupState
    data class Failed(val cause: Throwable) : CarStartupState
}

class CarApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableStartupState = MutableStateFlow<CarStartupState>(CarStartupState.Initializing)
    private val mutableExitPlaybackRequest = MutableStateFlow(0L)
    private var startupJob: Job? = null
    private var runtimePreparation: AndroidRuntimePreparation? = null
    private var runtimeSession: AndroidRuntimeSession? = null
    private var recoveryIncidentIds: List<String> = emptyList()

    val startupState: StateFlow<CarStartupState> = mutableStartupState.asStateFlow()
    val exitPlaybackRequest: StateFlow<Long> = mutableExitPlaybackRequest.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initializeApplication()
    }

    fun initializeApplication() {
        if (startupJob?.isActive == true || mutableStartupState.value == CarStartupState.Ready) return
        mutableStartupState.value = CarStartupState.Initializing
        try {
            val preparation = AndroidRuntimeBootstrap.prepare(this).also {
                runtimePreparation = it
            }
            if (!preparation.allowsInitialization) {
                mutableStartupState.value = CarStartupState.RecoveryRequired
                return
            }
            recoveryIncidentIds = preparation.recoveryIncidentIds
            startRuntime(preparation.diagnosticsState.startupPlan.disabledComponents)
        } catch (error: Throwable) {
            failStartup(error)
        }
    }

    fun retryRecovery() {
        if (startupJob?.isActive == true || runtimeSession != null) return
        val preparation = runtimePreparation ?: return
        val disabledComponents = preparation.diagnosticsState.startupPlan.disabledComponents
        mutableStartupState.value = CarStartupState.Initializing
        try {
            RustDiagnosticsRepository.beginRecovery(disabledComponents)
            recoveryIncidentIds = preparation.diagnosticsState.recoveryIncidentIds()
                .onEach { incidentId ->
                    RustDiagnosticsRepository.markRecoveryAttempted(
                        incidentId,
                        disabledComponents,
                    )
                }
            startRuntime(disabledComponents)
        } catch (error: Throwable) {
            failStartup(error)
        }
    }

    private fun startRuntime(disabledComponents: Set<String>) {
        val session = AndroidRuntimeBootstrap.createSession(
            additionalModules = listOf(carPlatformModule, carPresentationModule),
        )
        runtimeSession = session
        startupJob = applicationScope.launch {
            try {
                AndroidRuntimeBootstrap.initializeSession(session, disabledComponents)
                mutableStartupState.value = CarStartupState.Ready
                delay(STARTUP_STABLE_DELAY_MILLIS)
                RustDiagnosticsRepository.updateStartupStage(DiagnosticStartupStage.StartupStable)
                if (recoveryIncidentIds.isNotEmpty()) {
                    RustDiagnosticsRepository.completeRecovery(recoveryIncidentIds)
                    SafeModeRecoveryStore.clear()
                    recoveryIncidentIds = emptyList()
                }
            } catch (error: CancellationException) {
                session.close()
                if (runtimeSession === session) runtimeSession = null
                throw error
            } catch (error: Throwable) {
                failStartup(error)
            }
        }
    }

    private fun failStartup(error: Throwable) {
        runtimeSession?.close()
        runtimeSession = null
        mutableStartupState.value = CarStartupState.Failed(error)
    }

    fun requestExitPlayback() {
        mutableExitPlaybackRequest.value += 1L
    }

    override fun onTerminate() {
        startupJob?.cancel()
        applicationScope.cancel()
        AndroidRuntimeBootstrap.shutdown(runtimeSession)
        runtimeSession = null
        super.onTerminate()
    }

    private companion object {
        const val STARTUP_STABLE_DELAY_MILLIS = 10_000L
    }
}

package io.github.julystar.musicapp.car

import android.app.Application
import io.github.julystar.musicapp.car.di.carPlatformModule
import io.github.julystar.musicapp.car.presentation.di.carPresentationModule
import io.github.julystar.musicapp.runtime.AndroidRuntimeBootstrap
import io.github.julystar.musicapp.runtime.AndroidRuntimeSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private var runtimeSession: AndroidRuntimeSession? = null

    val startupState: StateFlow<CarStartupState> = mutableStartupState.asStateFlow()
    val exitPlaybackRequest: StateFlow<Long> = mutableExitPlaybackRequest.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initializeApplication()
    }

    fun initializeApplication() {
        if (startupJob?.isActive == true || mutableStartupState.value == CarStartupState.Ready) return
        mutableStartupState.value = CarStartupState.Initializing
        startupJob = applicationScope.launch {
            try {
                val preparation = AndroidRuntimeBootstrap.prepare(this@CarApplication)
                if (!preparation.allowsInitialization) {
                    mutableStartupState.value = CarStartupState.RecoveryRequired
                    return@launch
                }
                runtimeSession = AndroidRuntimeBootstrap.openSession(
                    additionalModules = listOf(carPlatformModule, carPresentationModule),
                    disabledComponents = preparation.diagnosticsState.startupPlan.disabledComponents,
                )
                mutableStartupState.value = CarStartupState.Ready
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                runtimeSession?.close()
                runtimeSession = null
                mutableStartupState.value = CarStartupState.Failed(error)
            }
        }
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
}

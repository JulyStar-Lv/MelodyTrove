package io.github.julystar.musicapp.car

import android.app.Application
import io.github.julystar.musicapp.car.presentation.di.carPresentationModule
import io.github.julystar.musicapp.di.AppInitializer
import io.github.julystar.musicapp.di.initKoin
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrap
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.collectAndroidHistoricalExitInfo
import io.github.julystar.musicapp.diagnostics.lastUserRequestedProcessExitAtEpochMs
import io.github.julystar.musicapp.diagnostics.recordKotlinUncaughtException
import io.github.julystar.musicapp.platform.appContext
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
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

sealed interface CarStartupState {
    data object Initializing : CarStartupState

    data object Ready : CarStartupState

    data class Failed(val cause: Throwable) : CarStartupState
}

class CarApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableStartupState = MutableStateFlow<CarStartupState>(CarStartupState.Initializing)
    private var startupJob: Job? = null
    private var koin: Koin? = null

    val startupState: StateFlow<CarStartupState> = mutableStartupState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        appContext = this
        initializeApplication()
    }

    fun initializeApplication() {
        if (startupJob?.isActive == true || mutableStartupState.value == CarStartupState.Ready) return
        mutableStartupState.value = CarStartupState.Initializing
        startupJob = applicationScope.launch {
            try {
                DiagnosticsBootstrap.initialize(
                    lastUserRequestedExitAtEpochMs = lastUserRequestedProcessExitAtEpochMs(),
                )
                installFatalHandler()
                collectAndroidHistoricalExitInfo()
                val diagnosticsState = DiagnosticsBootstrap.finishPlatformExitCollection()
                val disabledComponents = diagnosticsState.startupPlan.disabledComponents

                val initializedKoin = initKoin(additionalModules = listOf(carPresentationModule)).koin
                koin = initializedKoin
                AppInitializer.initializeBridgeAsync(initializedKoin, disabledComponents)
                AppInitializer.reloadRepositories(initializedKoin, disabledComponents)
                mutableStartupState.value = CarStartupState.Ready
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (koin != null) {
                    runCatching { stopKoin() }
                    koin = null
                }
                mutableStartupState.value = CarStartupState.Failed(error)
            }
        }
    }

    private fun installFatalHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            recordKotlinUncaughtException(thread.name, throwable)
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                exitProcess(1)
            }
        }
    }

    override fun onTerminate() {
        startupJob?.cancel()
        applicationScope.cancel()
        if (koin != null) stopKoin()
        runCatching { RustDiagnosticsRepository.shutdown() }
        super.onTerminate()
    }
}

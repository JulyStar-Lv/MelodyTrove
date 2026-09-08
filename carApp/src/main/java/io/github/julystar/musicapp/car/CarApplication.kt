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
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class CarApplication : Application() {
    lateinit var koin: Koin
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = this
        DiagnosticsBootstrap.initialize(
            lastUserRequestedExitAtEpochMs = lastUserRequestedProcessExitAtEpochMs(),
        )
        installFatalHandler()
        collectAndroidHistoricalExitInfo()
        val startupState = DiagnosticsBootstrap.finishPlatformExitCollection()
        val disabledComponents = startupState.startupPlan.disabledComponents

        koin = initKoin(additionalModules = listOf(carPresentationModule)).koin
        AppInitializer.initializeBridge(koin, disabledComponents)
        runBlocking {
            AppInitializer.reloadRepositories(koin, disabledComponents)
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
        stopKoin()
        runCatching { RustDiagnosticsRepository.shutdown() }
        super.onTerminate()
    }
}

package io.github.julystar.musicapp

import android.app.Application
import io.github.julystar.musicapp.core.domain.recovery.allowsNormalApplicationInitialization
import io.github.julystar.musicapp.di.AppInitializer
import io.github.julystar.musicapp.di.initKoin
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrap
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.collectAndroidHistoricalExitInfo
import io.github.julystar.musicapp.diagnostics.recordKotlinUncaughtException
import io.github.julystar.musicapp.platform.appContext
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class AppApplication : Application() {
    private var koin: Koin? = null
    var repositoriesLoaded: Boolean = false
        private set
    var recoveryIncidentIds: List<String> = emptyList()
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = this
        DiagnosticsBootstrap.initialize()
        installFatalHandler()
        collectAndroidHistoricalExitInfo()
        val diagnosticsState = DiagnosticsBootstrap.finishPlatformExitCollection()
        if (diagnosticsState.startupPlan.allowsNormalApplicationInitialization()) {
            recoveryIncidentIds = diagnosticsState.beginAutomaticDegradedRecovery()
            initializeFullApplication(diagnosticsState.startupPlan.disabledComponents)
        }
    }

    fun initializeFullApplication(disabledComponents: Set<String>): Koin {
        koin?.let { return it }
        val initialized = initKoin().koin
        try {
            AppInitializer.initializeBridge(initialized, disabledComponents)
            runBlocking {
                AppInitializer.reloadRepositories(initialized, disabledComponents)
            }
            repositoriesLoaded = true
        } catch (error: Throwable) {
            stopKoin()
            repositoriesLoaded = false
            recoveryIncidentIds = emptyList()
            throw error
        }
        return initialized.also { koin = it }
    }

    fun clearRecoveryTracking() {
        recoveryIncidentIds = emptyList()
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
        if (koin != null) {
            stopKoin()
            koin = null
            repositoriesLoaded = false
        }
        runCatching { RustDiagnosticsRepository.shutdown() }
        super.onTerminate()
    }
}

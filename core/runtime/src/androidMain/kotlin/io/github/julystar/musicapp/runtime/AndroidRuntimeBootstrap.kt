package io.github.julystar.musicapp.runtime

import android.content.Context
import io.github.julystar.musicapp.core.domain.recovery.allowsNormalApplicationInitialization
import io.github.julystar.musicapp.di.AppInitializer
import io.github.julystar.musicapp.di.initKoin
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrap
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrapState
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.diagnostics.collectAndroidHistoricalExitInfo
import io.github.julystar.musicapp.diagnostics.lastUserRequestedProcessExitAtEpochMs
import io.github.julystar.musicapp.diagnostics.recordKotlinUncaughtException
import io.github.julystar.musicapp.platform.appContext
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import kotlin.system.exitProcess

data class AndroidRuntimePreparation(
    val diagnosticsState: DiagnosticsBootstrapState,
    val allowsInitialization: Boolean,
    val recoveryIncidentIds: List<String>,
)

class AndroidRuntimeSession internal constructor(
    val koin: Koin,
) {
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        stopKoin()
        runCatching { RustDiagnosticsRepository.shutdown() }
    }
}

/** Shared Android application-runtime sequence used by the mobile and Automotive hosts. */
object AndroidRuntimeBootstrap {
    private val fatalHandlerLock = Any()

    @Volatile
    private var fatalHandlerInstalled = false

    fun prepare(context: Context): AndroidRuntimePreparation {
        appContext = context.applicationContext
        DiagnosticsBootstrap.initialize(
            lastUserRequestedExitAtEpochMs = lastUserRequestedProcessExitAtEpochMs(),
        )
        installFatalHandler()
        collectAndroidHistoricalExitInfo()
        val diagnosticsState = DiagnosticsBootstrap.finishPlatformExitCollection()
        val allowsInitialization = diagnosticsState.startupPlan.allowsNormalApplicationInitialization()
        return AndroidRuntimePreparation(
            diagnosticsState = diagnosticsState,
            allowsInitialization = allowsInitialization,
            recoveryIncidentIds = if (allowsInitialization) {
                diagnosticsState.beginAutomaticDegradedRecovery()
            } else {
                emptyList()
            },
        )
    }

    suspend fun openSession(
        additionalModules: List<Module>,
        disabledComponents: Set<String>,
    ): AndroidRuntimeSession {
        val initializedKoin = initKoin(additionalModules = additionalModules).koin
        return try {
            AppInitializer.initializeBridgeAsync(initializedKoin, disabledComponents)
            AppInitializer.reloadRepositories(initializedKoin, disabledComponents)
            AndroidRuntimeSession(initializedKoin)
        } catch (error: Throwable) {
            stopKoin()
            throw error
        }
    }

    fun openSessionBlocking(
        additionalModules: List<Module>,
        disabledComponents: Set<String>,
    ): AndroidRuntimeSession = runBlocking {
        openSession(additionalModules, disabledComponents)
    }

    fun shutdown(session: AndroidRuntimeSession?) {
        if (session != null) {
            session.close()
        } else {
            runCatching { RustDiagnosticsRepository.shutdown() }
        }
    }

    private fun installFatalHandler() {
        if (fatalHandlerInstalled) return
        synchronized(fatalHandlerLock) {
            if (fatalHandlerInstalled) return
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                recordKotlinUncaughtException(thread.name, throwable)
                if (previous != null) {
                    previous.uncaughtException(thread, throwable)
                } else {
                    exitProcess(1)
                }
            }
            fatalHandlerInstalled = true
        }
    }
}

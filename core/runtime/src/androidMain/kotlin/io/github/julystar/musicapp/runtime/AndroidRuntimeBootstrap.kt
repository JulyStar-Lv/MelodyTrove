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
    @Volatile
    private var closed = false

    fun close() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        AndroidRuntimeBootstrap.closeSession(this)
    }
}

/** Shared Android application-runtime sequence used by the mobile and Automotive hosts. */
object AndroidRuntimeBootstrap {
    private val fatalHandlerLock = Any()
    private val sessionLock = Any()

    @Volatile
    private var fatalHandlerInstalled = false

    @Volatile
    private var activeSession: AndroidRuntimeSession? = null

    val isDependencyGraphAvailable: Boolean
        get() = activeSession != null

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

    fun createSession(
        additionalModules: List<Module>,
    ): AndroidRuntimeSession {
        synchronized(sessionLock) {
            check(activeSession == null) { "Android runtime session is already open" }
            return AndroidRuntimeSession(
                initKoin(additionalModules = additionalModules).koin,
            ).also { session ->
                activeSession = session
            }
        }
    }

    suspend fun initializeSession(
        session: AndroidRuntimeSession,
        disabledComponents: Set<String>,
    ): AndroidRuntimeSession = try {
        check(activeSession === session) { "Android runtime session is not active" }
        AppInitializer.initializeBridgeAsync(session.koin, disabledComponents)
        AppInitializer.reloadRepositories(session.koin, disabledComponents)
        session
    } catch (error: Throwable) {
        session.close()
        throw error
    }

    suspend fun openSession(
        additionalModules: List<Module>,
        disabledComponents: Set<String>,
    ): AndroidRuntimeSession = initializeSession(
        session = createSession(additionalModules),
        disabledComponents = disabledComponents,
    )

    fun openSessionBlocking(
        additionalModules: List<Module>,
        disabledComponents: Set<String>,
    ): AndroidRuntimeSession = runBlocking {
        openSession(additionalModules, disabledComponents)
    }

    fun shutdown(session: AndroidRuntimeSession?) {
        session?.close()
        runCatching { RustDiagnosticsRepository.shutdown() }
    }

    internal fun closeSession(session: AndroidRuntimeSession) {
        val shouldStopKoin = synchronized(sessionLock) {
            if (activeSession !== session) {
                false
            } else {
                activeSession = null
                true
            }
        }
        if (shouldStopKoin) stopKoin()
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

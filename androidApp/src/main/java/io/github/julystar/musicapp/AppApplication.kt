package io.github.julystar.musicapp

import android.app.Application
import io.github.julystar.musicapp.di.appModule
import io.github.julystar.musicapp.runtime.AndroidRuntimeBootstrap
import io.github.julystar.musicapp.runtime.AndroidRuntimeSession
import org.koin.core.Koin

class AppApplication : Application() {
    private var runtimeSession: AndroidRuntimeSession? = null
    var repositoriesLoaded: Boolean = false
        private set
    var recoveryIncidentIds: List<String> = emptyList()
        private set

    override fun onCreate() {
        super.onCreate()
        val preparation = AndroidRuntimeBootstrap.prepare(this)
        if (preparation.allowsInitialization) {
            recoveryIncidentIds = preparation.recoveryIncidentIds
            initializeFullApplication(preparation.diagnosticsState.startupPlan.disabledComponents)
        }
    }

    fun initializeFullApplication(disabledComponents: Set<String>): Koin {
        runtimeSession?.let { return it.koin }
        try {
            val session = AndroidRuntimeBootstrap.openSessionBlocking(
                additionalModules = listOf(appModule),
                disabledComponents = disabledComponents,
            )
            runtimeSession = session
            repositoriesLoaded = true
            return session.koin
        } catch (error: Throwable) {
            repositoriesLoaded = false
            recoveryIncidentIds = emptyList()
            throw error
        }
    }

    fun clearRecoveryTracking() {
        recoveryIncidentIds = emptyList()
    }

    override fun onTerminate() {
        AndroidRuntimeBootstrap.shutdown(runtimeSession)
        runtimeSession = null
        repositoriesLoaded = false
        super.onTerminate()
    }
}

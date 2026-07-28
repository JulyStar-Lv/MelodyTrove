package com.github.tidetunes

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.common.util.concurrent.MoreExecutors
import com.github.tidetunes.core.PlaybackService
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import com.github.tidetunes.diagnostics.DiagnosticsBootstrap
import com.github.tidetunes.diagnostics.DiagnosticsBootstrapState
import com.github.tidetunes.diagnostics.RustDiagnosticsRepository
import com.github.tidetunes.diagnostics.collectAndroidHistoricalExitInfo
import com.github.tidetunes.diagnostics.recordKotlinUncaughtException
import com.github.tidetunes.core.domain.recovery.StartupMode
import com.github.tidetunes.core.domain.recovery.StartupPlan
import com.github.tidetunes.core.domain.recovery.allowsNormalApplicationInitialization
import com.github.tidetunes.platform.appContext
import com.github.tidetunes.singleton.PermissionRepository
import com.github.tidetunes.singleton.PlayerControllerRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.android.ext.android.inject
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private var isAppReady = false
    private var diagnosticsState by mutableStateOf<DiagnosticsBootstrapState?>(null)
    private var recoveryIncidentIds: List<String> = emptyList()
    private var repositoriesLoaded = false
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private val storageRepository: StorageRepositoryImpl by inject()
    private val permissionRepository: PermissionRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !isAppReady }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        enableEdgeToEdge()

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            _ -> permissionRepository.triggerPermissionChanged()
        }
        diagnosticsState = DiagnosticsBootstrap.state
        val tideTunesApplication = application as TideTunesApplication
        repositoriesLoaded = tideTunesApplication.repositoriesLoaded
        recoveryIncidentIds = tideTunesApplication.recoveryIncidentIds
        if (diagnosticsState?.safeMode == false) {
            permissionRepository.onCreate(this, requestPermissionLauncher)
        }

        setContent {
            Root(
                diagnosticsState = diagnosticsState,
                onReady = { isAppReady = true },
                onStartupStable = {
                    if (recoveryIncidentIds.isNotEmpty()) {
                        RustDiagnosticsRepository.completeRecovery(recoveryIncidentIds)
                        com.github.tidetunes.diagnostics.SafeModeRecoveryStore.clear()
                        recoveryIncidentIds = emptyList()
                        (application as TideTunesApplication).clearRecoveryTracking()
                    }
                },
                onTryNormalStartup = { disabledComponents ->
                    tryNormalStartup(requestPermissionLauncher, disabledComponents)
                },
            )
        }
        if (diagnosticsState?.safeMode == false) {
            handleOAuthRedirect(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (diagnosticsState?.safeMode != false) return
        ensurePostNotificationsPermission()

        lifecycleScope.launch {
            if (!repositoriesLoaded) {
                AppInitializer.reloadRepositories(
                    org.koin.core.context.GlobalContext.get(),
                    diagnosticsState?.startupPlan?.disabledComponents.orEmpty(),
                )
                repositoriesLoaded = true
            }
            setupMediaController()
        }
    }

    private fun setupMediaController() {
        val factory = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        ).buildAsync()
        factory.addListener(
            {
                if (factory.isDone) {
                    val controller = factory.get()
                    playerControllerRepository.setupMediaController(controller)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()
        if (diagnosticsState?.safeMode == false) {
            playerControllerRepository.destroyMediaController()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (diagnosticsState?.safeMode == false) {
            permissionRepository.onDestroy()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent) {
        if (diagnosticsState?.safeMode != false) return
        intent.data?.let { uri ->
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            if (code != null && state != null) {
                storageRepository.receiveOneDriveOAuthRedirect(code, state)
            }
        }
    }

    private fun ensurePostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun tryNormalStartup(
        requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
        disabledComponents: Set<String>,
    ) {
        val safeState = diagnosticsState ?: return
        lifecycleScope.launch {
            val incidentIds = safeState.recoveryIncidentIds()
            runCatching {
                RustDiagnosticsRepository.beginRecovery(disabledComponents)
                incidentIds.forEach { incidentId ->
                    RustDiagnosticsRepository.markRecoveryAttempted(incidentId, disabledComponents)
                }
                recoveryIncidentIds = incidentIds
                val application = application as TideTunesApplication
                application.initializeFullApplication(disabledComponents)
                permissionRepository.onCreate(this@MainActivity, requestPermissionLauncher)
                repositoriesLoaded = application.repositoriesLoaded
                setupMediaController()
                diagnosticsState = safeState.copy(
                    snapshot = RustDiagnosticsRepository.snapshot(),
                    startupPlan = StartupPlan(StartupMode.NormalStartup),
                )
            }
        }
    }
}

class TideTunesApplication : Application() {
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

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
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.github.tidetunes.core.KeepBackendService
import com.github.tidetunes.core.PlaybackService
import com.github.tidetunes.core.data.StorageRepositoryImpl
import com.github.tidetunes.di.AppInitializer
import com.github.tidetunes.di.initKoin
import com.github.tidetunes.platform.appContext
import com.github.tidetunes.singleton.PermissionRepository
import com.github.tidetunes.singleton.PlayerControllerRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.context.stopKoin
import uniffi.tidetunes_backend.tidetunesLog
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private val storageRepository: StorageRepositoryImpl by inject()
    private val permissionRepository: PermissionRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startService(Intent(this, KeepBackendService::class.java))
        AppInitializer.initializeBridge(org.koin.core.context.GlobalContext.get())
        setupExceptionHandler()

        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            _ -> permissionRepository.triggerPermissionChanged()
        }
        permissionRepository.onCreate(this, requestPermissionLauncher)

        setContent {
            Root()
        }
        handleOAuthRedirect(intent)
    }

    override fun onStart() {
        super.onStart()
        ensurePostNotificationsPermission()

        lifecycleScope.launch {
            AppInitializer.reloadRepositories(org.koin.core.context.GlobalContext.get(), this)
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

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            tidetunesLog("on uncaught exception: $throwable")
            tidetunesLog("on uncaught exception stacktrace: ${throwable.stackTraceToString()}")

            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }
    }

    override fun onStop() {
        super.onStop()
        playerControllerRepository.destroyMediaController()
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionRepository.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent) {
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
}

class TideTunesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        initKoin()
    }

    override fun onTerminate() {
        stopKoin()
        super.onTerminate()
    }
}

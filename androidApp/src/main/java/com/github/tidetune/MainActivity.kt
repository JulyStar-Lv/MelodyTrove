package com.github.tidetune

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
import com.github.tidetune.core.KeepBackendService
import com.github.tidetune.core.PlaybackService
import com.github.tidetune.di.appModule
import com.github.tidetune.platform.appContext
import com.github.tidetune.singleton.Bridge
import com.github.tidetune.singleton.PermissionRepository
import com.github.tidetune.singleton.PlayerControllerRepository
import com.github.tidetune.singleton.PlayerRepository
import com.github.tidetune.singleton.PlaylistRepository
import com.github.tidetune.singleton.StorageRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext.startKoin
import uniffi.tidetune_core.tidetuneLog
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val bridge: Bridge by inject()
    private val storageRepository: StorageRepository by inject()
    private val playlistRepository: PlaylistRepository by inject()
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private val playerRepository: PlayerRepository by inject()
    private val permissionRepository: PermissionRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startService(Intent(this, KeepBackendService::class.java))
        bridge.initialize()
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
            playerRepository.reload()
            storageRepository.reload()
            playlistRepository.reload()
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
                factory.let {
                    if (it.isDone) {
                        val controller = it.get()
                        playerControllerRepository.setupMediaController(controller)
                        controller
                    } else {
                        null
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            tidetuneLog("on uncaught exception: $throwable")
            tidetuneLog("on uncaught exception stacktrace: ${throwable.stackTraceToString()}")

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

class TideTuneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        startKoin {
            modules(appModule)
        }
    }
}

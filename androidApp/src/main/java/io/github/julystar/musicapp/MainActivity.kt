package io.github.julystar.musicapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
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
import io.github.julystar.musicapp.core.PlaybackService
import io.github.julystar.musicapp.core.data.StorageRepositoryImpl
import io.github.julystar.musicapp.di.AppInitializer
import io.github.julystar.musicapp.di.initKoin
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrap
import io.github.julystar.musicapp.diagnostics.DiagnosticsBootstrapState
import io.github.julystar.musicapp.diagnostics.RustDiagnosticsRepository
import io.github.julystar.musicapp.core.domain.recovery.StartupMode
import io.github.julystar.musicapp.core.domain.recovery.StartupPlan
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import io.github.julystar.musicapp.service.playback.domain.RepeatMode
import io.github.julystar.musicapp.singleton.PermissionRepository
import io.github.julystar.musicapp.singleton.PlayerControllerRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private var isAppReady = false
    private var diagnosticsState by mutableStateOf<DiagnosticsBootstrapState?>(null)
    private var recoveryIncidentIds: List<String> = emptyList()
    private var repositoriesLoaded = false
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private val playbackController: PlaybackController by inject()
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
        val appApplication = application as AppApplication
        repositoriesLoaded = appApplication.repositoriesLoaded
        recoveryIncidentIds = appApplication.recoveryIncidentIds
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
                        io.github.julystar.musicapp.diagnostics.SafeModeRecoveryStore.clear()
                        recoveryIncidentIds = emptyList()
                        (application as AppApplication).clearRecoveryTracking()
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
        if (mediaControllerAttached || mediaControllerSetupInFlight) return
        mediaControllerSetupInFlight = true
        val factory = MediaController.Builder(
            applicationContext,
            SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java))
        ).buildAsync()
        factory.addListener(
            {
                runCatching {
                    if (!factory.isDone) return@runCatching
                    val controller = factory.get()
                    playerControllerRepository.setupMediaController(controller)
                    mediaControllerAttached = true
                }
                mediaControllerSetupInFlight = false
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()
        // Playback is process-scoped. Keep the MediaController attached while the Activity is
        // backgrounded so PlaybackController commands remain available to the MediaSession service.
    }

    override fun onDestroy() {
        super.onDestroy()
        if (diagnosticsState?.safeMode == false) {
            permissionRepository.onDestroy()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true
        if (
            diagnosticsState?.safeMode != false ||
            event.action != KeyEvent.ACTION_UP ||
            event.repeatCount != 0
        ) {
            return false
        }

        return when {
            event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.matchesModifiers() -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            event.keyCode == KeyEvent.KEYCODE_SPACE && event.matchesModifiers(ctrl = true) -> {
                playbackController.togglePlayPause()
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.matchesModifiers(alt = true) -> {
                playbackController.skipPrevious()
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.matchesModifiers(alt = true) -> {
                playbackController.skipNext()
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.matchesModifiers(ctrl = true) -> {
                seekBy(-SEEK_SHORT_MS)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.matchesModifiers(ctrl = true) -> {
                seekBy(SEEK_SHORT_MS)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                event.matchesModifiers(ctrl = true, shift = true) -> {
                seekBy(-SEEK_LONG_MS)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                event.matchesModifiers(ctrl = true, shift = true) -> {
                seekBy(SEEK_LONG_MS)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_MOVE_HOME && event.matchesModifiers(ctrl = true) -> {
                playbackController.seekTo(0L)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_S && event.matchesModifiers(ctrl = true) -> {
                val state = playbackController.state.value
                playbackController.setShuffle(!state.shuffleEnabled)
                true
            }
            event.keyCode == KeyEvent.KEYCODE_R && event.matchesModifiers(ctrl = true) -> {
                val nextMode = when (playbackController.state.value.repeatMode) {
                    RepeatMode.Off -> RepeatMode.All
                    RepeatMode.All -> RepeatMode.One
                    RepeatMode.One -> RepeatMode.Off
                }
                playbackController.setRepeatMode(nextMode)
                true
            }
            else -> false
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
                val application = application as AppApplication
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

    private fun seekBy(deltaMs: Long) {
        val position = playbackController.position.value
        val maximum = position.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
        playbackController.seekTo((position.positionMs + deltaMs).coerceIn(0L, maximum))
    }

    private fun KeyEvent.matchesModifiers(
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
    ): Boolean {
        return isCtrlPressed == ctrl &&
            isAltPressed == alt &&
            isShiftPressed == shift &&
            !isMetaPressed
    }

    private companion object {
        @Volatile
        var mediaControllerAttached = false

        @Volatile
        var mediaControllerSetupInFlight = false

        const val SEEK_SHORT_MS = 10_000L
        const val SEEK_LONG_MS = 30_000L
    }
}
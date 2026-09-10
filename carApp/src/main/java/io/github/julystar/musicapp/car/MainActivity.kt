package io.github.julystar.musicapp.car

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.julystar.musicapp.car.presentation.CarRoot
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.core.PlaybackService
import io.github.julystar.musicapp.singleton.PlayerControllerRepository
import io.github.julystar.musicapp.singleton.PermissionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val layoutProfileResolver: CarLayoutProfileResolver by inject()
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private val permissionRepository: PermissionRepository by inject()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controllerAttached = false
    private var permissionAttached = false
    private var startupObserver: Job? = null
    private var lastWindowEvidence: String? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRepository.triggerPermissionChanged()
        }
        setContent {
            val startupState by (application as CarApplication).startupState.collectAsState()
            when (val state = startupState) {
                CarStartupState.Initializing -> CarStartupMessage("正在准备音乐库…")
                CarStartupState.Ready -> BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            logWindowEvidence(coordinates.size, coordinates.positionInWindow())
                        },
                ) {
                    val metrics = remember(maxWidth, maxHeight) {
                        layoutProfileResolver.resolve(DpSize(maxWidth, maxHeight))
                    }
                    LaunchedEffect(metrics.profile, metrics.contentSize) {
                        Log.i(
                            WINDOW_EVIDENCE_TAG,
                            "resolvedProfile=${metrics.profile} usableDp=${metrics.usableSize} contentDp=${metrics.contentSize}",
                        )
                    }
                    CarRoot(metrics = metrics, onExit = ::finish)
                }
                is CarStartupState.Failed -> CarStartupMessage(
                    message = "Tide Player 启动失败\n请重新打开应用",
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startupObserver = lifecycleScope.launch {
            (application as CarApplication).startupState
                .filterIsInstance<CarStartupState.Ready>()
                .first()
            if (!permissionAttached) {
                permissionRepository.onCreate(this@MainActivity, permissionLauncher)
                permissionAttached = true
            }
            connectMediaController()
        }
    }

    override fun onStop() {
        startupObserver?.cancel()
        startupObserver = null
        super.onStop()
    }

    private fun connectMediaController() {
        if (controllerAttached || controllerFuture != null) return
        val future = MediaController.Builder(
            applicationContext,
            SessionToken(
                applicationContext,
                ComponentName(applicationContext, PlaybackService::class.java),
            ),
        ).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (!future.isDone || future.isCancelled) return@addListener
                try {
                    playerControllerRepository.setupMediaController(future.get())
                    controllerAttached = true
                } catch (error: Exception) {
                    Log.e("TidePlayerCar", "Unable to attach the media controller", error)
                }
                controllerFuture = null
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onDestroy() {
        controllerFuture?.cancel(true)
        controllerFuture = null
        if (controllerAttached) {
            playerControllerRepository.destroyMediaController()
            controllerAttached = false
        }
        if (permissionAttached) {
            permissionRepository.onDestroy()
            permissionAttached = false
        }
        super.onDestroy()
    }

    private fun logWindowEvidence(rootSize: IntSize, rootPosition: Offset) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val configuration = resources.configuration
        val display = resources.displayMetrics
        val currentBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.toShortString()
        } else {
            "unavailable-before-api-30"
        }
        val maximumBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.maximumWindowMetrics.bounds.toShortString()
        } else {
            "unavailable-before-api-30"
        }
        val rootInsets = ViewCompat.getRootWindowInsets(window.decorView)
        fun insets(typeMask: Int): String = rootInsets?.getInsets(typeMask)?.let {
            "${it.left},${it.top},${it.right},${it.bottom}"
        } ?: "missing"
        val evidence = buildString {
            append("currentWindow=").append(currentBounds)
            append(" maximumWindow=").append(maximumBounds)
            append(" configurationDp=").append(configuration.screenWidthDp).append('x').append(configuration.screenHeightDp)
            append(" densityDpi=").append(configuration.densityDpi)
            append(" displayPixels=").append(display.widthPixels).append('x').append(display.heightPixels)
            append(" density=").append(display.density)
            append(" composeRootPx=").append(rootSize.width).append('x').append(rootSize.height)
            append(" composeRootPosition=").append(rootPosition.x).append(',').append(rootPosition.y)
            append(" statusBars=").append(insets(WindowInsetsCompat.Type.statusBars()))
            append(" navigationBars=").append(insets(WindowInsetsCompat.Type.navigationBars()))
            append(" displayCutout=").append(insets(WindowInsetsCompat.Type.displayCutout()))
            append(" systemGestures=").append(insets(WindowInsetsCompat.Type.systemGestures()))
            append(" mandatoryGestures=").append(insets(WindowInsetsCompat.Type.mandatorySystemGestures()))
            append(" ime=").append(insets(WindowInsetsCompat.Type.ime()))
        }
        if (evidence != lastWindowEvidence) {
            lastWindowEvidence = evidence
            Log.i(WINDOW_EVIDENCE_TAG, evidence)
        }
    }

    private companion object {
        const val WINDOW_EVIDENCE_TAG = "TideCarWindow"
    }
}

@androidx.compose.runtime.Composable
private fun CarStartupMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1118))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = message,
            style = TextStyle(
                color = Color(0xFFF4F7FA),
                fontSize = 32.sp,
            ),
        )
    }
}

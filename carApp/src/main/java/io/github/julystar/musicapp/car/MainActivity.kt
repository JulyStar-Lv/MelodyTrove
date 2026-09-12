package io.github.julystar.musicapp.car

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.julystar.musicapp.car.presentation.CarRoot
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.car.window.CarAppWindowBounds
import io.github.julystar.musicapp.car.window.CarAppWindowBoundsResolver
import io.github.julystar.musicapp.car.window.OemScreenStateMonitor
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
    private lateinit var oemScreenStateMonitor: OemScreenStateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        oemScreenStateMonitor = OemScreenStateMonitor(contentResolver)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRepository.triggerPermissionChanged()
        }
        setContent {
            val startupState by (application as CarApplication).startupState.collectAsState()
            val exitPlaybackRequest by (application as CarApplication).exitPlaybackRequest.collectAsState()
            val profileHint by oemScreenStateMonitor.profileHint.collectAsState()
            TideCarAppWindow(
                profileHint = profileHint,
                onRootPositioned = ::logWindowEvidence,
            ) { panelSize, effectiveHint ->
                when (val state = startupState) {
                    CarStartupState.Initializing -> CarStartupMessage("正在准备音乐库…")
                    CarStartupState.Ready -> {
                        val metrics = remember(panelSize, effectiveHint) {
                            layoutProfileResolver.resolve(panelSize, hint = effectiveHint)
                        }
                        LaunchedEffect(metrics.profile, metrics.contentSize) {
                            Log.i(
                                WINDOW_EVIDENCE_TAG,
                                "resolvedProfile=${metrics.profile} usableDp=${metrics.usableSize} contentDp=${metrics.contentSize}",
                            )
                        }
                        CarRoot(
                            metrics = metrics,
                            onExit = ::finish,
                            onEnterFullscreen = ::openFullscreenPlayback,
                            exitPlaybackRequest = exitPlaybackRequest,
                        )
                    }
                    CarStartupState.RecoveryRequired -> CarStartupMessage(
                        message = "检测到上次异常退出\n请先在手机端完成安全模式恢复",
                    )
                    is CarStartupState.Failed -> CarStartupMessage(
                        message = "Tide Player 启动失败\n请重新打开应用",
                    )
                }
            }
        }
    }

    private fun openFullscreenPlayback() {
        applicationContext.startActivity(
            Intent(applicationContext, FullscreenPlaybackActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                ),
        )
        overridePendingTransition(0, 0)
    }

    override fun onStart() {
        super.onStart()
        oemScreenStateMonitor.start()
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
        oemScreenStateMonitor.stop()
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

@Composable
internal fun TideCarAppWindow(
    profileHint: CarLayoutProfileHint,
    onRootPositioned: (IntSize, Offset) -> Unit,
    content: @Composable (DpSize, CarLayoutProfileHint) -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                onRootPositioned(coordinates.size, coordinates.positionInWindow())
            },
    ) {
        val density = LocalDensity.current
        val hostSizePx = with(density) {
            IntSize(maxWidth.roundToPx(), maxHeight.roundToPx())
        }
        val bounds = remember(hostSizePx, profileHint) {
            CarAppWindowBoundsResolver.resolve(hostSizePx, profileHint)
        }
        val boundsTransition = updateTransition(
            targetState = bounds,
            label = "car-app-window-bounds",
        )
        fun Transition.Segment<CarAppWindowBounds>.windowAnimationSpec() =
            if (initialState.embeddedInCockpit && targetState.embeddedInCockpit) {
                tween<Int>(WINDOW_TRANSITION_DURATION_MILLIS, easing = LinearEasing)
            } else {
                snap<Int>()
            }
        val animatedLeft = boundsTransition.animateInt(
            transitionSpec = { windowAnimationSpec() },
            label = "car-app-window-left",
        ) { it.left }
        val animatedTop = boundsTransition.animateInt(
            transitionSpec = { windowAnimationSpec() },
            label = "car-app-window-top",
        ) { it.top }
        val animatedRight = boundsTransition.animateInt(
            transitionSpec = { windowAnimationSpec() },
            label = "car-app-window-right",
        ) { it.left + it.width }
        val animatedBottom = boundsTransition.animateInt(
            transitionSpec = { windowAnimationSpec() },
            label = "car-app-window-bottom",
        ) { it.top + it.height }
        // Switch the responsive layout once, at transition start, then scale that stable
        // target layout with the animated frame. This avoids both transparent edge gaps
        // while expanding and the delayed layout jump at the transition endpoint.
        val contentBounds = boundsTransition.targetState
        val contentSize = with(density) {
            DpSize(contentBounds.width.toDp(), contentBounds.height.toDp())
        }

        LaunchedEffect(bounds) {
            Log.i(
                "TideCarWindow",
                "panelPx=[${bounds.left},${bounds.top} ${bounds.width}x${bounds.height}] " +
                    "embedded=${bounds.embeddedInCockpit} hint=${bounds.profileHint}",
            )
        }
        Layout(
            content = { content(contentSize, contentBounds.profileHint) },
            modifier = Modifier
                .offset { IntOffset(animatedLeft.value, animatedTop.value) }
                .clip(RoundedCornerShape(8.dp)),
        ) { measurables, _ ->
            val placeable = measurables.single().measure(
                Constraints.fixed(contentBounds.width, contentBounds.height),
            )
            val frameWidth = (animatedRight.value - animatedLeft.value).coerceAtLeast(1)
            val frameHeight = (animatedBottom.value - animatedTop.value).coerceAtLeast(1)
            layout(frameWidth, frameHeight) {
                placeable.placeWithLayer(0, 0) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = frameWidth.toFloat() / contentBounds.width
                    scaleY = frameHeight.toFloat() / contentBounds.height
                }
            }
        }
    }
}

@Composable
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

private const val WINDOW_TRANSITION_DURATION_MILLIS = 300

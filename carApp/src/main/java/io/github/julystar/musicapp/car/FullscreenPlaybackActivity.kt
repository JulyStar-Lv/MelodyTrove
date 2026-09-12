package io.github.julystar.musicapp.car

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import io.github.julystar.musicapp.car.presentation.CarRoot
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileHint
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import org.koin.android.ext.android.inject

/**
 * Uses a separate unresizable task so the cockpit window manager grants the
 * physical 5120-pixel surface instead of inheriting applist's 2560-pixel task.
 */
class FullscreenPlaybackActivity : ComponentActivity() {
    private val layoutProfileResolver: CarLayoutProfileResolver by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.isNavigationBarContrastEnforced = false
        setContent {
            val startupState by (application as CarApplication).startupState.collectAsState()
            TideCarAppWindow(
                profileHint = CarLayoutProfileHint.FullscreenCockpit,
                onRootPositioned = { _, _ -> },
            ) { panelSize, effectiveHint ->
                val metrics = remember(panelSize, effectiveHint) {
                    layoutProfileResolver.resolve(panelSize, hint = effectiveHint)
                }
                when (startupState) {
                    CarStartupState.Ready -> CarRoot(
                        metrics = metrics,
                        onExit = ::finishWithoutAnimation,
                        onExitPlayback = ::exitPlayback,
                        onExitFullscreen = ::finishWithoutAnimation,
                    )
                    else -> LaunchedEffect(startupState) {
                        if (startupState is CarStartupState.Failed) finishWithoutAnimation()
                    }
                }
            }
        }
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun exitPlayback() {
        (application as CarApplication).requestExitPlayback()
        finishWithoutAnimation()
    }
}

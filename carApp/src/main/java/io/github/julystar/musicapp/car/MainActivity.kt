package io.github.julystar.musicapp.car

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.julystar.musicapp.car.presentation.CarRoot
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfileResolver
import io.github.julystar.musicapp.core.PlaybackService
import io.github.julystar.musicapp.singleton.PlayerControllerRepository
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val layoutProfileResolver: CarLayoutProfileResolver by inject()
    private val playerControllerRepository: PlayerControllerRepository by inject()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controllerAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val metrics = remember(maxWidth, maxHeight) {
                    layoutProfileResolver.resolve(DpSize(maxWidth, maxHeight))
                }
                CarRoot(metrics = metrics)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectMediaController()
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
                runCatching {
                    playerControllerRepository.setupMediaController(future.get())
                    controllerAttached = true
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
        super.onDestroy()
    }
}

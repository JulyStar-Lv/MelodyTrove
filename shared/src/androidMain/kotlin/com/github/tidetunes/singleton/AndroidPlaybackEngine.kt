package com.github.tidetunes.singleton

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_STOP
import androidx.media3.session.MediaController
import com.github.tidetunes.core.playUtil
import com.github.tidetunes.core.domain.model.DiagnosticLogCategory
import com.github.tidetunes.diagnostics.TideLogger
import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal interface AndroidPlaybackEngine : PlaybackEngine

internal class MediaControllerAndroidPlaybackEngine(
    private val mediaController: MediaController,
    private val bridge: Bridge,
    private val scope: CoroutineScope,
) : AndroidPlaybackEngine {
    private val applicationHandler = Handler(mediaController.applicationLooper)

    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        return runOnApplicationThread {
            val resource = request.resource
            if (resource.isExpired(nowEpochMs = System.currentTimeMillis())) {
                return@runOnApplicationThread PlaybackEngineLoadResult.Failure(
                    PlaybackEngineFailureReason.ExpiredResource
                )
            }
            playUtil(
                item = request.item,
                player = mediaController,
                playbackUri = resource.uri,
            )
            PlaybackEngineLoadResult.Ready
        }
    }

    override fun play() {
        runOnApplicationThread {
            if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                mediaController.play()
            } else {
                TideLogger.warn(
                    DiagnosticLogCategory.Playback,
                    "AndroidPlaybackEngine",
                    "Resume command is unavailable",
                )
            }
        }
    }

    override fun pause() {
        runOnApplicationThread {
            if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
                mediaController.pause()
            } else {
                TideLogger.warn(
                    DiagnosticLogCategory.Playback,
                    "AndroidPlaybackEngine",
                    "Pause command is unavailable",
                )
            }
        }
    }

    override fun stop() {
        runOnApplicationThread {
            if (mediaController.isCommandAvailable(COMMAND_STOP)) {
                mediaController.stop()
            } else {
                TideLogger.warn(
                    DiagnosticLogCategory.Playback,
                    "AndroidPlaybackEngine",
                    "Stop command is unavailable",
                )
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        runOnApplicationThread {
            if (mediaController.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                mediaController.seekTo(positionMs.coerceAtLeast(0))
            } else {
                TideLogger.warn(
                    DiagnosticLogCategory.Playback,
                    "AndroidPlaybackEngine",
                    "Seek command is unavailable",
                )
            }
        }
    }

    override fun readPosition(): PlaybackPosition {
        return runOnApplicationThread {
            PlaybackPosition(
                positionMs = mediaController.currentPosition.coerceAtLeast(0),
                bufferedMs = mediaController.bufferedPosition.coerceAtLeast(0),
                durationMs = mediaController.duration.coerceAtLeast(0),
            )
        }
    }

    override fun release() {
        runOnApplicationThread {
            mediaController.release()
        }
    }

    private fun <T : Any> runOnApplicationThread(block: () -> T): T {
        if (Looper.myLooper() == mediaController.applicationLooper) {
            return block()
        }

        val value = AtomicReference<T?>()
        val throwable = AtomicReference<Throwable?>()
        val latch = CountDownLatch(1)
        check(applicationHandler.post {
            try {
                value.set(block())
            } catch (error: Throwable) {
                throwable.set(error)
            } finally {
                latch.countDown()
            }
        }) {
            "MediaController application thread is unavailable"
        }
        latch.await()
        throwable.get()?.let { throw it }
        return checkNotNull(value.get()) {
            "MediaController application thread completed without a result"
        }
    }
}

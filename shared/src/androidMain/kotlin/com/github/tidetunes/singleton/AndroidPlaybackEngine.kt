package com.github.tidetunes.singleton

import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_STOP
import androidx.media3.session.MediaController
import com.github.tidetunes.core.playUtil
import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import kotlinx.coroutines.CoroutineScope
import uniffi.tidetunes_core.tidetunesError

internal interface AndroidPlaybackEngine : PlaybackEngine

internal class MediaControllerAndroidPlaybackEngine(
    private val mediaController: MediaController,
    private val bridge: Bridge,
    private val scope: CoroutineScope,
) : AndroidPlaybackEngine {
    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        val resource = request.resource
        if (resource.isExpired(nowEpochMs = System.currentTimeMillis())) {
            return PlaybackEngineLoadResult.Failure(
                PlaybackEngineFailureReason.ExpiredResource
            )
        }
        playUtil(
            item = request.item,
            player = mediaController,
            playbackUri = resource.uri,
        )
        return PlaybackEngineLoadResult.Ready
    }

    override fun play() {
        if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
            mediaController.play()
        } else {
            tidetunesError("media controller resume failed, command COMMAND_PLAY_PAUSE is unavailable")
        }
    }

    override fun pause() {
        if (mediaController.isCommandAvailable(COMMAND_PLAY_PAUSE)) {
            mediaController.pause()
        } else {
            tidetunesError("media controller pause failed, command COMMAND_PLAY_PAUSE is unavailable")
        }
    }

    override fun stop() {
        if (mediaController.isCommandAvailable(COMMAND_STOP)) {
            mediaController.stop()
        } else {
            tidetunesError("media controller stop failed, command COMMAND_STOP is unavailable")
        }
    }

    override fun seekTo(positionMs: Long) {
        if (mediaController.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            mediaController.seekTo(positionMs.coerceAtLeast(0))
        } else {
            tidetunesError("media controller seek failed, command COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM is unavailable")
        }
    }

    override fun readPosition(): PlaybackPosition {
        return PlaybackPosition(
            positionMs = mediaController.currentPosition.coerceAtLeast(0),
            bufferedMs = mediaController.bufferedPosition.coerceAtLeast(0),
            durationMs = mediaController.duration.coerceAtLeast(0),
        )
    }

    override fun release() {
        mediaController.release()
    }
}

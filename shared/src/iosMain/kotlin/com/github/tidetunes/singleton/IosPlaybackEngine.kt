package com.github.tidetunes.singleton

import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*

internal interface IosPlaybackEngine : PlaybackEngine

@OptIn(ExperimentalForeignApi::class)
internal class AvPlayerIosPlaybackEngine : IosPlaybackEngine {
    private val player = AVPlayer()

    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        val resource = request.resource
        if (resource.isExpired(nowEpochMs = currentTimeMillis())) {
            return PlaybackEngineLoadResult.Failure(
                PlaybackEngineFailureReason.ExpiredResource
            )
        }
        val url = NSURL.URLWithString(resource.uri)
            ?: return PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.UnsupportedResource
            )
        val item = AVPlayerItem.playerItemWithURL(url)
        player.replaceCurrentItemWithPlayerItem(item)
        return PlaybackEngineLoadResult.Ready
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMake(value = positionMs.coerceAtLeast(0), timescale = 1_000))
    }

    override fun readPosition(): PlaybackPosition {
        val currentPositionMs = secondsToMillis(CMTimeGetSeconds(player.currentTime()))
        val seconds = player.currentItem?.let { CMTimeGetSeconds(it.duration) }
            ?: return PlaybackPosition.Zero
        return PlaybackPosition(
            positionMs = currentPositionMs,
            bufferedMs = currentPositionMs,
            durationMs = secondsToMillis(seconds),
        )
    }

    override fun release() {
        stop()
    }

    private fun secondsToMillis(seconds: Double): Long {
        return if (seconds.isFinite() && seconds >= 0.0) {
            (seconds * 1_000.0).toLong()
        } else {
            0L
        }
    }
}

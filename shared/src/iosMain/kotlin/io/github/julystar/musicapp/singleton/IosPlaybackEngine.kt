package io.github.julystar.musicapp.singleton

import io.github.julystar.musicapp.core.audio.tap.TideDspAudioTapAttach
import io.github.julystar.musicapp.core.audio.tap.TideDspAudioTapDetach
import io.github.julystar.musicapp.core.audio.tap.TideDspAudioTapReset
import io.github.julystar.musicapp.core.audio.toNativeDspConfiguration
import io.github.julystar.musicapp.core.domain.model.AudioEffectSettings
import io.github.julystar.musicapp.platform.currentTimeMillis
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngine
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineFailureReason
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineLoadRequest
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineLoadResult
import io.github.julystar.musicapp.service.playback.domain.PlaybackEngineUnsupportedReason
import io.github.julystar.musicapp.service.playback.domain.PlaybackPosition
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import uniffi.app_backend.NativeAudioDsp
import uniffi.app_backend.ctCreateAudioDspProcessor

internal interface IosPlaybackEngine : PlaybackEngine {
    val playbackCompleted: Flow<Unit>
    fun seekTo(positionMs: Long, completionHandler: (Boolean) -> Unit)
    fun updateAudioDsp(settings: AudioEffectSettings, inputGainDb: Float) = Unit
}

@OptIn(ExperimentalForeignApi::class)
internal class AvPlayerIosPlaybackEngine : IosPlaybackEngine {
    private val player = AVPlayer()
    private val nativeDsp: NativeAudioDsp = ctCreateAudioDspProcessor()
    private val nativeDspHandle = nativeDsp.nativeHandle()
    private val _playbackCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val playbackCompleted = _playbackCompleted.asSharedFlow()
    private val playbackCompletedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
        name = AVPlayerItemDidPlayToEndTimeNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { notification ->
        if (notification?.`object` == player.currentItem) {
            _playbackCompleted.tryEmit(Unit)
        }
    }

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
        player.currentItem?.let(::TideDspAudioTapDetach)
        TideDspAudioTapReset(nativeDspHandle)
        TideDspAudioTapAttach(item, nativeDspHandle)
        player.replaceCurrentItemWithPlayerItem(item)
        return PlaybackEngineLoadResult.Ready
    }

    override fun updateAudioDsp(
        settings: AudioEffectSettings,
        inputGainDb: Float,
    ) {
        nativeDsp.updateConfig(settings.toNativeDspConfiguration(inputGainDb))
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.pause()
        player.currentItem?.let(::TideDspAudioTapDetach)
        player.replaceCurrentItemWithPlayerItem(null)
        TideDspAudioTapReset(nativeDspHandle)
    }

    override fun seekTo(positionMs: Long) {
        seekTo(positionMs) { }
    }

    override fun seekTo(positionMs: Long, completionHandler: (Boolean) -> Unit) {
        TideDspAudioTapReset(nativeDspHandle)
        player.seekToTime(
            time = CMTimeMake(value = positionMs.coerceAtLeast(0), timescale = 1_000),
            toleranceBefore = CMTimeMake(value = 0, timescale = 1),
            toleranceAfter = CMTimeMake(value = 0, timescale = 1),
            completionHandler = completionHandler,
        )
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
        NSNotificationCenter.defaultCenter.removeObserver(playbackCompletedObserver)
        nativeDsp.close()
    }

    private fun secondsToMillis(seconds: Double): Long {
        return if (seconds.isFinite() && seconds >= 0.0) {
            (seconds * 1_000.0).toLong()
        } else {
            0L
        }
    }
}

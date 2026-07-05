package com.github.tidetunes.singleton

import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import uniffi.tidetunes_core.DesktopMpvLoadResult
import uniffi.tidetunes_core.DesktopMpvPlayer
import uniffi.tidetunes_core.ctCreateDesktopMpvPlayer

interface DesktopPlaybackEngine : PlaybackEngine

class NoopDesktopPlaybackEngine : DesktopPlaybackEngine {
    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        return PlaybackEngineLoadResult.Unsupported(
            PlaybackEngineUnsupportedReason.MissingPlatformEngine
        )
    }

    override fun play() = Unit

    override fun pause() = Unit

    override fun stop() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun readPosition(): PlaybackPosition = PlaybackPosition.Zero

    override fun release() = Unit
}

class MpvDesktopPlaybackEngine internal constructor(
    private val runtime: DesktopMpvRuntime = UniffiDesktopMpvRuntime(),
) : DesktopPlaybackEngine {
    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        val resource = request.resource
        if (resource.isExpired(nowEpochMs = System.currentTimeMillis())) {
            return PlaybackEngineLoadResult.Failure(
                PlaybackEngineFailureReason.ExpiredResource
            )
        }
        return if (runtime.load(resource.uri, resource.headers)) {
            PlaybackEngineLoadResult.Ready
        } else {
            PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.UnsupportedResource
            )
        }
    }

    override fun play() {
        runtime.play()
    }

    override fun pause() {
        runtime.pause()
    }

    override fun stop() {
        runtime.stop()
    }

    override fun seekTo(positionMs: Long) {
        runtime.seek(positionMs.coerceAtLeast(0).toULong())
    }

    override fun readPosition(): PlaybackPosition {
        return PlaybackPosition(
            positionMs = runtime.currentPositionMs().coerceAtLeast(0),
            bufferedMs = runtime.bufferedPositionMs().coerceAtLeast(0),
            durationMs = runtime.durationMs().coerceAtLeast(0),
        )
    }

    override fun release() = stop()
}

internal interface DesktopMpvRuntime {
    fun load(uri: String, headers: Map<String, String>): Boolean
    fun play()
    fun pause()
    fun stop()
    fun seek(ms: ULong)
    fun currentPositionMs(): Long
    fun bufferedPositionMs(): Long
    fun durationMs(): Long
}

private class UniffiDesktopMpvRuntime(
    private val player: DesktopMpvPlayer = ctCreateDesktopMpvPlayer(),
) : DesktopMpvRuntime {
    override fun load(uri: String, headers: Map<String, String>): Boolean {
        return player.load(
            uri = uri,
            httpHeaderFields = headers.toMpvHttpHeaderFields(),
        ) == DesktopMpvLoadResult.READY
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.stop()
    }

    override fun seek(ms: ULong) {
        player.seek(ms)
    }

    override fun currentPositionMs(): Long = player.currentPositionMs()

    override fun bufferedPositionMs(): Long = player.bufferedPositionMs()

    override fun durationMs(): Long = player.durationMs()
}

private fun Map<String, String>.toMpvHttpHeaderFields(): String {
    return entries
        .filter { (name, value) -> name.isNotBlank() && value.isNotBlank() }
        .joinToString("\n") { (name, value) ->
            "${name.trim()}: ${value.trim()}"
        }
}

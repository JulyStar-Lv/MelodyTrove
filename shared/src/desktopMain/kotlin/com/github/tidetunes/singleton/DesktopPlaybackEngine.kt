package com.github.tidetunes.singleton

import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uniffi.tidetunes_core.DesktopMpvLoadResult
import uniffi.tidetunes_core.DesktopMpvPlayer
import uniffi.tidetunes_core.ctCreateDesktopMpvPlayer
import uniffi.tidetunes_core.tidetunesError

interface DesktopPlaybackEngine : PlaybackEngine

class FallbackDesktopPlaybackEngine internal constructor(
    private val primary: DesktopPlaybackEngine,
    private val fallback: DesktopPlaybackEngine,
) : DesktopPlaybackEngine {
    private var activeEngine: DesktopPlaybackEngine? = null

    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        activeEngine = null
        return when (val primaryResult = primary.load(request)) {
            PlaybackEngineLoadResult.Ready -> {
                activeEngine = primary
                primaryResult
            }
            is PlaybackEngineLoadResult.Failure -> {
                if (primaryResult.reason == PlaybackEngineFailureReason.ExpiredResource) {
                    primaryResult
                } else {
                    loadFallback(request)
                }
            }
            is PlaybackEngineLoadResult.Unsupported -> {
                loadFallback(request)
            }
        }
    }

    override fun play() {
        activeEngine?.play()
    }

    override fun pause() {
        activeEngine?.pause()
    }

    override fun stop() {
        activeEngine?.stop()
        activeEngine = null
    }

    override fun seekTo(positionMs: Long) {
        activeEngine?.seekTo(positionMs)
    }

    override fun readPosition(): PlaybackPosition {
        return activeEngine?.readPosition() ?: PlaybackPosition.Zero
    }

    override fun release() {
        primary.release()
        fallback.release()
        activeEngine = null
    }

    private fun loadFallback(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        primary.stop()
        return when (val fallbackResult = fallback.load(request)) {
            PlaybackEngineLoadResult.Ready -> {
                activeEngine = fallback
                fallbackResult
            }
            else -> fallbackResult
        }
    }
}

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

class VlcjDesktopPlaybackEngine internal constructor(
    private val runtimeFactory: () -> DesktopVlcjRuntime? = { VlcjDesktopRuntime.createOrNull() },
) : DesktopPlaybackEngine {
    private var runtime: DesktopVlcjRuntime? = null

    override fun load(request: PlaybackEngineLoadRequest): PlaybackEngineLoadResult {
        val resource = request.resource
        if (resource.isExpired(nowEpochMs = System.currentTimeMillis())) {
            return PlaybackEngineLoadResult.Failure(
                PlaybackEngineFailureReason.ExpiredResource
            )
        }

        val runtime = runtime ?: runtimeFactory()?.also { runtime = it }
            ?: return PlaybackEngineLoadResult.Unsupported(
                PlaybackEngineUnsupportedReason.MissingPlatformEngine
            )

        return try {
            if (runtime.load(resource.uri, resource.toVlcjMediaOptions())) {
                PlaybackEngineLoadResult.Ready
            } else {
                PlaybackEngineLoadResult.Unsupported(
                    PlaybackEngineUnsupportedReason.UnsupportedResource
                )
            }
        } catch (error: Throwable) {
            tidetunesError("desktop VLCJ playback failed to load: ${error.message ?: error}")
            PlaybackEngineLoadResult.Failure(PlaybackEngineFailureReason.EngineError)
        }
    }

    override fun play() {
        runtime?.play()
    }

    override fun pause() {
        runtime?.pause()
    }

    override fun stop() {
        runtime?.stop()
    }

    override fun seekTo(positionMs: Long) {
        runtime?.seek(positionMs.coerceAtLeast(0))
    }

    override fun readPosition(): PlaybackPosition {
        val runtime = runtime ?: return PlaybackPosition.Zero
        return PlaybackPosition(
            positionMs = runtime.currentPositionMs().coerceAtLeast(0),
            bufferedMs = runtime.bufferedPositionMs().coerceAtLeast(0),
            durationMs = runtime.durationMs().coerceAtLeast(0),
        )
    }

    override fun release() {
        runtime?.release()
        runtime = null
    }
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

internal interface DesktopVlcjRuntime {
    fun load(uri: String, options: List<String>): Boolean
    fun play()
    fun pause()
    fun stop()
    fun seek(ms: Long)
    fun currentPositionMs(): Long
    fun bufferedPositionMs(): Long
    fun durationMs(): Long
    fun release()
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

private class VlcjDesktopRuntime private constructor(
    private val factory: MediaPlayerFactory,
    private val player: MediaPlayer,
) : DesktopVlcjRuntime {
    override fun load(uri: String, options: List<String>): Boolean {
        player.controls().stop()
        return player.media().startPaused(uri, *options.toTypedArray())
    }

    override fun play() {
        player.controls().play()
    }

    override fun pause() {
        player.controls().setPause(true)
    }

    override fun stop() {
        player.controls().stop()
    }

    override fun seek(ms: Long) {
        player.controls().setTime(ms)
    }

    override fun currentPositionMs(): Long = player.status().time()

    override fun bufferedPositionMs(): Long = currentPositionMs()

    override fun durationMs(): Long = player.status().length()

    override fun release() {
        player.release()
        factory.release()
    }

    companion object {
        fun createOrNull(): DesktopVlcjRuntime? {
            return try {
                NativeDiscovery().discover()
                val factory = MediaPlayerFactory("--no-video", "--quiet", "--intf", "dummy")
                VlcjDesktopRuntime(
                    factory = factory,
                    player = factory.mediaPlayers().newMediaPlayer(),
                )
            } catch (error: Throwable) {
                tidetunesError("desktop VLCJ playback engine unavailable: ${error.message ?: error}")
                null
            }
        }
    }
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

private fun com.github.tidetunes.service.playback.domain.PlaybackEngineResource.toVlcjMediaOptions(): List<String> {
    val options = mutableListOf(":no-video")
    headers["User-Agent"]?.takeIf { it.isNotBlank() }?.let { userAgent ->
        options += ":http-user-agent=$userAgent"
    }
    headers["Referer"]?.takeIf { it.isNotBlank() }?.let { referer ->
        options += ":http-referrer=$referer"
    }
    return options
}

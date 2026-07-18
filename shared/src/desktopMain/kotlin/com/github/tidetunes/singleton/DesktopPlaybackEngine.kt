package com.github.tidetunes.singleton

import com.github.tidetunes.core.domain.model.AudioEffectSettings
import com.github.tidetunes.core.domain.model.PlaybackAdvancedSettings
import com.github.tidetunes.service.playback.domain.PlaybackEngine
import com.github.tidetunes.service.playback.domain.PlaybackEngineFailureReason
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadRequest
import com.github.tidetunes.service.playback.domain.PlaybackEngineLoadResult
import com.github.tidetunes.service.playback.domain.PlaybackEngineUnsupportedReason
import com.github.tidetunes.service.playback.domain.PlaybackPosition
import uniffi.tidetunes_backend.DesktopRodioLoadResult
import uniffi.tidetunes_backend.DesktopRodioPlayer
import uniffi.tidetunes_backend.ctCreateDesktopRodioPlayer

interface DesktopPlaybackEngine : PlaybackEngine {
    fun configureAudioProcessing(
        effects: AudioEffectSettings,
        playback: PlaybackAdvancedSettings,
        replayGainDb: Float,
    ) = Unit
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

class RodioDesktopPlaybackEngine internal constructor(
    private val runtime: DesktopRodioRuntime = UniffiDesktopRodioRuntime(),
) : DesktopPlaybackEngine {
    override fun configureAudioProcessing(
        effects: AudioEffectSettings,
        playback: PlaybackAdvancedSettings,
        replayGainDb: Float,
    ) {
        runtime.configureAudioProcessing(
            enabled = effects.enabled,
            eqBandGainsDb = effects.eqBandGainsDb.map(Int::toFloat),
            eqQ = effects.eqQHundredths / 100f,
            bassDb = effects.bassDb.toFloat(),
            trebleDb = effects.trebleDb.toFloat(),
            compressorEnabled = effects.compressorEnabled,
            compressorThresholdDb = effects.compressorThresholdDb.toFloat(),
            compressorRatio = effects.compressorRatio.toFloat(),
            compressorMakeupDb = effects.compressorMakeupDb.toFloat(),
            stereoWidth = effects.stereoWidthPercent / 100f,
            reverbPreset = effects.reverbPreset.ordinal.toUByte(),
            replayGainDb = replayGainDb,
            crossfadeDurationMs = playback.crossfadeDurationMs.toULong(),
        )
    }

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

internal interface DesktopRodioRuntime {
    fun load(uri: String, headers: Map<String, String>): Boolean
    fun play()
    fun pause()
    fun stop()
    fun seek(ms: ULong)
    fun currentPositionMs(): Long
    fun bufferedPositionMs(): Long
    fun durationMs(): Long
    fun configureAudioProcessing(
        enabled: Boolean,
        eqBandGainsDb: List<Float>,
        eqQ: Float,
        bassDb: Float,
        trebleDb: Float,
        compressorEnabled: Boolean,
        compressorThresholdDb: Float,
        compressorRatio: Float,
        compressorMakeupDb: Float,
        stereoWidth: Float,
        reverbPreset: UByte,
        replayGainDb: Float,
        crossfadeDurationMs: ULong,
    ) = Unit
}

private class UniffiDesktopRodioRuntime(
    private val player: DesktopRodioPlayer = ctCreateDesktopRodioPlayer(),
) : DesktopRodioRuntime {
    override fun load(uri: String, headers: Map<String, String>): Boolean {
        return player.load(
            uri = uri,
            httpHeaderFields = headers.toHttpHeaderFields(),
        ) == DesktopRodioLoadResult.READY
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

    override fun configureAudioProcessing(
        enabled: Boolean,
        eqBandGainsDb: List<Float>,
        eqQ: Float,
        bassDb: Float,
        trebleDb: Float,
        compressorEnabled: Boolean,
        compressorThresholdDb: Float,
        compressorRatio: Float,
        compressorMakeupDb: Float,
        stereoWidth: Float,
        reverbPreset: UByte,
        replayGainDb: Float,
        crossfadeDurationMs: ULong,
    ) {
        player.configureAudioProcessing(
            enabled = enabled,
            eqBandGainsDb = eqBandGainsDb,
            eqQ = eqQ,
            bassDb = bassDb,
            trebleDb = trebleDb,
            compressorEnabled = compressorEnabled,
            compressorThresholdDb = compressorThresholdDb,
            compressorRatio = compressorRatio,
            compressorMakeupDb = compressorMakeupDb,
            stereoWidth = stereoWidth,
            reverbPreset = reverbPreset,
            replayGainDb = replayGainDb,
            crossfadeDurationMs = crossfadeDurationMs,
        )
    }
}

private fun Map<String, String>.toHttpHeaderFields(): String {
    return entries
        .filter { (name, value) -> name.isNotBlank() && value.isNotBlank() }
        .joinToString("\n") { (name, value) ->
            "${name.trim()}: ${value.trim()}"
        }
}

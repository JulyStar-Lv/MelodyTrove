package io.github.julystar.musicapp.core.data.settings

import io.github.julystar.musicapp.core.audio.toNativeDspConfiguration
import io.github.julystar.musicapp.core.domain.model.AudioEffectSettings
import io.github.julystar.musicapp.core.domain.repository.AudioDspAnalysisRepository
import io.github.julystar.musicapp.core.domain.repository.AudioDspFrequencyResponse
import kotlin.math.pow
import uniffi.app_backend.ctCalculateDspFrequencyResponse

object RustAudioDspAnalysisRepository : AudioDspAnalysisRepository {
    private const val POINT_COUNT = 96

    override fun calculateFrequencyResponse(
        settings: AudioEffectSettings,
        sampleRate: UInt,
    ): AudioDspFrequencyResponse {
        val upperFrequency = minOf(20_000.0, sampleRate.toDouble() * 0.48)
        val ratio = upperFrequency / 20.0
        val frequencies = List(POINT_COUNT) { index ->
            (20.0 * ratio.pow(index.toDouble() / (POINT_COUNT - 1))).toFloat()
        }
        val gains = ctCalculateDspFrequencyResponse(
            config = settings.toNativeDspConfiguration(),
            sampleRate = sampleRate,
            frequenciesHz = frequencies,
        )
        return if (gains.size == frequencies.size && gains.all(Float::isFinite)) {
            AudioDspFrequencyResponse(frequencies, gains)
        } else {
            AudioDspFrequencyResponse.Empty
        }
    }
}

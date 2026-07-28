package io.github.julystar.musicapp.core.domain.repository

import io.github.julystar.musicapp.core.domain.model.AudioEffectSettings

data class AudioDspFrequencyResponse(
    val frequenciesHz: List<Float>,
    val gainsDb: List<Float>,
) {
    companion object {
        val Empty = AudioDspFrequencyResponse(emptyList(), emptyList())
    }
}

interface AudioDspAnalysisRepository {
    fun calculateFrequencyResponse(
        settings: AudioEffectSettings,
        sampleRate: UInt = 48_000u,
    ): AudioDspFrequencyResponse
}

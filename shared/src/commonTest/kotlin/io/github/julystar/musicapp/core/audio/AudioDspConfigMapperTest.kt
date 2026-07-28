package io.github.julystar.musicapp.core.audio

import io.github.julystar.musicapp.core.domain.model.AudioEffectProfile
import io.github.julystar.musicapp.core.domain.model.AudioEffectSettings
import io.github.julystar.musicapp.core.domain.model.CompressorSettings
import io.github.julystar.musicapp.core.domain.model.MoogFilterSettings
import io.github.julystar.musicapp.core.domain.model.ReverbPreset
import io.github.julystar.musicapp.core.domain.model.ReverbSettings
import io.github.julystar.musicapp.core.domain.model.SpatialAudioMode
import io.github.julystar.musicapp.core.domain.model.SpatialAudioSettings
import io.github.julystar.musicapp.core.domain.model.withAudioEffectProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uniffi.app_backend.DspReverbPreset
import uniffi.app_backend.DspSpatialMode

class AudioDspConfigMapperTest {
    @Test
    fun replayGainDoesNotEnableSavedEffectsWhenMasterSwitchIsOff() {
        val settings = AudioEffectSettings.Default.withAudioEffectProfile(
            AudioEffectProfile.Default.copy(
                compressor = CompressorSettings(enabled = true),
                moogFilter = MoogFilterSettings(enabled = true),
                spatialAudio = SpatialAudioSettings(mode = SpatialAudioMode.Surround360),
                reverb = ReverbSettings(preset = ReverbPreset.Hall),
            )
        )

        val config = settings.toNativeDspConfiguration(inputGainDb = -4.5f)

        assertTrue(config.enabled)
        assertEquals(-4.5f, config.inputGainDb)
        assertFalse(config.graphicEqualizer.enabled)
        assertFalse(config.compressor.enabled)
        assertFalse(config.moogFilter.enabled)
        assertEquals(DspSpatialMode.NONE, config.spatialAudio.mode)
        assertEquals(DspReverbPreset.NONE, config.reverb.preset)
        assertTrue(config.limiter.enabled)
    }

    @Test
    fun masterSwitchEnablesConfiguredEffects() {
        val settings = AudioEffectSettings.Default.copy(enabled = true)
            .withAudioEffectProfile(
                AudioEffectProfile.Default.copy(
                    compressor = CompressorSettings(enabled = true),
                    spatialAudio = SpatialAudioSettings(mode = SpatialAudioMode.Surround360),
                )
            )

        val config = settings.toNativeDspConfiguration()

        assertTrue(config.graphicEqualizer.enabled)
        assertTrue(config.compressor.enabled)
        assertEquals(DspSpatialMode.SURROUND360, config.spatialAudio.mode)
    }
}

package io.github.julystar.musicapp.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioEffectSettingsTest {
    @Test
    fun legacyFieldsMigrateIntoVersionedProfile() {
        val normalized = normalizeAudioEffectSettings(
            AudioEffectSettings(
                eqBandGainsDb = listOf(6),
                bassDb = 4,
                compressorEnabled = true,
                stereoWidthPercent = 125,
            )
        )

        assertEquals(AUDIO_DSP_SCHEMA_VERSION, normalized.schemaVersion)
        assertEquals(6, normalized.profile.graphicEqualizer.bandGainsDb.first())
        assertEquals(4, normalized.profile.tone.bassGainDb)
        assertTrue(normalized.profile.compressor.enabled)
        assertEquals(125, normalized.profile.stereoWidth.widthPercent)
    }

    @Test
    fun versionedProfileIsNotOverwrittenByLegacyMirrors() {
        val settings = AudioEffectSettings.Default.withAudioEffectProfile(
            AudioEffectProfile.Default.copy(
                equalizerMode = EqualizerMode.Parametric,
                parametricEqualizer = ParametricEqualizerSettings(
                    enabled = true,
                    bands = listOf(ParametricEqBand(frequencyHz = 2_400)),
                ),
                tone = ToneControlSettings(bassGainDb = 7),
            )
        )
        val normalized = normalizeAudioEffectSettings(settings.copy(bassDb = -12))

        assertEquals(EqualizerMode.Parametric, normalized.profile.equalizerMode)
        assertEquals(2_400, normalized.profile.parametricEqualizer.bands.single().frequencyHz)
        assertEquals(7, normalized.profile.tone.bassGainDb)
        assertEquals(7, normalized.bassDb)
    }

    @Test
    fun profileAndLegacyMirrorsStaySynchronized() {
        val normalized = AudioEffectSettings.Default.withAudioEffectProfile(
            AudioEffectProfile.Default.copy(
                graphicEqualizer = GraphicEqualizerSettings(
                    bandGainsDb = List(EQ_BAND_COUNT) { it - 5 },
                    qHundredths = 175,
                ),
                reverb = ReverbSettings(preset = ReverbPreset.Hall),
            )
        )

        assertEquals(normalized.profile.graphicEqualizer.bandGainsDb, normalized.eqBandGainsDb)
        assertEquals(175, normalized.eqQHundredths)
        assertEquals(ReverbPreset.Hall, normalized.reverbPreset)
    }
}

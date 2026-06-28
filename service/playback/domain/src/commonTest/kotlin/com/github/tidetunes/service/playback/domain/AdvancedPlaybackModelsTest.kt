package com.github.tidetunes.service.playback.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AdvancedPlaybackModelsTest {
    @Test
    fun defaultEnhancementSettingsDisableAdvancedProcessing() {
        val settings = PlaybackEnhancementSettings.Default

        assertFalse(settings.gaplessEnabled)
        assertFalse(settings.crossfadeEnabled)
        assertEquals(ReplayGainMode.Off, settings.replayGainMode)
        assertEquals(0f, settings.replayGainPreampDb)
    }

    @Test
    fun crossfadeIsEnabledWhenDurationIsPositive() {
        val settings = PlaybackEnhancementSettings(crossfadeDurationMs = 5_000)

        assertTrue(settings.crossfadeEnabled)
    }

    @Test
    fun rejectsInvalidCrossfadeDuration() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackEnhancementSettings(crossfadeDurationMs = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackEnhancementSettings(crossfadeDurationMs = 30_001)
        }
    }

    @Test
    fun rejectsInvalidReplayGainPreamp() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackEnhancementSettings(replayGainPreampDb = -20.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            PlaybackEnhancementSettings(replayGainPreampDb = 20.1f)
        }
    }

    @Test
    fun capabilitiesReportSupportedFeatures() {
        val capabilities = PlaybackEngineCapabilities(
            supportedFeatures = setOf(
                PlaybackFeature.GaplessPlayback,
                PlaybackFeature.ReplayGain,
            )
        )

        assertTrue(capabilities.supports(PlaybackFeature.GaplessPlayback))
        assertTrue(capabilities.supports(PlaybackFeature.ReplayGain))
        assertFalse(capabilities.supports(PlaybackFeature.Crossfade))
    }

    @Test
    fun outputStateFindsSelectedDevice() {
        val builtIn = AudioOutputDevice(
            id = AudioOutputDeviceId("built-in"),
            name = "Built-in Output",
            type = AudioOutputDeviceType.BuiltIn,
        )
        val bluetooth = AudioOutputDevice(
            id = AudioOutputDeviceId("bt-headphones"),
            name = "Headphones",
            type = AudioOutputDeviceType.Bluetooth,
        )

        val state = AudioOutputState(
            devices = listOf(builtIn, bluetooth),
            selectedDeviceId = bluetooth.id,
        )

        assertSame(bluetooth, state.selectedDevice)
    }

    @Test
    fun emptyOutputStateHasNoSelectedDevice() {
        assertNull(AudioOutputState.Empty.selectedDevice)
    }

    @Test
    fun rejectsBlankOutputDeviceValues() {
        assertFailsWith<IllegalArgumentException> {
            AudioOutputDeviceId("")
        }
        assertFailsWith<IllegalArgumentException> {
            AudioOutputDevice(id = AudioOutputDeviceId("id"), name = "")
        }
    }
}

package io.github.julystar.musicapp.service.playback.data

import io.github.julystar.musicapp.service.playback.domain.AudioOutputDevice
import io.github.julystar.musicapp.service.playback.domain.AudioOutputDeviceId
import io.github.julystar.musicapp.service.playback.domain.AudioOutputDeviceType
import io.github.julystar.musicapp.service.playback.domain.AudioOutputController
import io.github.julystar.musicapp.service.playback.domain.AudioOutputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.sound.sampled.AudioSystem

/**
 * Desktop implementation of [AudioOutputController] using the Java Sound API.
 *
 * Enumerates audio mixers available on the system and reports them as
 * [AudioOutputDevice] entries. When no device is explicitly selected,
 * the system default output is used.
 *
 * Currently read-only — device selection via rodio/cpal requires bridging
 * output-device selection through the Rust FFI layer.
 */
class DesktopAudioOutputController : AudioOutputController {
    private val _outputState = MutableStateFlow(scanDevices())

    override val outputState: StateFlow<AudioOutputState>
        get() = _outputState.asStateFlow()

    override fun selectOutputDevice(deviceId: AudioOutputDeviceId?) {
        // Device selection through rodio/cpal is not yet bridged to Kotlin.
        // The Rust DesktopRodioPlayer would need a setAudioDevice(deviceId: String) method.
        // For now, update the selected device in state to reflect intent.
        _outputState.value = _outputState.value.copy(selectedDeviceId = deviceId)
    }

    fun refreshDevices() {
        _outputState.value = scanDevices()
    }

    companion object {
        private fun scanDevices(): AudioOutputState {
            return try {
                val mixers = AudioSystem.getMixerInfo()
                val devices = mixers.mapIndexed { index, mixerInfo ->
                    val name = mixerInfo.name.trim()
                    val type = classifyMixer(name, mixerInfo.description)
                    AudioOutputDevice(
                        id = AudioOutputDeviceId("desktop:${index}"),
                        name = if (name.isNotBlank()) name else "Audio Device $index",
                        type = type,
                        isSystemDefault = index == 0,
                    )
                }
                AudioOutputState(
                    devices = devices,
                    selectedDeviceId = null, // system default
                )
            } catch (_: Exception) {
                // Fallback: no audio devices detected
                AudioOutputState.Empty
            }
        }

        private fun classifyMixer(name: String, description: String): AudioOutputDeviceType {
            val lower = "${name} ${description}".lowercase()
            return when {
                lower.contains("bluetooth") || lower.contains("bt") -> AudioOutputDeviceType.Bluetooth
                lower.contains("usb") -> AudioOutputDeviceType.Usb
                lower.contains("hdmi") || lower.contains("displayport") -> AudioOutputDeviceType.Hdmi
                lower.contains("airplay") -> AudioOutputDeviceType.AirPlay
                lower.contains("network") || lower.contains("dlna") || lower.contains("chromecast")
                    -> AudioOutputDeviceType.Network
                lower.contains("built-in") || lower.contains("speaker") || lower.contains("headphone")
                    || lower.contains("default") -> AudioOutputDeviceType.BuiltIn
                else -> AudioOutputDeviceType.Unknown
            }
        }
    }
}

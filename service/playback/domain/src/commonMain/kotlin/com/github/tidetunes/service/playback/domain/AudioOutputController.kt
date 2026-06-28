package com.github.tidetunes.service.playback.domain

import kotlinx.coroutines.flow.StateFlow

interface AudioOutputController {
    val outputState: StateFlow<AudioOutputState>

    fun selectOutputDevice(deviceId: AudioOutputDeviceId?)
}

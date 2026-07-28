package io.github.julystar.musicapp.service.playback.domain

import kotlinx.coroutines.flow.StateFlow

interface SleepController {
    val sleepState: StateFlow<SleepModeState>
    fun scheduleSleep(newExpiredMs: Long)
    fun cancelSleep()
}

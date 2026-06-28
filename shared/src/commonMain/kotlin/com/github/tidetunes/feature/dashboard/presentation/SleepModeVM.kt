package com.github.tidetunes.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import com.github.tidetunes.service.playback.domain.SleepController
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepModeVM constructor(
    private val sleepController: SleepController,
    private val currentTimeMs: () -> Long = { 0L },
) : ViewModel() {
    private val _modalOpen = MutableStateFlow(false)
    private val _editLeftTime = MutableStateFlow(SleepModeLeftTime(0))

    val state = sleepController.sleepState

    val modalOpen = _modalOpen.asStateFlow()
    val editLeftTime = _editLeftTime.asStateFlow()

    fun openModal(leftTime: SleepModeLeftTime) {
        _editLeftTime.value = leftTime
        _modalOpen.value = true
    }

    fun openModal() {
        openModal(SleepModeLeftTime(state.value.expiredMs - currentTimeMs()))
    }

    fun closeModal() {
        _modalOpen.value = false
    }

    fun remove() {
        sleepController.cancelSleep()
    }

    fun set(hour: Int, minute: Int) {
        val newExpiredMs = currentTimeMs() +
            hour.toLong() * 3600_000 +
            minute.toLong() * 60_000

        sleepController.scheduleSleep(newExpiredMs)
    }
}

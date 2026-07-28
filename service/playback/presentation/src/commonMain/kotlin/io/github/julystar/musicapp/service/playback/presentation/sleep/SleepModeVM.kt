package io.github.julystar.musicapp.service.playback.presentation.sleep

import androidx.lifecycle.ViewModel
import io.github.julystar.musicapp.service.playback.domain.SleepController
import io.github.julystar.musicapp.service.playback.domain.SleepModeLeftTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class SleepModeVM constructor(
    private val sleepController: SleepController,
    private val currentTimeMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
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

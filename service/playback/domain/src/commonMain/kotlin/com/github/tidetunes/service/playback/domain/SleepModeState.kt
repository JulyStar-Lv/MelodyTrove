package com.github.tidetunes.service.playback.domain

data class SleepModeState(
    val enabled: Boolean = false,
    val expiredMs: Long = 0,
)

data class SleepModeLeftTime(val remainingMs: Long) {
    val hour: Int
        get() = (remainingMs.coerceAtLeast(0L) / (1000 * 60 * 60)).toInt()
    val minute: Int
        get() = ((remainingMs.coerceAtLeast(0L) / (1000 * 60)) % 60).toInt()
}

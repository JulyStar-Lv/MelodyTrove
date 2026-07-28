package io.github.julystar.musicapp.core.utils

import kotlin.time.Duration

fun formatDuration(duration: Duration?): String {
    if (duration == null) {
        return "--:--:--"
    }

    val all = duration.inWholeMilliseconds
    val h = all / 1000 / 60 / 60
    val m = all / 1000 / 60 % 60
    val s = all / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

fun toMusicDurationMs(duration: Duration): ULong {
    return duration.inWholeMilliseconds.toULong()
}

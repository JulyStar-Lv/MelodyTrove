package com.github.tidetunes.core.utils

import uniffi.tidetunes_core.Music
import kotlin.time.Duration


fun formatDuration(duration: Duration?): String {
    if (duration != null) {
        val all = duration.inWholeMilliseconds
        val h = all / 1000 / 60 / 60
        val m = all / 1000 / 60 % 60
        val s = all / 1000 % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        return "--:--:--"
    }
}

fun formatDuration(music: Music?): String {
    return formatDuration(music?.meta?.duration)
}

fun toMusicDurationMs(music: Music?): ULong {
    return music?.meta?.duration?.inWholeMilliseconds?.toULong() ?: 0uL
}

fun toMusicDurationMs(duration: Duration): ULong {
    return duration.inWholeMilliseconds.toULong()
}

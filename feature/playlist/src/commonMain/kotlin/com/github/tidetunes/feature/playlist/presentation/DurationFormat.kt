package com.github.tidetunes.feature.playlist.presentation

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

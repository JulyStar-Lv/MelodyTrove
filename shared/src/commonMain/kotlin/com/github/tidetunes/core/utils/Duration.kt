package com.github.tidetunes.core.utils

import uniffi.tidetunes_core.Music

fun formatDuration(music: Music?): String {
    return formatDuration(music?.meta?.duration)
}

fun toMusicDurationMs(music: Music?): ULong {
    return music?.meta?.duration?.inWholeMilliseconds?.toULong() ?: 0uL
}

package io.github.julystar.musicapp.core.utils

import uniffi.app_backend.Music

fun formatDuration(music: Music?): String {
    return formatDuration(music?.meta?.duration)
}

fun toMusicDurationMs(music: Music?): ULong {
    return music?.meta?.duration?.inWholeMilliseconds?.toULong() ?: 0uL
}

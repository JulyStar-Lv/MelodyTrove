package io.github.julystar.musicapp.feature.library.presentation

/**
 * CommonMain-safe formatter for the minute/second template used by LibraryDesignScreen.
 * Avoids java.util.Formatter on iOS and native targets.
 */
internal fun String.format(minutes: Long, seconds: Long): String {
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

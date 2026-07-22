package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

@Immutable
data class LyricsState(
    val trackId: Long? = null,
    val isLoading: Boolean = true,
    val trackTitle: String = "",
    val trackArtist: String? = null,
    val lines: ImmutableList<String> = persistentListOf(),
    val format: String? = null,
    val synchronized: Boolean = false,
    val error: String? = null,
    val wordTimedLines: ImmutableList<WordTimedLyricLine> = persistentListOf(),
)

@Immutable
data class WordTimedLyricLine(
    val duration: Duration,
    val text: String,
    val words: ImmutableList<WordTimedToken> = persistentListOf(),
)

@Immutable
data class WordTimedToken(
    val text: String,
    val startOffset: Duration,
    val duration: Duration,
)

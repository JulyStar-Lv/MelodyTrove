package com.github.tidetunes.feature.lyrics.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class LyricsState(
    val isLoading: Boolean = true,
    val trackTitle: String = "",
    val trackArtist: String? = null,
    val lines: ImmutableList<String> = persistentListOf(),
    val format: String? = null,
    val synchronized: Boolean = false,
    val error: String? = null,
)

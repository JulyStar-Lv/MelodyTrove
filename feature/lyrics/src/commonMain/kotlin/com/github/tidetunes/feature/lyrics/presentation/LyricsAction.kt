package com.github.tidetunes.feature.lyrics.presentation

sealed interface LyricsAction {
    data object NavigateBack : LyricsAction
    data object Retry : LyricsAction
}

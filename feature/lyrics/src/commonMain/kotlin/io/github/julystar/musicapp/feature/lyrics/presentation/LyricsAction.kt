package io.github.julystar.musicapp.feature.lyrics.presentation

sealed interface LyricsAction {
    data object NavigateBack : LyricsAction
    data object Retry : LyricsAction
}

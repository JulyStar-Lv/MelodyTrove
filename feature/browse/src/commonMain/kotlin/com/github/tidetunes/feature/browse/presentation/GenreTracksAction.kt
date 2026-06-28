package com.github.tidetunes.feature.browse.presentation

sealed interface GenreTracksAction {
    data object NavigateBack : GenreTracksAction
    data object Retry : GenreTracksAction
    data object PlayAll : GenreTracksAction
    data class PlayTrack(val trackId: Long) : GenreTracksAction
    data class DownloadTrack(val track: GenreTrackItem) : GenreTracksAction
}

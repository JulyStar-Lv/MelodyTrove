package com.github.tidetunes.feature.artist.presentation

sealed interface ArtistAction {
    data object NavigateBack : ArtistAction
    data object Retry : ArtistAction
    data object PlayAll : ArtistAction
    data class PlayTrack(val trackId: Long) : ArtistAction
    data class NavigateToAlbum(val albumId: Long) : ArtistAction
    data class DownloadTrack(val track: ArtistTrackItem) : ArtistAction
}

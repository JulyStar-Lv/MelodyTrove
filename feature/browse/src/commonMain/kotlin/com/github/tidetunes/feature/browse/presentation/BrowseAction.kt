package com.github.tidetunes.feature.browse.presentation

sealed interface BrowseAction {
    data object NavigateBack : BrowseAction
    data object Retry : BrowseAction
    data class NavigateToAlbum(val albumId: Long) : BrowseAction
    data class NavigateToArtist(val artistId: Long) : BrowseAction
    data class NavigateToGenre(val genre: String) : BrowseAction
}

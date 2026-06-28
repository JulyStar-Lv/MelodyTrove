package com.github.tidetunes.feature.artist.presentation

sealed interface ArtistEvent {
    data class ShowMessage(val message: String) : ArtistEvent
}

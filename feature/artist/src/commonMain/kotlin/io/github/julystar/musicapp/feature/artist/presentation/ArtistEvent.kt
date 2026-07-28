package io.github.julystar.musicapp.feature.artist.presentation

sealed interface ArtistEvent {
    data class ShowMessage(val message: String) : ArtistEvent
}

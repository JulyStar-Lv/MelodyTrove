package io.github.julystar.musicapp.feature.album.presentation

sealed interface AlbumEvent {
    data class ShowMessage(val message: String) : AlbumEvent
}

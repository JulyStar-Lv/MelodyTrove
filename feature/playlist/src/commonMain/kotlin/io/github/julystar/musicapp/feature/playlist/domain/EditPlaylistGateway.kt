package io.github.julystar.musicapp.feature.playlist.domain

import io.github.julystar.musicapp.source.api.SourceNodeSelection

data class PlaylistMetaToEdit(
    val title: String,
    val coverSelection: SourceNodeSelection?,
)

interface EditPlaylistGateway {
    fun getPlaylistMetaToEdit(id: Long): PlaylistMetaToEdit?

    fun updatePlaylist(
        id: Long,
        title: String,
        cover: SourceNodeSelection?,
    )
}

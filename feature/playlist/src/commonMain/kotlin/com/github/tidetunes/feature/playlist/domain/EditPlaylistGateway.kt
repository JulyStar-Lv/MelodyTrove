package com.github.tidetunes.feature.playlist.domain

import com.github.tidetunes.source.api.SourceNodeSelection

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

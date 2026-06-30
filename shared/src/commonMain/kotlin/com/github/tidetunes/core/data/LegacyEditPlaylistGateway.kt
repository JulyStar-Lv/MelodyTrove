package com.github.tidetunes.core.data

import com.github.tidetunes.feature.playlist.domain.EditPlaylistGateway
import com.github.tidetunes.feature.playlist.domain.PlaylistMetaToEdit
import com.github.tidetunes.source.api.SourceNodeSelection

class LegacyEditPlaylistGateway(
    private val playlistRepository: PlaylistRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
) : EditPlaylistGateway {
    override fun getPlaylistMetaToEdit(id: Long): PlaylistMetaToEdit? {
        return playlistRepository.playlists.value
            .find { item -> item.meta.id.value == id }
            ?.let { item ->
                PlaylistMetaToEdit(
                    title = item.meta.title,
                    coverSelection = item.meta.cover?.toSourceNodeSelection(
                        storageRepository.storages.value,
                    ),
                )
            }
    }

    override fun updatePlaylist(
        id: Long,
        title: String,
        cover: SourceNodeSelection?,
    ) {
        playlistRepository.editPlaylist(
            UpdatePlaylistRequest(
                id = id,
                title = title,
                cover = cover,
            ),
        )
    }
}

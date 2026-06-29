package com.github.tidetunes.core.data

import com.github.tidetunes.singleton.RoomLibraryStore
import com.github.tidetunes.source.api.PlaylistImportTarget
import com.github.tidetunes.source.api.SourceNodeSelection

class PlaylistImportTargetImpl(
    private val roomLibraryStore: RoomLibraryStore,
) : PlaylistImportTarget {
    override suspend fun createPlaylistFromSelections(
        title: String,
        cover: SourceNodeSelection?,
        entries: List<SourceNodeSelection>,
    ) {
        roomLibraryStore.createPlaylist(
            CreatePlaylistRequest(
                title = title,
                cover = cover,
                entries = entries,
            )
        )
    }

    override suspend fun addMusicSelectionsToPlaylist(
        playlistId: Long,
        selections: List<SourceNodeSelection>,
    ): List<Long> {
        return roomLibraryStore.addMusicSelectionsById(playlistId, selections)
    }
}

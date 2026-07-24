package com.github.tidetunes.feature.library.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import com.github.tidetunes.core.domain.model.PlaylistSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class LibraryState(
    val tracks: ImmutableList<LibraryTrackItem> = persistentListOf(),
    val albums: ImmutableList<LibraryAlbumItem> = persistentListOf(),
    val artists: ImmutableList<LibraryArtistItem> = persistentListOf(),
    val playlists: ImmutableList<PlaylistSummary> = persistentListOf(),
)

sealed interface LibraryAction {
    data object Refresh : LibraryAction
    data class PlayTrack(val trackId: Long) : LibraryAction
    data class DownloadTrack(val track: LibraryTrackItem) : LibraryAction
}

sealed interface LibraryEvent {
    data class ShowMessage(val message: String) : LibraryEvent
}

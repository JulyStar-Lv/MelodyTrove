package io.github.julystar.musicapp.feature.library.presentation

import androidx.compose.runtime.Composable

/**
 * Compatibility entry point retained for existing navigation wiring.
 * The production screen delegates to the fully localized library implementation.
 */
@Composable
fun LibraryDesignScreen(
    state: LibraryState,
    currentPlayingTrackId: Long? = null,
    onNavigateToLibraryFolderImport: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onAction: (LibraryAction) -> Unit,
) {
    LibraryScreen(
        state = state,
        currentPlayingTrackId = currentPlayingTrackId,
        onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
        onNavigateToAlbum = onNavigateToAlbum,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToPlaylist = onNavigateToPlaylist,
        onNavigateToPlaylists = onNavigateToPlaylists,
        onAction = onAction,
    )
}

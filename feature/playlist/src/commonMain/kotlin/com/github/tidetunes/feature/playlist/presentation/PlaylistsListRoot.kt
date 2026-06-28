package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistsListRoot(
    onNavigateToPlaylist: (Long) -> Unit,
    onCreatePlaylist: () -> Unit = {},
    playlistsViewModel: PlaylistsVM = koinViewModel(),
) {
    val state by playlistsViewModel.state.collectAsState()

    PlaylistsListScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is PlaylistsListAction.NavigateToPlaylist -> onNavigateToPlaylist(action.id)
                PlaylistsListAction.CreatePlaylist -> onCreatePlaylist()
                else -> playlistsViewModel.onAction(action)
            }
        },
    )
}

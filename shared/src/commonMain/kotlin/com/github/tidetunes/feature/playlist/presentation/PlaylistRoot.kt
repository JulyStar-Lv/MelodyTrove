package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistVM
import com.github.tidetunes.viewmodels.PlayerVM
import com.github.tidetunes.viewmodels.PlaylistVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistRoot(
    scaffoldPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    playlistViewModel: PlaylistVM = koinViewModel(),
    playerViewModel: PlayerVM = koinViewModel(),
    editPlaylistViewModel: EditPlaylistVM = koinViewModel(),
) {
    val state by playlistViewModel.state.collectAsState()
    val currentPlaying by playerViewModel.music.collectAsState()

    LaunchedEffect(playlistViewModel) {
        playlistViewModel.events.collect { event ->
            when (event) {
                is PlaylistEvent.ShowMessage -> Unit
            }
        }
    }

    PlaylistScreen(
        state = state,
        currentPlayingTrackId = currentPlaying?.meta?.id?.value,
        scaffoldPadding = scaffoldPadding,
        onAction = { action ->
            when (action) {
                PlaylistAction.NavigateBack -> onNavigateBack()
                PlaylistAction.EditPlaylist -> editPlaylistViewModel.openModal()
                PlaylistAction.ImportTracks -> {
                    playlistViewModel.onAction(action)
                    onNavigateToImport()
                }
                PlaylistAction.ConfirmRemovePlaylist -> {
                    playlistViewModel.onAction(action)
                    onNavigateBack()
                }
                PlaylistAction.PlayAll -> {
                    val track = state.tracks.firstOrNull()
                    if (track != null) {
                        playerViewModel.play(track.id, state.playlistId)
                        onNavigateToPlayer()
                    }
                }
                is PlaylistAction.PlayTrack -> {
                    playerViewModel.play(action.trackId, state.playlistId)
                    onNavigateToPlayer()
                }
                else -> playlistViewModel.onAction(action)
            }
        },
    )
}

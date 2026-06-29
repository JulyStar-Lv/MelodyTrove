package com.github.tidetunes.feature.artist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistRoot(
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (albumId: Long) -> Unit,
    viewModel: ArtistViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ArtistEvent.ShowMessage -> Unit
            }
        }
    }

    ArtistScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ArtistAction.NavigateBack -> onNavigateBack()
                ArtistAction.Retry -> viewModel.onAction(action)
                ArtistAction.PlayAll -> {
                    val items = state.tracks.map { track ->
                        PlayableItem(
                            title = track.title,
                            libraryTrackId = track.id,
                        )
                    }
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(items = items)
                    }
                }
                is ArtistAction.PlayTrack -> {
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(
                            items = listOf(
                                PlayableItem(
                                    title = "Track ${action.trackId}",
                                    libraryTrackId = action.trackId,
                                )
                            )
                        )
                    }
                }
                is ArtistAction.NavigateToAlbum -> onNavigateToAlbum(action.albumId)
                is ArtistAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

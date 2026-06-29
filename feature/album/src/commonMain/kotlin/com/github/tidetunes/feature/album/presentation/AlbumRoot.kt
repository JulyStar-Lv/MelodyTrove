package com.github.tidetunes.feature.album.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlbumRoot(
    onNavigateBack: () -> Unit,
    viewModel: AlbumViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AlbumEvent.ShowMessage -> Unit
            }
        }
    }

    AlbumScreen(
        state = state,
        onAction = { action ->
            when (action) {
                AlbumAction.NavigateBack -> onNavigateBack()
                AlbumAction.Retry -> viewModel.onAction(action)
                AlbumAction.PlayAll -> {
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
                is AlbumAction.PlayTrack -> {
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
                is AlbumAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

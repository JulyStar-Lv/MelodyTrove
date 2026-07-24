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
                    val items = state.tracks.map { it.toPlayableItem(state.artist) }
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(items = items)
                    }
                }
                is AlbumAction.PlayTrack -> {
                    val items = state.tracks.map { it.toPlayableItem(state.artist) }
                    val startIndex = state.tracks.indexOfFirst { it.id == action.trackId }
                    if (startIndex >= 0) {
                        kotlinx.coroutines.runBlocking {
                            playbackController.play(items = items, startIndex = startIndex)
                        }
                    }
                }
                is AlbumAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

private fun AlbumTrackItem.toPlayableItem(albumArtist: String): PlayableItem = PlayableItem(
    mediaId = mediaId,
    title = title,
    artist = albumArtist.ifBlank { null },
    durationMs = durationMs,
    libraryTrackId = id,
)

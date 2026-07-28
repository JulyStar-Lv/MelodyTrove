package io.github.julystar.musicapp.feature.artist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
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
                    val items = state.tracks.map { it.toPlayableItem(state.name) }
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(items = items)
                    }
                }
                is ArtistAction.PlayTrack -> {
                    val items = state.tracks.map { it.toPlayableItem(state.name) }
                    val startIndex = state.tracks.indexOfFirst { it.id == action.trackId }
                    if (startIndex >= 0) {
                        kotlinx.coroutines.runBlocking {
                            playbackController.play(items = items, startIndex = startIndex)
                        }
                    }
                }
                is ArtistAction.NavigateToAlbum -> onNavigateToAlbum(action.albumId)
                is ArtistAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

private fun ArtistTrackItem.toPlayableItem(artistName: String): PlayableItem = PlayableItem(
    mediaId = mediaId,
    title = title,
    artist = artistName.ifBlank { null },
    durationMs = durationMs,
    libraryTrackId = id,
)

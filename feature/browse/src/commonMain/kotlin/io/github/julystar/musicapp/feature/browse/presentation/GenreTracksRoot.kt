package io.github.julystar.musicapp.feature.browse.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GenreTracksRoot(
    onNavigateBack: () -> Unit,
    viewModel: GenreTracksViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GenreTracksEvent.ShowMessage -> Unit
            }
        }
    }

    GenreTracksScreen(
        state = state,
        onAction = { action ->
            when (action) {
                GenreTracksAction.NavigateBack -> onNavigateBack()
                GenreTracksAction.Retry -> viewModel.onAction(action)
                GenreTracksAction.PlayAll -> {
                    val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                    kotlinx.coroutines.runBlocking { playbackController.play(items = items) }
                }
                is GenreTracksAction.PlayTrack -> {
                    kotlinx.coroutines.runBlocking {
                        val startIndex = state.tracks.indexOfFirst { it.id == action.trackId }.coerceAtLeast(0)
                        val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                        playbackController.play(items = items, startIndex = startIndex)
                    }
                }
                is GenreTracksAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

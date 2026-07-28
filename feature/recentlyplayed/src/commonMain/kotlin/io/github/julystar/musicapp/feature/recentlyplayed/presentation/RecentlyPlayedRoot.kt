package io.github.julystar.musicapp.feature.recentlyplayed.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentlyPlayedRoot(
    onNavigateBack: () -> Unit,
    viewModel: RecentlyPlayedViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecentlyPlayedEvent.ShowMessage -> Unit
            }
        }
    }

    RecentlyPlayedScreen(
        state = state,
        onAction = { action ->
            when (action) {
                RecentlyPlayedAction.NavigateBack -> onNavigateBack()
                RecentlyPlayedAction.Retry -> viewModel.onAction(action)
                RecentlyPlayedAction.PlayAll -> {
                    val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                    kotlinx.coroutines.runBlocking { playbackController.play(items = items) }
                }
                is RecentlyPlayedAction.PlayTrack -> {
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(items = listOf(PlayableItem(title = "Track ${action.trackId}", libraryTrackId = action.trackId)))
                    }
                }
                is RecentlyPlayedAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

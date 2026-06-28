package com.github.tidetunes.feature.recentlyadded.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentlyAddedRoot(
    onNavigateBack: () -> Unit,
    viewModel: RecentlyAddedViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecentlyAddedEvent.ShowMessage -> Unit
            }
        }
    }

    RecentlyAddedScreen(
        state = state,
        onAction = { action ->
            when (action) {
                RecentlyAddedAction.NavigateBack -> onNavigateBack()
                RecentlyAddedAction.Retry -> viewModel.onAction(action)
                RecentlyAddedAction.PlayAll -> {
                    val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                    kotlinx.coroutines.runBlocking { playbackController.play(items = items) }
                }
                is RecentlyAddedAction.PlayTrack -> {
                    kotlinx.coroutines.runBlocking {
                        playbackController.play(items = listOf(PlayableItem(title = "Track ${action.trackId}", libraryTrackId = action.trackId)))
                    }
                }
                is RecentlyAddedAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

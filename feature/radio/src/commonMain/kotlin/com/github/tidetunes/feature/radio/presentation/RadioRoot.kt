package com.github.tidetunes.feature.radio.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RadioRoot(
    onNavigateBack: () -> Unit,
    viewModel: RadioViewModel = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RadioEvent.ShowMessage -> Unit
            }
        }
    }

    RadioScreen(
        state = state,
        onAction = { action ->
            when (action) {
                RadioAction.NavigateBack -> onNavigateBack()
                RadioAction.Refresh -> viewModel.onAction(action)
                RadioAction.PlayAll -> {
                    val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                    kotlinx.coroutines.runBlocking { playbackController.play(items = items) }
                }
                is RadioAction.PlayTrack -> {
                    kotlinx.coroutines.runBlocking {
                        val startIndex = state.tracks.indexOfFirst { it.id == action.trackId }.coerceAtLeast(0)
                        val items = state.tracks.map { t -> PlayableItem(title = t.title, libraryTrackId = t.id) }
                        playbackController.play(items = items, startIndex = startIndex)
                    }
                }
                is RadioAction.DownloadTrack -> viewModel.onAction(action)
            }
        },
    )
}

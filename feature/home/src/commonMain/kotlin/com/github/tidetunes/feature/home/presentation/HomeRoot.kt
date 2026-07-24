package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.github.tidetunes.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    scaffoldPadding: PaddingValues,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val playbackController = koinInject<PlaybackController>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToDownloads -> onNavigateToDownloads()
                HomeEvent.NavigateToLibrary -> onNavigateToLibrary()
                HomeEvent.NavigateToSearch -> onNavigateToSearch()
                HomeEvent.OpenSleepTimer -> onOpenSleepTimer()
            }
        }
    }

    HomeDesignScreen(
        scaffoldPadding = scaffoldPadding,
        state = state,
        onAction = { action ->
            if (action is HomeAction.PlayTrack) {
                val startIndex = state.recentTracks.indexOfFirst { it.id == action.trackId }
                if (startIndex >= 0) {
                    coroutineScope.launch {
                        playbackController.play(
                            items = state.recentTracks.map { it.toPlayableItem() },
                            startIndex = startIndex,
                        )
                    }
                }
            } else {
                viewModel.onAction(action)
            }
        },
    )
}

private fun HomeRecentTrack.toPlayableItem(): PlayableItem = PlayableItem(
    mediaId = mediaId,
    title = title,
    artist = subtitle.ifBlank { null },
    durationMs = durationMs,
    libraryTrackId = id,
    libraryPlaylistId = LIBRARY_PLAYBACK_PLAYLIST_ID,
)

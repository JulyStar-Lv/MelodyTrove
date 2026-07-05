package com.github.tidetunes.feature.library.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.github.tidetunes.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryRoot(
    onOpenNowPlaying: () -> Unit = {},
    viewModel: LibraryVM = koinViewModel(),
) {
    val playbackController = koinInject<PlaybackController>()
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val playerState by playbackController.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ShowMessage -> Unit
            }
        }
    }

    LibraryScreen(
        state = state,
        currentPlayingTrackId = playerState.currentItem?.libraryTrackId,
        onAction = { action ->
            when (action) {
                is LibraryAction.PlayTrack -> {
                    val items = state.tracks.map { track -> track.toPlayableItem() }
                    val startIndex = state.tracks.indexOfFirst { track -> track.id == action.trackId }
                    if (items.isNotEmpty() && startIndex >= 0) {
                        coroutineScope.launch {
                            playbackController.play(
                                items = items,
                                startIndex = startIndex,
                            )
                        }
                        onOpenNowPlaying()
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
    )
}

private fun LibraryTrackItem.toPlayableItem(): PlayableItem {
    return PlayableItem(
        mediaId = mediaId,
        title = title,
        artist = artist,
        durationMs = durationMs,
        libraryTrackId = id,
        libraryPlaylistId = LIBRARY_PLAYBACK_PLAYLIST_ID,
    )
}

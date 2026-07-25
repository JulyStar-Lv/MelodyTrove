package com.github.tidetunes.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.tidetunes.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import com.github.tidetunes.feature.search.domain.SearchTrackItem
import com.github.tidetunes.service.playback.domain.PlayableItem
import com.github.tidetunes.service.playback.domain.PlaybackController
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchRoot(
    viewModel: SearchViewModel = koinViewModel(),
    onNavigateToAlbum: (albumId: Long) -> Unit = {},
    onNavigateToArtist: (artistId: Long) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val playbackController = koinInject<PlaybackController>()

    LaunchedEffect(viewModel, playbackController, onNavigateToAlbum, onNavigateToArtist) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.OpenTrack -> playbackController.play(
                    items = listOf(event.track.toPlayableItem()),
                )
                is SearchEvent.NavigateToAlbum -> onNavigateToAlbum(event.albumId)
                is SearchEvent.NavigateToArtist -> onNavigateToArtist(event.artistId)
                is SearchEvent.ShowMessage -> Unit
            }
        }
    }

    SearchDesignScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

internal fun SearchTrackItem.toPlayableItem(): PlayableItem = PlayableItem(
    mediaId = mediaId,
    title = title,
    artist = artist,
    durationMs = durationMs,
    libraryTrackId = id,
    libraryPlaylistId = id?.let { LIBRARY_PLAYBACK_PLAYLIST_ID },
)

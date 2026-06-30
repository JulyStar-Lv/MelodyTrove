package com.github.tidetunes.feature.browse.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BrowseRoot(
    onNavigateToAlbum: (albumId: Long) -> Unit,
    onNavigateToArtist: (artistId: Long) -> Unit,
    onNavigateToGenre: (genre: String) -> Unit,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    BrowseScreen(
        state = state,
        onAction = { action ->
            when (action) {
                BrowseAction.NavigateBack -> Unit
                BrowseAction.Retry -> viewModel.onAction(action)
                is BrowseAction.NavigateToAlbum -> onNavigateToAlbum(action.albumId)
                is BrowseAction.NavigateToArtist -> onNavigateToArtist(action.artistId)
                is BrowseAction.NavigateToGenre -> onNavigateToGenre(action.genre)
            }
        },
    )
}

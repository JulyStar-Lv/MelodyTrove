package io.github.julystar.musicapp.feature.browse.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.browse.presentation.BrowseRoot
import io.github.julystar.musicapp.feature.browse.presentation.GenreTracksRoot

fun NavGraphBuilder.browseGraph(
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (albumId: Long) -> Unit,
    onNavigateToArtist: (artistId: Long) -> Unit,
    onNavigateToGenre: (genre: String) -> Unit,
) {
    composable<MusicGraph.Browse> {
        BrowseRoot(
            onNavigateToAlbum = onNavigateToAlbum,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToGenre = onNavigateToGenre,
        )
    }
    composable<MusicGraph.BrowseGenre> {
        GenreTracksRoot(
            onNavigateBack = onNavigateBack,
        )
    }
}

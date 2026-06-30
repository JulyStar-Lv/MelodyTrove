package com.github.tidetunes.feature.browse.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.browse.presentation.BrowseRoot
import com.github.tidetunes.feature.browse.presentation.GenreTracksRoot

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

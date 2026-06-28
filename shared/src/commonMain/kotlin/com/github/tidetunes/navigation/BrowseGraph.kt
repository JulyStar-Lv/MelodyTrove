package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.tidetunes.feature.browse.presentation.BrowseRoot
import com.github.tidetunes.feature.browse.presentation.GenreTracksRoot

fun NavGraphBuilder.browseGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Browse> {
        BrowseRoot(
            onNavigateToAlbum = { albumId ->
                navController.navigate(MusicGraph.Album(id = albumId))
            },
            onNavigateToArtist = { artistId ->
                navController.navigate(MusicGraph.Artist(id = artistId))
            },
            onNavigateToGenre = { genre ->
                navController.navigate(MusicGraph.BrowseGenre(genre = genre))
            },
        )
    }
    composable<MusicGraph.BrowseGenre> { backStackEntry ->
        GenreTracksRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

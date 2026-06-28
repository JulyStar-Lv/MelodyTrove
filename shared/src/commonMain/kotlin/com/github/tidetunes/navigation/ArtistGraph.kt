package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.tidetunes.feature.artist.presentation.ArtistRoot

fun NavGraphBuilder.artistGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Artist> { backStackEntry ->
        val route: MusicGraph.Artist = backStackEntry.toRoute()
        ArtistRoot(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToAlbum = { albumId ->
                navController.navigate(MusicGraph.Album(id = albumId))
            },
        )
    }
}

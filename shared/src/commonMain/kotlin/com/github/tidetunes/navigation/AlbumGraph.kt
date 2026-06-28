package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.tidetunes.feature.album.presentation.AlbumRoot

fun NavGraphBuilder.albumGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Album> { backStackEntry ->
        val route: MusicGraph.Album = backStackEntry.toRoute()
        AlbumRoot(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}

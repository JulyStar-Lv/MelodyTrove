package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.tidetunes.feature.lyrics.presentation.LyricsRoot

fun NavGraphBuilder.lyricsGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Lyrics> { backStackEntry ->
        LyricsRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

package com.github.tidetunes.feature.lyrics.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.lyrics.presentation.LyricsRoot

fun NavGraphBuilder.lyricsGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Lyrics> {
        LyricsRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

package io.github.julystar.musicapp.feature.lyrics.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.lyrics.presentation.LyricsRoot

fun NavGraphBuilder.lyricsGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Lyrics> {
        LyricsRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

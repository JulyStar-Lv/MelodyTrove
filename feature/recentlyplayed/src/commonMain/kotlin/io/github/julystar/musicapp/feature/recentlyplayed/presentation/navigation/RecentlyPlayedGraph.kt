package io.github.julystar.musicapp.feature.recentlyplayed.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.recentlyplayed.presentation.RecentlyPlayedRoot

fun NavGraphBuilder.recentlyPlayedGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.RecentlyPlayed> {
        RecentlyPlayedRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

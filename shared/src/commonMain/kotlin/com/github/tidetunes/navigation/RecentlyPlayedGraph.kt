package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.recentlyplayed.presentation.RecentlyPlayedRoot

fun NavGraphBuilder.recentlyPlayedGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.RecentlyPlayed> {
        RecentlyPlayedRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

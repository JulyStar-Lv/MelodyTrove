package com.github.tidetunes.feature.recentlyplayed.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

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

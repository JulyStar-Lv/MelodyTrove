package com.github.tidetunes.feature.recentlyadded.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.recentlyadded.presentation.RecentlyAddedRoot

fun NavGraphBuilder.recentlyAddedGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.RecentlyAdded> {
        RecentlyAddedRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

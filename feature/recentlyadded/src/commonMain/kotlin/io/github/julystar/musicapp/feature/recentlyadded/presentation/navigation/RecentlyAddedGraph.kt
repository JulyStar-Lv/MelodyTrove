package io.github.julystar.musicapp.feature.recentlyadded.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.recentlyadded.presentation.RecentlyAddedRoot

fun NavGraphBuilder.recentlyAddedGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.RecentlyAdded> {
        RecentlyAddedRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

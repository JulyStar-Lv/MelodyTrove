package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.queue.presentation.QueueRoot

fun NavGraphBuilder.queueGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Queue> {
        QueueRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

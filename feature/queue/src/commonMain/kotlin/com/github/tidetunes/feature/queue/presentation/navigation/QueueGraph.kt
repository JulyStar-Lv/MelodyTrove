package com.github.tidetunes.feature.queue.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

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

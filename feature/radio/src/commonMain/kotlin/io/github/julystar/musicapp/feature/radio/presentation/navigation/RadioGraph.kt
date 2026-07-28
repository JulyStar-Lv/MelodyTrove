package io.github.julystar.musicapp.feature.radio.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.radio.presentation.RadioRoot

fun NavGraphBuilder.radioGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Radio> {
        RadioRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

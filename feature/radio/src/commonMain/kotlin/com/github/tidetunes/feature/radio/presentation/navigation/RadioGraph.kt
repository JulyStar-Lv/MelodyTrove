package com.github.tidetunes.feature.radio.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.radio.presentation.RadioRoot

fun NavGraphBuilder.radioGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Radio> {
        RadioRoot(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}

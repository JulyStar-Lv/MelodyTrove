package com.github.tidetunes.navigation

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

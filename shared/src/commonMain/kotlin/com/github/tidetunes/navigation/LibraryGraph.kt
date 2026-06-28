package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.importing.presentation.ImportRoot

fun NavGraphBuilder.libraryGraph(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
) {
    composable<MusicGraph.Import> {
        ImportRoot(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}

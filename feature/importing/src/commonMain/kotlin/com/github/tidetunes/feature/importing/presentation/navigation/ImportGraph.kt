package com.github.tidetunes.feature.importing.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.feature.importing.presentation.ImportRoot

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.importGraph(
    onNavigateBack: () -> Unit,
) {
    composable<MusicGraph.Import> {
        ImportRoot(
            onNavigateBack = onNavigateBack,
        )
    }
}

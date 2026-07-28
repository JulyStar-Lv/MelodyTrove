package io.github.julystar.musicapp.feature.importing.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph
import io.github.julystar.musicapp.feature.importing.presentation.ImportRoot

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

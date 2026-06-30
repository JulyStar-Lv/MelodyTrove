package com.github.tidetunes.feature.sources.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.feature.sources.presentation.SourceEditorRoot

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.sourcesGraph(
    onNavigateBack: () -> Unit,
    onNavigateToLibraryFolderImport: () -> Unit,
) {
    composable<MusicGraph.EditStorage> {
        SourceEditorRoot(
            onNavigateBack = onNavigateBack,
            onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
        )
    }
}

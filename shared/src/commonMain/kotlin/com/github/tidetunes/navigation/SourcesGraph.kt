package com.github.tidetunes.navigation

import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.importing.presentation.ImportRoot
import com.github.tidetunes.feature.sources.presentation.SourceEditorRoot
import com.github.tidetunes.feature.importing.data.RouteImportType

fun NavGraphBuilder.sourcesGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.EditStorage> {
        SourceEditorRoot(
            onNavigateBack = {
                navController.navigateUp()
            },
            onNavigateToLibraryFolderImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.LibraryFolder))
            },
        )
    }
    composable<MusicGraph.Import> {
        ImportRoot(
            onNavigateBack = {
                navController.navigateUp()
            },
        )
    }
}

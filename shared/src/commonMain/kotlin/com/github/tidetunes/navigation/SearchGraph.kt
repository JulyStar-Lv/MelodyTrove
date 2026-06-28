package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.search.presentation.SearchRoot

fun NavGraphBuilder.searchGraph() {
    composable<MusicGraph.Search> {
        SearchRoot()
    }
}

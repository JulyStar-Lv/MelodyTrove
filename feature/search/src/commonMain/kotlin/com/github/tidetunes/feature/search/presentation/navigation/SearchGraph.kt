package com.github.tidetunes.feature.search.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.search.presentation.SearchRoot

fun NavGraphBuilder.searchGraph() {
    composable<MusicGraph.Search> {
        SearchRoot()
    }
}

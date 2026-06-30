package com.github.tidetunes.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.homeGraph(
    scaffoldPadding: PaddingValues,
) {
    composable<MusicGraph.Home> {
        HomePage(scaffoldPadding = scaffoldPadding)
    }
}

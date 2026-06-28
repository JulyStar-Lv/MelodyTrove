package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.widgets.home.HomePage

fun NavGraphBuilder.homeGraph(
    scaffoldPadding: PaddingValues,
) {
    composable<MusicGraph.Home> {
        HomePage(scaffoldPadding = scaffoldPadding)
    }
}

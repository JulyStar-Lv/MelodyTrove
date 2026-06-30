package com.github.tidetunes.feature.settings.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.settings.presentation.DebugRoot
import com.github.tidetunes.feature.settings.presentation.LogRoot

fun NavGraphBuilder.settingsGraph() {
    composable<MusicGraph.Log> {
        LogRoot()
    }
    composable<MusicGraph.DebugMore> {
        DebugRoot()
    }
}

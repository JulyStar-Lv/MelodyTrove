package com.github.tidetunes.navigation

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

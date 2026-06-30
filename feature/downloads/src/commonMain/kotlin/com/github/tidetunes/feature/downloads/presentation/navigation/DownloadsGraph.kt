package com.github.tidetunes.feature.downloads.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.downloads.presentation.DownloadsRoot

fun NavGraphBuilder.downloadsGraph() {
    composable<MusicGraph.Downloads> {
        DownloadsRoot()
    }
}

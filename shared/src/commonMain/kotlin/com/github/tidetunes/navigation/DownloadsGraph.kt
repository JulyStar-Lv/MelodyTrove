package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.downloads.presentation.DownloadsRoot

fun NavGraphBuilder.downloadsGraph() {
    composable<MusicGraph.Downloads> {
        DownloadsRoot()
    }
}

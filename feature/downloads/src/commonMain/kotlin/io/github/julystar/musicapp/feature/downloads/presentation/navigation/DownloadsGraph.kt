package io.github.julystar.musicapp.feature.downloads.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.downloads.presentation.DownloadsRoot

fun NavGraphBuilder.downloadsGraph() {
    composable<MusicGraph.Downloads> {
        DownloadsRoot()
    }
}

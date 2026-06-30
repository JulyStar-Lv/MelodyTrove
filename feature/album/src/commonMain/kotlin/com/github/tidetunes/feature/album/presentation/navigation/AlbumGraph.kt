package com.github.tidetunes.feature.album.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.album.presentation.AlbumRoot

fun NavGraphBuilder.albumGraph(
    onNavigateBack: () -> Unit,
) {
    composable<MusicGraph.Album> {
        AlbumRoot(
            onNavigateBack = onNavigateBack,
        )
    }
}

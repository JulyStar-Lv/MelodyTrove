package com.github.tidetunes.feature.artist.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.artist.presentation.ArtistRoot

fun NavGraphBuilder.artistGraph(
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (albumId: Long) -> Unit,
) {
    composable<MusicGraph.Artist> {
        ArtistRoot(
            onNavigateBack = onNavigateBack,
            onNavigateToAlbum = onNavigateToAlbum,
        )
    }
}

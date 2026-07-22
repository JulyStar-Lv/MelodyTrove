package com.github.tidetunes.service.playback.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingRoot
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.service.playback.presentation.sleep.TimeToPauseModal

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.playerGraph(
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: (Long) -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToLyricImport: () -> Unit,
    onSearchMetadata: (NowPlayingTrackItem) -> Unit,
) {
    composable<MusicGraph.NowPlaying> {
        NowPlayingRoot(
            onNavigateBack = onNavigateBack,
            onNavigateToLyrics = onNavigateToLyrics,
            onNavigateToQueue = onNavigateToQueue,
            onNavigateToLyricImport = onNavigateToLyricImport,
            onSearchMetadata = onSearchMetadata,
        )
        TimeToPauseModal()
    }
}

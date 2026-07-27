package com.github.tidetunes.service.playback.presentation.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingRoot
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.service.playback.presentation.sleep.TimeToPauseModal
import com.github.tidetunes.service.playback.presentation.transition.LocalPlayerArtworkAnimatedVisibilityScope

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.playerGraph(
    onNavigateBack: () -> Unit,
    onNavigateToLyrics: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onNavigateToLyricImport: () -> Unit,
    onSearchMetadata: (NowPlayingTrackItem) -> Unit,
) {
    composable<MusicGraph.NowPlaying> {
        val animatedVisibilityScope = this
        CompositionLocalProvider(
            LocalPlayerArtworkAnimatedVisibilityScope provides animatedVisibilityScope,
        ) {
            NowPlayingRoot(
                onNavigateBack = onNavigateBack,
                onNavigateToLyrics = onNavigateToLyrics,
                onOpenQueue = onOpenQueue,
                onNavigateToLyricImport = onNavigateToLyricImport,
                onSearchMetadata = onSearchMetadata,
            )
            TimeToPauseModal()
        }
    }
}

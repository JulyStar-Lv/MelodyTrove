package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingRoot
import com.github.tidetunes.feature.importing.data.RouteImportType
import com.github.tidetunes.feature.dashboard.presentation.TimeToPauseModal

fun NavGraphBuilder.playerGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.NowPlaying> {
        NowPlayingRoot(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToLyricImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.Lyric))
            },
        )
        TimeToPauseModal()
    }
}

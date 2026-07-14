package com.github.tidetunes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.feature.album.presentation.navigation.albumGraph
import com.github.tidetunes.feature.artist.presentation.navigation.artistGraph
import com.github.tidetunes.feature.browse.presentation.navigation.browseGraph
import com.github.tidetunes.feature.downloads.presentation.navigation.downloadsGraph
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.feature.importing.presentation.navigation.importGraph
import com.github.tidetunes.feature.lyrics.presentation.navigation.lyricsGraph
import com.github.tidetunes.feature.queue.presentation.navigation.queueGraph
import com.github.tidetunes.feature.radio.presentation.navigation.radioGraph
import com.github.tidetunes.feature.recentlyadded.presentation.navigation.recentlyAddedGraph
import com.github.tidetunes.feature.recentlyplayed.presentation.navigation.recentlyPlayedGraph
import com.github.tidetunes.feature.search.presentation.navigation.searchGraph
import com.github.tidetunes.feature.sources.presentation.navigation.sourcesGraph
import com.github.tidetunes.plugin.management.PluginSettingsRoot
import com.github.tidetunes.service.playback.presentation.navigation.playerGraph

@Composable
internal fun RootNavHost(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
) {
    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = MusicGraph.Home,
        enterTransition = {
            slideIn(
                animationSpec = tween(300),
                initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
            )
        },
        exitTransition = {
            slideOut(
                animationSpec = tween(300),
                targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
            )
        },
        popEnterTransition = {
            slideIn(
                animationSpec = tween(300),
                initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
            )
        },
        popExitTransition = {
            slideOut(
                animationSpec = tween(300),
                targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
            )
        },
    ) {
        homeGraph(scaffoldPadding)
        albumGraph(
            onNavigateBack = { navController.popBackStack() },
        )
        artistGraph(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAlbum = { albumId ->
                navController.navigate(MusicGraph.Album(id = albumId))
            },
        )
        browseGraph(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAlbum = { albumId ->
                navController.navigate(MusicGraph.Album(id = albumId))
            },
            onNavigateToArtist = { artistId ->
                navController.navigate(MusicGraph.Artist(id = artistId))
            },
            onNavigateToGenre = { genre ->
                navController.navigate(MusicGraph.BrowseGenre(genre = genre))
            },
        )
        radioGraph(navController)
        recentlyAddedGraph(navController)
        recentlyPlayedGraph(navController)
        lyricsGraph(navController)
        queueGraph(navController)
        sourcesGraph(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToLibraryFolderImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.LibraryFolder))
            },
        )
        importGraph(
            onNavigateBack = { navController.popBackStack() },
        )
        composable<MusicGraph.PluginSettings> {
            PluginSettingsRoot(onBack = { navController.popBackStack() })
        }
        searchGraph()
        downloadsGraph()
        playerGraph(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLyricImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.Lyric))
            },
        )
    }
}

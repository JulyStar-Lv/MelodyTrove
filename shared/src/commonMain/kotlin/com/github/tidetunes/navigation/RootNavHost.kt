package com.github.tidetunes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.isRouteNowPlaying
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.album.presentation.navigation.albumGraph
import com.github.tidetunes.feature.artist.presentation.navigation.artistGraph
import com.github.tidetunes.feature.browse.presentation.navigation.browseGraph
import com.github.tidetunes.feature.downloads.presentation.navigation.downloadsGraph
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.feature.importing.presentation.navigation.importGraph
import com.github.tidetunes.feature.lyrics.presentation.navigation.lyricsGraph
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListRoot
import com.github.tidetunes.feature.queue.presentation.navigation.queueGraph
import com.github.tidetunes.feature.radio.presentation.navigation.radioGraph
import com.github.tidetunes.feature.recentlyadded.presentation.navigation.recentlyAddedGraph
import com.github.tidetunes.feature.recentlyplayed.presentation.navigation.recentlyPlayedGraph
import com.github.tidetunes.feature.search.presentation.navigation.searchGraph
import com.github.tidetunes.feature.sources.presentation.navigation.sourcesGraph
import com.github.tidetunes.plugin.management.PluginSettingsRoot
import com.github.tidetunes.plugin.management.ManualMetadataSearchDialog
import com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingTrackItem
import com.github.tidetunes.service.playback.presentation.navigation.playerGraph
import com.github.tidetunes.service.playback.presentation.shell.PlaybackMiniPlayerHost
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun RootNavHost(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
) {
    var metadataTrack by remember { mutableStateOf<NowPlayingTrackItem?>(null) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showSecondaryMiniPlayer = shouldShowPersistentMiniPlayer(currentRoute)
    val playerSpacing = TideTunesTokens.spacing
    val secondaryMiniPlayerReservedHeight = TideTunesTokens.player.miniBarHeight + playerSpacing.md

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showSecondaryMiniPlayer) secondaryMiniPlayerReservedHeight else 0.dp),
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
        composable<MusicGraph.Playlists> {
            val createPlaylistVM: CreatePlaylistVM = koinViewModel()
            PlaylistsListRoot(
                onNavigateToPlaylist = { id ->
                    navController.navigate(MusicGraph.Playlist(id))
                },
                onCreatePlaylist = createPlaylistVM::openModal,
            )
            CreatePlaylistRoot(
                createPlaylistVM = createPlaylistVM,
                onNavigateToImport = {
                    navController.navigate(MusicGraph.Import(RouteImportType.EditPlaylist))
                },
                onNavigateToCoverImport = {
                    navController.navigate(MusicGraph.Import(RouteImportType.EditPlaylistCover))
                },
            )
        }
        composable<MusicGraph.Playlist> {
            PlaylistRoot(
                scaffoldPadding = scaffoldPadding,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToImport = {
                    navController.navigate(MusicGraph.Import(RouteImportType.Music))
                },
            )
            EditPlaylistRoot(
                onNavigateToCoverImport = {
                    navController.navigate(MusicGraph.Import(RouteImportType.EditPlaylistCover))
                },
            )
        }
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
            onNavigateToLyrics = { trackId -> navController.navigate(MusicGraph.Lyrics(trackId)) },
            onNavigateToQueue = { navController.navigate(MusicGraph.Queue) },
            onNavigateToLyricImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.Lyric))
            },
            onSearchMetadata = { track -> metadataTrack = track },
        )
        }
        if (showSecondaryMiniPlayer) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = playerSpacing.sm,
                        top = playerSpacing.xs,
                        end = playerSpacing.sm,
                        bottom = playerSpacing.xs + scaffoldPadding.calculateBottomPadding(),
                    ),
            ) {
                PlaybackMiniPlayerHost(
                    onOpenNowPlaying = { navController.navigate(MusicGraph.NowPlaying) },
                    onBrowseLibrary = { navController.popBackStack() },
                )
            }
        }
    }
    ManualMetadataSearchDialog(
        track = metadataTrack,
        onDismiss = { metadataTrack = null },
    )
}

internal fun shouldShowPersistentMiniPlayer(route: String?): Boolean =
    !isRouteHome(route) && !isRouteNowPlaying(route)

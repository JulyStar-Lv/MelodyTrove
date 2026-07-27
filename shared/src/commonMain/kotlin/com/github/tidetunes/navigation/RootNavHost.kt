package com.github.tidetunes.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.isRouteNowPlaying
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.presentation.components.TideGlassOverlayScene
import com.github.tidetunes.core.presentation.components.getBottomBarSpace
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.feature.album.presentation.navigation.albumGraph
import com.github.tidetunes.feature.artist.presentation.navigation.artistGraph
import com.github.tidetunes.feature.browse.presentation.navigation.browseGraph
import com.github.tidetunes.feature.downloads.presentation.navigation.downloadsGraph
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.feature.importing.presentation.navigation.importGraph
import com.github.tidetunes.feature.home.presentation.ListeningRoot
import com.github.tidetunes.feature.lyrics.presentation.navigation.lyricsGraph
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListRoot
import com.github.tidetunes.feature.queue.presentation.QueueRoot
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
import com.github.tidetunes.service.playback.presentation.transition.LocalPlayerArtworkSharedTransitionScope
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RootNavHost(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
) {
    var metadataTrack by remember { mutableStateOf<NowPlayingTrackItem?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var selectedRootTabName by rememberSaveable { mutableStateOf(HomeTab.HOME.name) }
    val selectedRootTab = HomeTab.entries.firstOrNull { it.name == selectedRootTabName } ?: HomeTab.HOME
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showSecondaryMiniPlayer = shouldShowPersistentMiniPlayer(currentRoute)
    val onRootTabSelected: (HomeTab) -> Unit = { selectedRootTabName = it.name }
    val playerTransitionDurationMillis = TideTunesTokens.motion.playerExpandMillis

    val navigationContent: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = MusicGraph.Home,
            enterTransition = {
                if (isRouteNowPlaying(targetState.destination.route)) {
                    immediateEnterTransition(playerTransitionDurationMillis)
                } else {
                    slideIn(
                        animationSpec = tween(300),
                        initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
                    )
                }
            },
            exitTransition = {
                if (isRouteNowPlaying(targetState.destination.route)) {
                    immediateExitTransition(playerTransitionDurationMillis)
                } else {
                    slideOut(
                        animationSpec = tween(300),
                        targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
                    )
                }
            },
            popEnterTransition = {
                if (isRouteNowPlaying(initialState.destination.route)) {
                    immediateEnterTransition(playerTransitionDurationMillis)
                } else {
                    slideIn(
                        animationSpec = tween(300),
                        initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
                    )
                }
            },
            popExitTransition = {
                if (isRouteNowPlaying(initialState.destination.route)) {
                    immediateExitTransition(playerTransitionDurationMillis)
                } else {
                    slideOut(
                        animationSpec = tween(300),
                        targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
                    )
                }
            },
        ) {
        homeGraph(
            scaffoldPadding = scaffoldPadding,
            currentTab = selectedRootTab,
            onTabSelected = onRootTabSelected,
            onOpenQueue = { showQueue = true },
        )
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
        composable<MusicGraph.Listening> {
            ListeningRoot(onNavigateBack = { navController.popBackStack() })
        }
        lyricsGraph(navController)
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
        searchGraph(navController)
        downloadsGraph()
        playerGraph(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLyrics = { trackId -> navController.navigate(MusicGraph.Lyrics(trackId)) },
            onOpenQueue = { showQueue = true },
            onNavigateToLyricImport = {
                navController.navigate(MusicGraph.Import(RouteImportType.Lyric))
            },
            onSearchMetadata = { track -> metadataTrack = track },
        )
        }
    }
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sharedTransitionScope = this
        CompositionLocalProvider(
            LocalPlayerArtworkSharedTransitionScope provides sharedTransitionScope,
        ) {
            if (showSecondaryMiniPlayer) {
                SecondaryRootNavigationLayout(
                    currentTab = selectedRootTab,
                    onTabSelected = { tab ->
                        onRootTabSelected(tab)
                        if (!navController.popBackStack<MusicGraph.Home>(inclusive = false)) {
                            navController.navigate(MusicGraph.Home)
                        }
                    },
                    scaffoldPadding = scaffoldPadding,
                    onOpenNowPlaying = { navController.navigate(MusicGraph.NowPlaying) },
                    onOpenQueue = { showQueue = true },
                    onBrowseLibrary = {
                        onRootTabSelected(HomeTab.LIBRARY)
                        if (!navController.popBackStack<MusicGraph.Home>(inclusive = false)) {
                            navController.navigate(MusicGraph.Home)
                        }
                    },
                    content = navigationContent,
                )
            } else {
                navigationContent(Modifier.fillMaxSize())
            }
            ManualMetadataSearchDialog(
                track = metadataTrack,
                onDismiss = { metadataTrack = null },
            )
            QueueRoot(
                show = showQueue,
                onDismiss = { showQueue = false },
            )
        }
    }
}

private fun immediateEnterTransition(durationMillis: Int) = fadeIn(
    initialAlpha = 0f,
    animationSpec = keyframes {
        this.durationMillis = durationMillis
        1f at 1
    },
)

private fun immediateExitTransition(durationMillis: Int) = fadeOut(
    targetAlpha = 0f,
    animationSpec = keyframes {
        this.durationMillis = durationMillis
        0f at 1
    },
)

internal fun shouldShowPersistentMiniPlayer(route: String?): Boolean =
    !isRouteHome(route) && !isRouteNowPlaying(route)

@Composable
private fun SecondaryRootNavigationLayout(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    scaffoldPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    onBrowseLibrary: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)
    val themeScope = rememberCoroutineScope()
    val miniPlayerContent: @Composable () -> Unit = {
        PlaybackMiniPlayerHost(
            onOpenNowPlaying = onOpenNowPlaying,
            onBrowseLibrary = onBrowseLibrary,
            onOpenQueue = onOpenQueue,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        val windowSizeClass = rememberWindowSizeClass(
            containerSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight),
        )
        when (windowSizeClass) {
                WindowSizeClass.Compact -> {
                    TideGlassOverlayScene(
                        modifier = Modifier.fillMaxSize(),
                        contentBottomInset = getBottomBarSpace(true, scaffoldPadding),
                        backdropContent = {
                            content(
                                Modifier
                                    .fillMaxSize()
                            )
                        },
                        overlayContent = {
                            BottomBar(
                                currentTab = currentTab,
                                onTabSelected = onTabSelected,
                                miniPlayerContent = miniPlayerContent,
                                showMiniPlayer = true,
                                showChrome = true,
                                scaffoldPadding = scaffoldPadding,
                            )
                        },
                    )
                }

                WindowSizeClass.Medium -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRailBar(
                            currentTab = currentTab,
                            onTabSelected = onTabSelected,
                            modifier = Modifier.fillMaxHeight(),
                            windowSizeClass = windowSizeClass,
                        )
                        RootContentPane(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            showMiniPlayer = true,
                            miniPlayerContent = miniPlayerContent,
                        ) {
                            content(Modifier.fillMaxSize())
                        }
                    }
                }

                WindowSizeClass.Expanded,
                WindowSizeClass.Large,
                WindowSizeClass.XL -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        SidebarBar(
                            currentTab = currentTab,
                            onTabSelected = onTabSelected,
                            isDark = settings.themeMode != AppThemeMode.Light,
                            onToggleTheme = {
                                themeScope.launch {
                                    settingsRepository.setThemeMode(
                                        if (settings.themeMode == AppThemeMode.Dark) {
                                            AppThemeMode.Light
                                        } else {
                                            AppThemeMode.Dark
                                        },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxHeight(),
                            windowSizeClass = windowSizeClass,
                        )
                        RootContentPane(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            showMiniPlayer = true,
                            miniPlayerContent = miniPlayerContent,
                        ) {
                            content(Modifier.fillMaxSize())
                        }
                    }
                }
        }
    }
}

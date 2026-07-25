package com.github.tidetunes.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.isRouteNowPlaying
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.presentation.components.TideGlassScene
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
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun RootNavHost(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
) {
    var metadataTrack by remember { mutableStateOf<NowPlayingTrackItem?>(null) }
    var selectedRootTabName by rememberSaveable { mutableStateOf(HomeTab.HOME.name) }
    val selectedRootTab = HomeTab.entries.firstOrNull { it.name == selectedRootTabName } ?: HomeTab.HOME
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showSecondaryMiniPlayer = shouldShowPersistentMiniPlayer(currentRoute)
    val onRootTabSelected: (HomeTab) -> Unit = { selectedRootTabName = it.name }
    val density = LocalDensity.current
    val motion = TideTunesTokens.motion
    val spacing = TideTunesTokens.spacing
    val player = TideTunesTokens.player
    val navigation = TideTunesTokens.navigation
    val adaptive = TideTunesTokens.adaptive
    val collapseDurationMillis = motion.playerExpandMillis
    val collapseFadeDurationMillis = motion.instantMillis
    val collapseFadeDelayMillis = collapseDurationMillis - collapseFadeDurationMillis
    val compactMaxWidthPx = with(density) { adaptive.compactMaxWidth.roundToPx() }
    val expandedMaxWidthPx = with(density) { adaptive.expandedMaxWidth.roundToPx() }
    val railWidthPx = with(density) { adaptive.railWidth.roundToPx() }
    val sidebarWidthPx = with(density) { adaptive.sidebarWidth.roundToPx() }
    val miniPlayerArtworkCenterPx = with(density) {
        (spacing.sm + spacing.sm + 44.dp / 2).roundToPx()
    }
    val miniPlayerCenterFromBottomPx = with(density) {
        (spacing.xs + player.miniBarHeight / 2).roundToPx()
    }
    val compactMiniPlayerCenterFromBottomPx = with(density) {
        (
            scaffoldPadding.calculateBottomPadding() +
                navigation.compactBarHeight +
                spacing.xs +
                player.miniBarHeight / 2
        ).roundToPx()
    }
    val playerCollapseEasing = CubicBezierEasing(0.32f, 0f, 0.15f, 1f)

    val navigationContent: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            modifier = modifier,
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
                if (isRouteNowPlaying(initialState.destination.route)) {
                    fadeIn(
                        initialAlpha = 0.92f,
                        animationSpec = tween(durationMillis = motion.fastMillis),
                    )
                } else {
                    slideIn(
                        animationSpec = tween(300),
                        initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
                    )
                }
            },
            popExitTransition = {
                if (isRouteNowPlaying(initialState.destination.route)) {
                    val returningToHome = isRouteHome(targetState.destination.route)
                    val collapseAnimationSpec = tween<IntOffset>(
                        durationMillis = collapseDurationMillis,
                        easing = playerCollapseEasing,
                    )
                    scaleOut(
                        targetScale = 0.14f,
                        transformOrigin = TransformOrigin.Center,
                        animationSpec = tween(
                            durationMillis = collapseDurationMillis,
                            easing = playerCollapseEasing,
                        ),
                    ) + slideOut(
                        animationSpec = collapseAnimationSpec,
                        targetOffset = { fullSize ->
                            val leadingChromeWidth = when {
                                !returningToHome -> 0
                                fullSize.width <= compactMaxWidthPx -> 0
                                fullSize.width <= expandedMaxWidthPx -> railWidthPx
                                else -> sidebarWidthPx
                            }
                            val targetCenterY = when {
                                !returningToHome -> {
                                    fullSize.height + miniPlayerCenterFromBottomPx
                                }
                                leadingChromeWidth == 0 -> {
                                    fullSize.height - compactMiniPlayerCenterFromBottomPx
                                }
                                else -> {
                                    fullSize.height - miniPlayerCenterFromBottomPx
                                }
                            }
                            IntOffset(
                                x = leadingChromeWidth + miniPlayerArtworkCenterPx - fullSize.width / 2,
                                y = targetCenterY - fullSize.height / 2,
                            )
                        },
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = collapseFadeDurationMillis,
                            delayMillis = collapseFadeDelayMillis,
                        ),
                    )
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
        searchGraph(navController)
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
    }
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
}

internal fun shouldShowPersistentMiniPlayer(route: String?): Boolean =
    !isRouteHome(route) && !isRouteNowPlaying(route)

@Composable
private fun SecondaryRootNavigationLayout(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    scaffoldPadding: PaddingValues,
    onOpenNowPlaying: () -> Unit,
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
        )
    }

    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowSizeClass = rememberWindowSizeClass(
                containerSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight),
            )
            when (windowSizeClass) {
                WindowSizeClass.Compact -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content(
                            Modifier
                                .fillMaxSize()
                                .padding(bottom = getBottomBarSpace(true, scaffoldPadding)),
                        )
                        BottomBar(
                            currentTab = currentTab,
                            onTabSelected = onTabSelected,
                            miniPlayerContent = miniPlayerContent,
                            showMiniPlayer = true,
                            showChrome = true,
                            scaffoldPadding = scaffoldPadding,
                        )
                    }
                }

                WindowSizeClass.Medium,
                WindowSizeClass.Expanded -> {
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
}

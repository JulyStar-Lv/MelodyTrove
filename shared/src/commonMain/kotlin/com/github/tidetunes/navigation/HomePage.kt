package com.github.tidetunes.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.isRouteNowPlaying
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.core.presentation.components.TideGlassOverlayScene
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.components.TideStickyHeaderState
import com.github.tidetunes.core.presentation.components.getBottomBarSpace
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.service.playback.presentation.shell.PlaybackMiniPlayerHost
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomePage(
    scaffoldPadding: PaddingValues,
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onOpenQueue: () -> Unit,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)
    val themeScope = rememberCoroutineScope()
    val globalNavController = LocalNavController.current
    val currentRootBackStackEntry by globalNavController.currentBackStackEntryAsState()
    val currentRootRoute = currentRootBackStackEntry?.destination?.route.orEmpty()
    val showHomeChrome = isRouteHome(currentRootRoute) || isRouteNowPlaying(currentRootRoute)
    val onOpenNowPlaying = {
        globalNavController.navigate(MusicGraph.NowPlaying)
    }
    val onNavigateToDownloads = {
        globalNavController.navigate(MusicGraph.Downloads)
    }
    val onNavigateToLibraryFolderImport = {
        globalNavController.navigate(MusicGraph.Import(RouteImportType.LibraryFolder))
    }
    val onNavigateToAlbum = { id: Long ->
        globalNavController.navigate(MusicGraph.Album(id))
    }
    val onNavigateToArtist = { id: Long ->
        globalNavController.navigate(MusicGraph.Artist(id))
    }
    val onNavigateToPlaylist = { id: Long ->
        globalNavController.navigate(MusicGraph.Playlist(id))
    }
    val onNavigateToPlaylists = {
        globalNavController.navigate(MusicGraph.Playlists)
    }
    val miniPlayerContent: @Composable () -> Unit = {
        PlaybackMiniPlayerHost(
            onOpenNowPlaying = onOpenNowPlaying,
            onBrowseLibrary = { onTabSelected(HomeTab.LIBRARY) },
            onOpenQueue = onOpenQueue,
        )
    }

    val libraryNavController = rememberNavController()
    val searchNavController = rememberNavController()
    val settingsNavController = rememberNavController()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val windowSizeClass = rememberWindowSizeClass(
            containerSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight),
        )
        val statusBarInset = WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
        var stickyHeaderState by remember(currentTab) {
            mutableStateOf<TideStickyHeaderState?>(null)
        }
        val stickyHeaderStateSink = remember(currentTab) {
            { state: TideStickyHeaderState? -> stickyHeaderState = state }
        }

        val tabContent: @Composable (
            HomeTab,
            PaddingValues,
            ((TideStickyHeaderState?) -> Unit)?,
        ) -> Unit = { tab, contentPadding, stickyHeaderSink ->
            HomeTabContent(
                currentTab = tab,
                libraryNavController = libraryNavController,
                searchNavController = searchNavController,
                settingsNavController = settingsNavController,
                scaffoldPadding = contentPadding,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToLibrary = { onTabSelected(HomeTab.LIBRARY) },
                onNavigateToSearch = { onTabSelected(HomeTab.SEARCH) },
                onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                onNavigateToPlaylist = onNavigateToPlaylist,
                onNavigateToPlaylists = onNavigateToPlaylists,
                stickyHeaderStateSink = stickyHeaderSink,
            )
        }

        when (windowSizeClass) {
            WindowSizeClass.Compact -> {
                TideGlassOverlayScene(
                    modifier = Modifier.fillMaxSize(),
                    contentBottomInset = getBottomBarSpace(showHomeChrome, scaffoldPadding),
                    backdropContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MiuixTheme.colorScheme.background)
                                .statusBarsPadding(),
                        ) {
                            tabContent(
                                currentTab,
                                PaddingValues(
                                    bottom = getBottomBarSpace(showHomeChrome, scaffoldPadding),
                                ),
                                stickyHeaderStateSink,
                            )
                        }
                    },
                    overlayContent = {
                        stickyHeaderState?.let { state ->
                            TideStickyGlassActionBar(
                                title = state.title,
                                subtitle = state.subtitle,
                                collapseFraction = state.collapseFraction,
                                statusBarInset = statusBarInset,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                        BottomBar(
                            currentTab = currentTab,
                            onTabSelected = onTabSelected,
                            miniPlayerContent = miniPlayerContent,
                            showMiniPlayer = showHomeChrome,
                            showChrome = showHomeChrome,
                            scaffoldPadding = scaffoldPadding,
                        )
                    },
                )
            }
            WindowSizeClass.Medium -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
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
                        showMiniPlayer = showHomeChrome,
                        miniPlayerContent = miniPlayerContent,
                    ) {
                        tabContent(currentTab, scaffoldPadding, null)
                    }
                }
            }
            WindowSizeClass.Expanded,
            WindowSizeClass.Large,
            WindowSizeClass.XL -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
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
                        showMiniPlayer = showHomeChrome,
                        miniPlayerContent = miniPlayerContent,
                    ) {
                        tabContent(currentTab, scaffoldPadding, null)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RootContentPane(
    showMiniPlayer: Boolean,
    miniPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!showMiniPlayer) {
        Box(modifier = modifier) { content() }
        return
    }

    TideGlassOverlayScene(
        modifier = modifier,
        backdropContent = { content() },
        overlayContent = {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                miniPlayerContent()
            }
        },
    )
}

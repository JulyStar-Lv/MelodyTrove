package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.service.playback.presentation.shell.PlaybackMiniPlayerHost
import com.github.tidetunes.service.playback.presentation.shell.rememberIsPlaybackPlaying
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import com.github.tidetunes.widgets.appbar.getBottomBarSpace
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomePage(
    scaffoldPadding: PaddingValues,
) {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(AppSettings.Default)
    val themeScope = rememberCoroutineScope()
    val globalNavController = LocalNavController.current
    val currentRootBackStackEntry by globalNavController.currentBackStackEntryAsState()
    val currentRootRoute = currentRootBackStackEntry?.destination?.route.orEmpty()
    val showHomeChrome = isRouteHome(currentRootRoute)
    val isPlaybackPlaying = rememberIsPlaybackPlaying()
    val onOpenNowPlaying = {
        globalNavController.navigate(MusicGraph.NowPlaying)
    }
    val onNavigateToDownloads = {
        globalNavController.navigate(MusicGraph.Downloads)
    }
    val onNavigateToLibraryFolderImport = {
        globalNavController.navigate(MusicGraph.Import(RouteImportType.LibraryFolder))
    }
    var wasPlaybackPlaying by remember { mutableStateOf(isPlaybackPlaying) }
    LaunchedEffect(isPlaybackPlaying, settings.playerInteraction.openPlayerOnPlay) {
        if (
            settings.playerInteraction.openPlayerOnPlay &&
            isPlaybackPlaying &&
            !wasPlaybackPlaying &&
            showHomeChrome
        ) {
            onOpenNowPlaying()
        }
        wasPlaybackPlaying = isPlaybackPlaying
    }
    var currentTab by remember { mutableStateOf(HomeTab.HOME) }
    val miniPlayerContent: @Composable () -> Unit = {
        PlaybackMiniPlayerHost(
            onOpenNowPlaying = onOpenNowPlaying,
            onBrowseLibrary = { currentTab = HomeTab.LIBRARY },
        )
    }

    val libraryNavController = rememberNavController()
    val searchNavController = rememberNavController()
    val settingsNavController = rememberNavController()

    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowSizeClass = rememberWindowSizeClass(
                containerSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight),
            )

        val tabContent: @Composable (HomeTab) -> Unit = { tab ->
            HomeTabContent(
                currentTab = tab,
                libraryNavController = libraryNavController,
                searchNavController = searchNavController,
                settingsNavController = settingsNavController,
                scaffoldPadding = scaffoldPadding,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToLibrary = { currentTab = HomeTab.LIBRARY },
                onNavigateToSearch = { currentTab = HomeTab.SEARCH },
                onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                onOpenNowPlaying = onOpenNowPlaying,
            )
        }

            when (windowSizeClass) {
            WindowSizeClass.Compact -> {
                Box(
                    modifier = Modifier.padding(
                        bottom = getBottomBarSpace(showHomeChrome, scaffoldPadding),
                    ),
                ) {
                    tabContent(currentTab)
                }
                BottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    miniPlayerContent = miniPlayerContent,
                    showMiniPlayer = showHomeChrome,
                    showChrome = showHomeChrome,
                    scaffoldPadding = scaffoldPadding,
                )
            }
            WindowSizeClass.Medium,
            WindowSizeClass.Expanded -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRailBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        modifier = Modifier.fillMaxHeight(),
                        windowSizeClass = windowSizeClass,
                    )
                    HomeMainPane(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showMiniPlayer = showHomeChrome,
                        miniPlayerContent = miniPlayerContent,
                    ) {
                        tabContent(currentTab)
                    }
                }
            }
            WindowSizeClass.Large,
            WindowSizeClass.XL -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    SidebarBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
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
                    HomeMainPane(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        showMiniPlayer = showHomeChrome,
                        miniPlayerContent = miniPlayerContent,
                    ) {
                        tabContent(currentTab)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun HomeMainPane(
    showMiniPlayer: Boolean,
    miniPlayerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (showMiniPlayer) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                miniPlayerContent()
            }
        }
    }
}

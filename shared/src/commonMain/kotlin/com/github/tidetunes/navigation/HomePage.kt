package com.github.tidetunes.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.tidetunes.core.isRouteHome
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.service.playback.presentation.shell.PlaybackMiniPlayerHost
import com.github.tidetunes.service.playback.presentation.shell.rememberIsPlaybackPlaying
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import com.github.tidetunes.widgets.appbar.getBottomBarSpace

@Composable
fun HomePage(
    scaffoldPadding: PaddingValues,
) {
    val globalNavController = LocalNavController.current
    val currentRootBackStackEntry by globalNavController.currentBackStackEntryAsState()
    val currentRootRoute = currentRootBackStackEntry?.destination?.route.orEmpty()
    val showHomeChrome = isRouteHome(currentRootRoute)
    val isPlaying = rememberIsPlaybackPlaying()
    val onOpenNowPlaying = {
        globalNavController.navigate(MusicGraph.NowPlaying)
    }
    val onNavigateToImport = { type: String ->
        globalNavController.navigate(MusicGraph.Import(type))
    }
    val onNavigateToDownloads = {
        globalNavController.navigate(MusicGraph.Downloads)
    }
    val onNavigateToEditStorage = { id: Long ->
        globalNavController.navigate(MusicGraph.EditStorage(id))
    }
    val onNavigateToLog = {
        globalNavController.navigate(MusicGraph.Log)
    }
    val onNavigateToDebugMore = {
        globalNavController.navigate(MusicGraph.DebugMore)
    }
    val miniPlayerContent: @Composable () -> Unit = {
        PlaybackMiniPlayerHost(
            onOpenNowPlaying = onOpenNowPlaying,
        )
    }

    var currentTab by remember { mutableStateOf(HomeTab.PLAYLISTS) }

    val playlistsNavController = rememberNavController()
    val libraryNavController = rememberNavController()
    val searchNavController = rememberNavController()
    val dashboardNavController = rememberNavController()
    val settingsNavController = rememberNavController()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = rememberWindowSizeClass(
            containerSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight),
        )

        val tabContent: @Composable (HomeTab) -> Unit = { tab ->
            HomeTabContent(
                currentTab = tab,
                playlistsNavController = playlistsNavController,
                libraryNavController = libraryNavController,
                searchNavController = searchNavController,
                dashboardNavController = dashboardNavController,
                settingsNavController = settingsNavController,
                scaffoldPadding = scaffoldPadding,
                onNavigateToImport = onNavigateToImport,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToEditStorage = onNavigateToEditStorage,
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
                onOpenNowPlaying = onOpenNowPlaying,
            )
        }

        when (windowSizeClass) {
            WindowSizeClass.Compact -> {
                Box(
                    modifier = Modifier.padding(
                        bottom = getBottomBarSpace(isPlaying, scaffoldPadding),
                    ),
                ) {
                    tabContent(currentTab)
                }
                BottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    miniPlayerContent = miniPlayerContent,
                    showChrome = showHomeChrome,
                    scaffoldPadding = scaffoldPadding,
                )
            }
            WindowSizeClass.Medium -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRailBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        miniPlayerContent = miniPlayerContent,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        tabContent(currentTab)
                    }
                }
            }
            WindowSizeClass.Expanded -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    SidebarBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        miniPlayerContent = miniPlayerContent,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        tabContent(currentTab)
                    }
                }
            }
        }
    }
}

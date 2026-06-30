package com.github.tidetunes.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.github.tidetunes.feature.dashboard.presentation.navigation.DashboardTabGraph
import com.github.tidetunes.feature.importing.presentation.navigation.RouteImportType
import com.github.tidetunes.feature.library.presentation.navigation.LibraryTabGraph
import com.github.tidetunes.feature.playlist.presentation.navigation.PlaylistsTabGraph
import com.github.tidetunes.feature.search.presentation.navigation.SearchTabGraph
import com.github.tidetunes.feature.settings.presentation.navigation.SettingsTabGraph
import com.github.tidetunes.feature.sources.presentation.navigation.SourcesDashboardContent
import com.github.tidetunes.platform.getAppVersion
import com.github.tidetunes.service.playback.presentation.shell.PlaybackSleepTimerHost
import com.github.tidetunes.service.playback.presentation.shell.rememberOpenSleepTimer

@Composable
internal fun HomeTabContent(
    currentTab: HomeTab,
    playlistsNavController: NavHostController,
    libraryNavController: NavHostController,
    searchNavController: NavHostController,
    dashboardNavController: NavHostController,
    settingsNavController: NavHostController,
    scaffoldPadding: PaddingValues,
    onNavigateToImport: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToEditStorage: (Long) -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val openSleepTimer = rememberOpenSleepTimer()

    Crossfade(targetState = currentTab) { tab ->
        when (tab) {
            HomeTab.PLAYLISTS -> PlaylistsTabGraph(
                navController = playlistsNavController,
                scaffoldPadding = scaffoldPadding,
                onNavigateToEditPlaylistImport = {
                    onNavigateToImport(RouteImportType.EditPlaylist)
                },
                onNavigateToEditPlaylistCoverImport = {
                    onNavigateToImport(RouteImportType.EditPlaylistCover)
                },
                onNavigateToMusicImport = {
                    onNavigateToImport(RouteImportType.Music)
                },
                onOpenNowPlaying = onOpenNowPlaying,
            )
            HomeTab.LIBRARY -> LibraryTabGraph(libraryNavController)
            HomeTab.SEARCH -> SearchTabGraph(searchNavController)
            HomeTab.DASHBOARD -> DashboardTabGraph(
                navController = dashboardNavController,
                onOpenSleepTimer = openSleepTimer,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToEditStorage = onNavigateToEditStorage,
                sourcesContent = {
                    SourcesDashboardContent(
                        onNavigateToSourceEditor = onNavigateToEditStorage,
                    )
                },
                sleepTimerContent = {
                    PlaybackSleepTimerHost()
                },
            )
            HomeTab.SETTINGS -> SettingsTabGraph(
                navController = settingsNavController,
                appVersion = getAppVersion(),
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
            )
        }
    }
}

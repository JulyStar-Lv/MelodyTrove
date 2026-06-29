package com.github.tidetunes.widgets.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.github.tidetunes.navigation.HomeTab
import com.github.tidetunes.service.playback.presentation.sleep.SleepModeVM

@Composable
internal fun HomeTabContent(
    currentTab: HomeTab,
    playlistsNavController: NavHostController,
    libraryNavController: NavHostController,
    searchNavController: NavHostController,
    dashboardNavController: NavHostController,
    settingsNavController: NavHostController,
    scaffoldPadding: PaddingValues,
    sleepModeVM: SleepModeVM,
    onNavigateToImport: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToEditStorage: (Long) -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    Crossfade(targetState = currentTab) { tab ->
        when (tab) {
            HomeTab.PLAYLISTS -> PlaylistsTabGraph(
                navController = playlistsNavController,
                scaffoldPadding = scaffoldPadding,
                onNavigateToImport = onNavigateToImport,
                onOpenNowPlaying = onOpenNowPlaying,
            )
            HomeTab.LIBRARY -> LibraryTabGraph(libraryNavController)
            HomeTab.SEARCH -> SearchTabGraph(searchNavController)
            HomeTab.DASHBOARD -> DashboardTabGraph(
                navController = dashboardNavController,
                sleepModeVM = sleepModeVM,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToEditStorage = onNavigateToEditStorage,
            )
            HomeTab.SETTINGS -> SettingsTabGraph(
                navController = settingsNavController,
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
            )
        }
    }
}

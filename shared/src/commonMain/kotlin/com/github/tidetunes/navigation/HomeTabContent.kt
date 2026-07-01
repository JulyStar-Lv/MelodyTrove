package com.github.tidetunes.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.github.tidetunes.feature.home.presentation.HomeRoot
import com.github.tidetunes.feature.library.presentation.navigation.LibraryTabGraph
import com.github.tidetunes.feature.search.presentation.navigation.SearchTabGraph
import com.github.tidetunes.feature.settings.presentation.navigation.SettingsTabGraph
import com.github.tidetunes.platform.getAppVersion
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime
import com.github.tidetunes.service.playback.presentation.shell.rememberOpenSleepTimer

@Composable
internal fun HomeTabContent(
    currentTab: HomeTab,
    libraryNavController: NavHostController,
    searchNavController: NavHostController,
    settingsNavController: NavHostController,
    scaffoldPadding: PaddingValues,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val openSleepTimer = rememberOpenSleepTimer()

    Crossfade(targetState = currentTab) { tab ->
        when (tab) {
            HomeTab.HOME -> HomeRoot(
                scaffoldPadding = scaffoldPadding,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToSearch = onNavigateToSearch,
                onOpenSleepTimer = { openSleepTimer(SleepModeLeftTime(30 * 60 * 1000L)) },
                onOpenNowPlaying = onOpenNowPlaying,
            )
            HomeTab.SEARCH -> SearchTabGraph(searchNavController)
            HomeTab.LIBRARY -> LibraryTabGraph(libraryNavController)
            HomeTab.SETTINGS -> SettingsTabGraph(
                navController = settingsNavController,
                appVersion = getAppVersion(),
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
            )
        }
    }
}

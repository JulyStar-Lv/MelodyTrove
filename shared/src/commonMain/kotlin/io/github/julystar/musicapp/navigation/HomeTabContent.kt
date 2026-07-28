package io.github.julystar.musicapp.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import io.github.julystar.musicapp.core.LocalNavController
import io.github.julystar.musicapp.core.presentation.components.LocalDesignStickyHeaderStateSink
import io.github.julystar.musicapp.core.presentation.components.DesignStickyHeaderState
import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph
import io.github.julystar.musicapp.feature.home.presentation.HomeRoot
import io.github.julystar.musicapp.feature.library.presentation.navigation.LibraryTabGraph
import io.github.julystar.musicapp.feature.search.presentation.navigation.SearchTabGraph
import io.github.julystar.musicapp.feature.settings.presentation.navigation.SettingsTabGraph
import io.github.julystar.musicapp.platform.getAppBuildInfo
import io.github.julystar.musicapp.platform.getAppGitCommitSha
import io.github.julystar.musicapp.platform.getAppVersion
import io.github.julystar.musicapp.service.playback.domain.SleepModeLeftTime
import io.github.julystar.musicapp.service.playback.presentation.shell.rememberOpenSleepTimer

@Composable
internal fun HomeTabContent(
    currentTab: HomeTab,
    libraryNavController: NavHostController,
    searchNavController: NavHostController,
    settingsNavController: NavHostController,
    scaffoldPadding: PaddingValues,
    onNavigateToDownloads: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibraryFolderImport: () -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToPlaylists: () -> Unit,
    stickyHeaderStateSink: ((DesignStickyHeaderState?) -> Unit)? = null,
) {
    val openSleepTimer = rememberOpenSleepTimer()
    val rootNavController = LocalNavController.current

    Crossfade(targetState = currentTab) { tab ->
        CompositionLocalProvider(
            LocalDesignStickyHeaderStateSink provides if (
                stickyHeaderStateSink == null || tab == currentTab
            ) {
                stickyHeaderStateSink
            } else {
                IgnoreStickyHeaderState
            },
        ) {
            when (tab) {
                HomeTab.HOME -> HomeRoot(
                    scaffoldPadding = scaffoldPadding,
                    onNavigateToDownloads = onNavigateToDownloads,
                    onNavigateToLibrary = onNavigateToLibrary,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToListening = {
                        rootNavController.navigate(MusicGraph.Listening)
                    },
                    onOpenSleepTimer = { openSleepTimer(SleepModeLeftTime(30 * 60 * 1000L)) },
                )
                HomeTab.SEARCH -> SearchTabGraph(searchNavController)
                HomeTab.LIBRARY -> LibraryTabGraph(
                    navController = libraryNavController,
                    onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                    onNavigateToAlbum = onNavigateToAlbum,
                    onNavigateToArtist = onNavigateToArtist,
                    onNavigateToPlaylist = onNavigateToPlaylist,
                    onNavigateToPlaylists = onNavigateToPlaylists,
                )
                HomeTab.SETTINGS -> SettingsTabGraph(
                    navController = settingsNavController,
                    appVersion = getAppVersion(),
                    appBuildInfo = getAppBuildInfo(),
                    gitCommitSha = getAppGitCommitSha(),
                    onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                    onNavigateToPlugins = {
                        rootNavController.navigate(MusicGraph.PluginSettings)
                    },
                )
            }
        }
    }
}

private val IgnoreStickyHeaderState: (DesignStickyHeaderState?) -> Unit = {}

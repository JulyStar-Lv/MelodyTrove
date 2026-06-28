package com.github.tidetunes.widgets.home

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.tidetunes.core.LocalNavController
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.feature.dashboard.presentation.DashboardRoot
import com.github.tidetunes.feature.dashboard.presentation.TimeToPauseModal
import com.github.tidetunes.feature.library.presentation.LibraryRoot
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListRoot
import com.github.tidetunes.feature.search.presentation.SearchRoot
import com.github.tidetunes.feature.settings.presentation.SettingsRoot
import com.github.tidetunes.navigation.HomeTab
import com.github.tidetunes.viewmodels.PlayerVM
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.github.tidetunes.widgets.appbar.BottomBar
import com.github.tidetunes.widgets.appbar.NavigationRailBar
import com.github.tidetunes.widgets.appbar.SidebarBar
import com.github.tidetunes.widgets.appbar.getBottomBarSpace
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun HomePage(
    playerVM: PlayerVM = koinViewModel(),
    scaffoldPadding: PaddingValues,
) {
    val globalNavController = LocalNavController.current
    val isPlaying by playerVM.playing.collectAsState()

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
            Crossfade(targetState = tab) {
                when (tab) {
                    HomeTab.PLAYLISTS -> {
                        NavHost(
                            navController = playlistsNavController,
                            startDestination = "playlists_list",
                        ) {
                            composable("playlists_list") {
                                val playlistRepo: com.github.tidetunes.core.domain.repository.PlaylistRepository = koinInject()
                                val importRepo: com.github.tidetunes.source.api.ImportRepository = koinInject()
                                val createPlaylistVM = remember(playlistRepo, importRepo) {
                                    com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM(
                                        importRepository = importRepo,
                                        onCreatePlaylistRequest = { title, cover, entries ->
                                            (playlistRepo as com.github.tidetunes.core.data.PlaylistRepositoryImpl).createPlaylist(
                                                com.github.tidetunes.core.data.CreatePlaylistRequest(
                                                    title = title,
                                                    cover = cover,
                                                    entries = entries,
                                                )
                                            )
                                        },
                                    )
                                }
                                PlaylistsListRoot(
                                    onNavigateToPlaylist = { id ->
                                        playlistsNavController.navigate("playlist/$id")
                                    },
                                    onCreatePlaylist = createPlaylistVM::openModal
                                )
                                CreatePlaylistRoot(createPlaylistVM = createPlaylistVM)
                            }
                            composable(
                                route = "playlist/{id}",
                            ) {
                                PlaylistRoot(
                                    scaffoldPadding = scaffoldPadding,
                                    onNavigateBack = {
                                        playlistsNavController.popBackStack()
                                    },
                                    onNavigateToImport = {
                                        globalNavController.navigate(
                                            com.github.tidetunes.navigation.MusicGraph.Import("Music")
                                        )
                                    },
                                    onNavigateToPlayer = {
                                        globalNavController.navigate(
                                            com.github.tidetunes.navigation.MusicGraph.NowPlaying
                                        )
                                    },
                                )
                                EditPlaylistRoot()
                            }
                        }
                    }
                    HomeTab.LIBRARY -> {
                        NavHost(
                            navController = libraryNavController,
                            startDestination = "library",
                        ) {
                            composable("library") {
                                LibraryRoot()
                            }
                        }
                    }
                    HomeTab.SEARCH -> {
                        NavHost(
                            navController = searchNavController,
                            startDestination = "search",
                        ) {
                            composable("search") {
                                SearchRoot()
                            }
                        }
                    }
                    HomeTab.DASHBOARD -> {
                        NavHost(
                            navController = dashboardNavController,
                            startDestination = "dashboard",
                        ) {
                            composable("dashboard") {
                                DashboardRoot()
                                TimeToPauseModal()
                            }
                        }
                    }
                    HomeTab.SETTINGS -> {
                        NavHost(
                            navController = settingsNavController,
                            startDestination = "settings",
                        ) {
                            composable("settings") {
                                SettingsRoot()
                            }
                        }
                    }
                }
            }
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
                    scaffoldPadding = scaffoldPadding,
                )
            }
            WindowSizeClass.Medium -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRailBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
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

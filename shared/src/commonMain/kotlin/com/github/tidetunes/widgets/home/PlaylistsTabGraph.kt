package com.github.tidetunes.widgets.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.importing.data.RouteImportType
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListRoot
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun PlaylistsTabGraph(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
    onNavigateToImport: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "playlists_list",
    ) {
        composable("playlists_list") {
            val createPlaylistVM: CreatePlaylistVM = koinViewModel()
            PlaylistsListRoot(
                onNavigateToPlaylist = { id ->
                    navController.navigate("playlist/$id")
                },
                onCreatePlaylist = createPlaylistVM::openModal,
            )
            CreatePlaylistRoot(
                createPlaylistVM = createPlaylistVM,
                onNavigateToImport = {
                    onNavigateToImport(RouteImportType.EditPlaylist)
                },
                onNavigateToCoverImport = {
                    onNavigateToImport(RouteImportType.EditPlaylistCover)
                },
            )
        }
        composable(route = "playlist/{id}") {
            PlaylistRoot(
                scaffoldPadding = scaffoldPadding,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToImport = {
                    onNavigateToImport(RouteImportType.Music)
                },
                onNavigateToPlayer = onOpenNowPlaying,
            )
            EditPlaylistRoot(
                onNavigateToCoverImport = {
                    onNavigateToImport(RouteImportType.EditPlaylistCover)
                },
            )
        }
    }
}

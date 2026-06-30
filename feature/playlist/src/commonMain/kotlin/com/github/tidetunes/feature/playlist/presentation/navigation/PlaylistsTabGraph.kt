package com.github.tidetunes.feature.playlist.presentation.navigation

import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.CreatePlaylistVM
import com.github.tidetunes.feature.playlist.presentation.EditPlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistRoot
import com.github.tidetunes.feature.playlist.presentation.PlaylistsListRoot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistsTabGraph(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
    onNavigateToEditPlaylistImport: () -> Unit,
    onNavigateToEditPlaylistCoverImport: () -> Unit,
    onNavigateToMusicImport: () -> Unit,
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
                onNavigateToImport = onNavigateToEditPlaylistImport,
                onNavigateToCoverImport = onNavigateToEditPlaylistCoverImport,
            )
        }
        composable(route = "playlist/{id}") {
            PlaylistRoot(
                scaffoldPadding = scaffoldPadding,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToImport = onNavigateToMusicImport,
                onNavigateToPlayer = onOpenNowPlaying,
            )
            EditPlaylistRoot(
                onNavigateToCoverImport = onNavigateToEditPlaylistCoverImport,
            )
        }
    }
}

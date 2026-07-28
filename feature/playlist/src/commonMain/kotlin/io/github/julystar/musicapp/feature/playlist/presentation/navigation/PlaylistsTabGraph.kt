package io.github.julystar.musicapp.feature.playlist.presentation.navigation

import io.github.julystar.musicapp.feature.playlist.presentation.CreatePlaylistRoot
import io.github.julystar.musicapp.feature.playlist.presentation.CreatePlaylistVM
import io.github.julystar.musicapp.feature.playlist.presentation.EditPlaylistRoot
import io.github.julystar.musicapp.feature.playlist.presentation.PlaylistRoot
import io.github.julystar.musicapp.feature.playlist.presentation.PlaylistsListRoot

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
            )
            EditPlaylistRoot(
                onNavigateToCoverImport = onNavigateToEditPlaylistCoverImport,
            )
        }
    }
}

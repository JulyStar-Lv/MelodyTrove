package io.github.julystar.musicapp.feature.library.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.library.presentation.LibraryRoot

@Composable
fun LibraryTabGraph(
    navController: NavHostController,
    onNavigateToLibraryFolderImport: () -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = "library",
    ) {
        composable("library") {
            LibraryRoot(
                onNavigateToLibraryFolderImport = onNavigateToLibraryFolderImport,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                onNavigateToPlaylist = onNavigateToPlaylist,
                onNavigateToPlaylists = onNavigateToPlaylists,
            )
        }
    }
}

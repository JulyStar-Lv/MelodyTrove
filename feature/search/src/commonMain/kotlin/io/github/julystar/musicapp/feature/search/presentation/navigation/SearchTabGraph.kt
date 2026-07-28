package io.github.julystar.musicapp.feature.search.presentation.navigation

import io.github.julystar.musicapp.feature.search.presentation.SearchRoot

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SearchTabGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = "search",
    ) {
        composable("search") {
            SearchRoot(
                onNavigateToAlbum = { albumId -> },
                onNavigateToArtist = { artistId -> },
            )
        }
    }
}

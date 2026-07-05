package com.github.tidetunes.feature.library.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.library.presentation.LibraryRoot

@Composable
fun LibraryTabGraph(
    navController: NavHostController,
    onOpenNowPlaying: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = "library",
    ) {
        composable("library") {
            LibraryRoot(
                onOpenNowPlaying = onOpenNowPlaying,
            )
        }
    }
}

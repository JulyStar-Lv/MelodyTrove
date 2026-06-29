package com.github.tidetunes.widgets.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.library.presentation.LibraryRoot

@Composable
internal fun LibraryTabGraph(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = "library",
    ) {
        composable("library") {
            LibraryRoot()
        }
    }
}

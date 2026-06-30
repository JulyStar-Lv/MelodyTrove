package com.github.tidetunes.feature.library.presentation.navigation

import com.github.tidetunes.feature.library.presentation.LibraryRoot

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun LibraryTabGraph(
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

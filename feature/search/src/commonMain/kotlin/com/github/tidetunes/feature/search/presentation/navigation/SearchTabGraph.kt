package com.github.tidetunes.feature.search.presentation.navigation

import com.github.tidetunes.feature.search.presentation.SearchRoot

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
            SearchRoot()
        }
    }
}

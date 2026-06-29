package com.github.tidetunes.widgets.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.search.presentation.SearchRoot

@Composable
internal fun SearchTabGraph(
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

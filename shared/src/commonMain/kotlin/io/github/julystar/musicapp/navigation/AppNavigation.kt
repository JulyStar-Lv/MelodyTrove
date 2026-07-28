package io.github.julystar.musicapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun AppNavigation(
    navController: NavHostController,
) {
    AppShell { scaffoldPadding ->
        RootNavHost(
            navController = navController,
            scaffoldPadding = scaffoldPadding,
        )
    }
}

package io.github.julystar.musicapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppNavigation(
    navController: NavHostController,
) {
    val currentRoute by navController.currentBackStackEntryAsState()
    AppShell(
        immersiveContent = isImmersivePlayerRoute(currentRoute?.destination?.route),
    ) { scaffoldPadding ->
        RootNavHost(
            navController = navController,
            scaffoldPadding = scaffoldPadding,
        )
    }
}

package com.github.tidetunes.feature.settings.presentation.navigation

import com.github.tidetunes.feature.settings.presentation.SettingsRoot

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun SettingsTabGraph(
    navController: NavHostController,
    appVersion: String,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "settings",
    ) {
        composable("settings") {
            SettingsRoot(
                appVersion = appVersion,
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
            )
        }
    }
}

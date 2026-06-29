package com.github.tidetunes.widgets.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.settings.presentation.SettingsRoot
import com.github.tidetunes.platform.getAppVersion

@Composable
internal fun SettingsTabGraph(
    navController: NavHostController,
    onNavigateToLog: () -> Unit,
    onNavigateToDebugMore: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "settings",
    ) {
        composable("settings") {
            SettingsRoot(
                appVersion = getAppVersion(),
                onNavigateToLog = onNavigateToLog,
                onNavigateToDebugMore = onNavigateToDebugMore,
            )
        }
    }
}

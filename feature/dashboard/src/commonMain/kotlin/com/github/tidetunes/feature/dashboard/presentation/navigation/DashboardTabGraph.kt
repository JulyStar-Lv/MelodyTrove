package com.github.tidetunes.feature.dashboard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.core.presentation.navigation.NEW_STORAGE_ID
import com.github.tidetunes.feature.dashboard.presentation.DashboardRoot
import com.github.tidetunes.service.playback.domain.SleepModeLeftTime

@Composable
fun DashboardTabGraph(
    navController: NavHostController,
    onOpenSleepTimer: (SleepModeLeftTime) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToEditStorage: (Long) -> Unit,
    sourcesContent: @Composable () -> Unit,
    sleepTimerContent: @Composable () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
    ) {
        composable("dashboard") {
            DashboardRoot(
                onOpenSleepTimer = onOpenSleepTimer,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToAddDevice = {
                    onNavigateToEditStorage(NEW_STORAGE_ID)
                },
                sourcesContent = sourcesContent,
            )
            sleepTimerContent()
        }
    }
}

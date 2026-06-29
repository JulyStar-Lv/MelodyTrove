package com.github.tidetunes.widgets.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.dashboard.presentation.DashboardRoot
import com.github.tidetunes.feature.sources.presentation.SourcesRoot
import com.github.tidetunes.navigation.NEW_STORAGE_ID
import com.github.tidetunes.service.playback.presentation.sleep.SleepModeVM
import com.github.tidetunes.service.playback.presentation.sleep.TimeToPauseModal

@Composable
internal fun DashboardTabGraph(
    navController: NavHostController,
    sleepModeVM: SleepModeVM,
    onNavigateToDownloads: () -> Unit,
    onNavigateToEditStorage: (Long) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
    ) {
        composable("dashboard") {
            DashboardRoot(
                onOpenSleepTimer = sleepModeVM::openModal,
                onNavigateToDownloads = onNavigateToDownloads,
                onNavigateToAddDevice = {
                    onNavigateToEditStorage(NEW_STORAGE_ID)
                },
                sourcesContent = {
                    SourcesRoot(
                        onNavigateToSourceEditor = onNavigateToEditStorage,
                    )
                },
            )
            TimeToPauseModal(sleepModeVM = sleepModeVM)
        }
    }
}

package com.github.tidetunes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.github.tidetunes.feature.onboarding.presentation.OnboardingRoot

fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Onboarding> {
        OnboardingRoot(
            onOnboardingComplete = { navController.popBackStack() },
            onNavigateToSources = {
                navController.navigate(MusicGraph.EditStorage(id = NEW_STORAGE_ID))
            },
        )
    }
}

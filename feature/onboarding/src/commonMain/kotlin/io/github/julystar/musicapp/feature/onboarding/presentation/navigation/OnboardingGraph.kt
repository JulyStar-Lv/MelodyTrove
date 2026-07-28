package io.github.julystar.musicapp.feature.onboarding.presentation.navigation

import io.github.julystar.musicapp.core.presentation.navigation.MusicGraph
import io.github.julystar.musicapp.core.presentation.navigation.NEW_STORAGE_ID

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.julystar.musicapp.feature.onboarding.presentation.OnboardingRoot

fun NavGraphBuilder.onboardingGraph(
    navController: NavHostController,
) {
    composable<MusicGraph.Onboarding> {
        OnboardingRoot(
            onOnboardingComplete = {
                navController.navigate(MusicGraph.Home) {
                    popUpTo(MusicGraph.Onboarding) {
                        inclusive = true
                    }
                }
            },
            onNavigateToSources = {
                navController.navigate(MusicGraph.EditStorage(id = NEW_STORAGE_ID))
            },
        )
    }
}

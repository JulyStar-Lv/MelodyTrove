package io.github.julystar.musicapp.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoot(
    onOnboardingComplete: () -> Unit,
    onNavigateToSources: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.OnboardingComplete -> onOnboardingComplete()
                OnboardingEvent.NavigateToSources -> onNavigateToSources()
            }
        }
    }

    OnboardingScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

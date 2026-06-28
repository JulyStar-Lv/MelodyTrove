package com.github.tidetunes.feature.onboarding.presentation

sealed interface OnboardingAction {
    data object NextPage : OnboardingAction
    data object PreviousPage : OnboardingAction
    data object Finish : OnboardingAction
    data object NavigateToSources : OnboardingAction
}

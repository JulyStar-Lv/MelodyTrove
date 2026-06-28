package com.github.tidetunes.feature.onboarding.presentation

sealed interface OnboardingEvent {
    data object OnboardingComplete : OnboardingEvent
    data object NavigateToSources : OnboardingEvent
}

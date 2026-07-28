package io.github.julystar.musicapp.feature.onboarding.presentation

sealed interface OnboardingEvent {
    data object OnboardingComplete : OnboardingEvent
    data object NavigateToSources : OnboardingEvent
}

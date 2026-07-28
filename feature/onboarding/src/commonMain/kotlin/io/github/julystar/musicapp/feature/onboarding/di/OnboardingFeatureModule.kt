package io.github.julystar.musicapp.feature.onboarding.di

import io.github.julystar.musicapp.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingFeatureModule = module {
    viewModelOf(::OnboardingViewModel)
}

package com.github.tidetunes.feature.home.di

import com.github.tidetunes.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeFeatureModule = module {
    viewModel { HomeViewModel(get(), get()) }
}

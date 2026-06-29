package com.github.tidetunes.feature.dashboard.di

import com.github.tidetunes.feature.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dashboardFeatureModule = module {
    viewModel { DashboardViewModel(get(), get()) }
}

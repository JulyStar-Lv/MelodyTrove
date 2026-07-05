package com.github.tidetunes.feature.settings.di

import com.github.tidetunes.feature.settings.presentation.SettingsVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureDiModule = module {
    viewModel { SettingsVM(get(), get(), get(), get(), get(), get(), get(), get()) }
}

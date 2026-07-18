package com.github.tidetunes.feature.settings.di

import com.github.tidetunes.feature.settings.presentation.SettingsVM
import com.github.tidetunes.feature.settings.presentation.ComposeSettingsTextProvider
import com.github.tidetunes.feature.settings.presentation.SettingsTextProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureDiModule = module {
    single<SettingsTextProvider> { ComposeSettingsTextProvider() }
    viewModel {
        SettingsVM(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    }
}

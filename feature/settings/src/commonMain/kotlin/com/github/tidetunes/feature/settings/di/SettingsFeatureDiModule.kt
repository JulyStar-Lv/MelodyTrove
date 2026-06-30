package com.github.tidetunes.feature.settings.di

import com.github.tidetunes.feature.settings.presentation.DebugMoreVM
import com.github.tidetunes.feature.settings.presentation.LogVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureDiModule = module {
    viewModel { LogVM(get()) }
    viewModel { DebugMoreVM(get()) }
}

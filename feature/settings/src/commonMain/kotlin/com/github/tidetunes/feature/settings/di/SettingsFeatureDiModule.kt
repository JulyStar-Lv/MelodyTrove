package com.github.tidetunes.feature.settings.di

import com.github.tidetunes.feature.settings.presentation.DebugMoreVM
import com.github.tidetunes.feature.settings.presentation.LogVM
import com.github.tidetunes.feature.settings.presentation.ToastVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureDiModule = module {
    viewModel { ToastVM(get()) }
    viewModel { LogVM(get()) }
    viewModel { DebugMoreVM(get()) }
}

package com.github.tidetunes.feature.importing.di

import com.github.tidetunes.feature.importing.presentation.ImportStatusVM
import com.github.tidetunes.feature.importing.presentation.ImportVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val importingFeatureDiModule = module {
    viewModel { ImportStatusVM(get()) }
    viewModel { ImportVM(get(), get(), get(), get()) }
}

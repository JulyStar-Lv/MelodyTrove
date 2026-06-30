package com.github.tidetunes.feature.importing.di

import com.github.tidetunes.feature.importing.data.ImportRepositoryImpl
import com.github.tidetunes.feature.importing.presentation.ImportStatusVM
import com.github.tidetunes.feature.importing.presentation.ImportVM
import com.github.tidetunes.source.api.ImportRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val importingFeatureDiModule = module {
    single<ImportRepository> { ImportRepositoryImpl() }
    viewModel { ImportStatusVM(get()) }
    viewModel { ImportVM(get(), get(), get(), get()) }
}

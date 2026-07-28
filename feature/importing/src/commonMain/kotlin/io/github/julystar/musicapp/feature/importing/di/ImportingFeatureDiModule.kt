package io.github.julystar.musicapp.feature.importing.di

import io.github.julystar.musicapp.feature.importing.data.ImportRepositoryImpl
import io.github.julystar.musicapp.feature.importing.presentation.ImportStatusVM
import io.github.julystar.musicapp.feature.importing.presentation.ImportVM
import io.github.julystar.musicapp.source.api.ImportRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val importingFeatureDiModule = module {
    single<ImportRepository> { ImportRepositoryImpl() }
    viewModel { ImportStatusVM(get()) }
    viewModel { ImportVM(get(), get(), get(), get()) }
}

package io.github.julystar.musicapp.feature.library.di

import io.github.julystar.musicapp.feature.library.presentation.LibraryVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryFeatureDiModule = module {
    viewModel { LibraryVM(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

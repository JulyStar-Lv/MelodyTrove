package com.github.tidetunes.feature.library.di

import com.github.tidetunes.feature.library.presentation.LibraryVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryFeatureDiModule = module {
    viewModel { LibraryVM(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

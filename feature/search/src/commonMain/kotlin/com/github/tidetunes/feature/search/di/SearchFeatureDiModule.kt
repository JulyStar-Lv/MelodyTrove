package com.github.tidetunes.feature.search.di

import com.github.tidetunes.feature.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchFeatureDiModule = module {
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
}

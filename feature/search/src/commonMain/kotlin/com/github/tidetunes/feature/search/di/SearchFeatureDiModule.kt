package com.github.tidetunes.feature.search.di

import com.github.tidetunes.feature.search.data.DataStoreSearchHistoryRepository
import com.github.tidetunes.feature.search.data.MusicSourceSearchAggregator
import com.github.tidetunes.feature.search.domain.SearchAggregator
import com.github.tidetunes.feature.search.domain.SearchHistoryRepository
import com.github.tidetunes.feature.search.domain.SearchLibraryUseCase
import com.github.tidetunes.feature.search.domain.SearchSuggestionsUseCase
import com.github.tidetunes.feature.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchFeatureDiModule = module {
    single<SearchAggregator> { MusicSourceSearchAggregator(get(), get()) }
    single<SearchHistoryRepository> { DataStoreSearchHistoryRepository(get()) }
    single { SearchLibraryUseCase(get()) }
    single { SearchSuggestionsUseCase(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
}

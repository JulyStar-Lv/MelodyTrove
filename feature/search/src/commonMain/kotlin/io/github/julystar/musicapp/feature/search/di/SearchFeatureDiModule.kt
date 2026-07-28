package io.github.julystar.musicapp.feature.search.di

import io.github.julystar.musicapp.feature.search.data.DataStoreSearchHistoryRepository
import io.github.julystar.musicapp.feature.search.data.MusicSourceSearchAggregator
import io.github.julystar.musicapp.feature.search.domain.SearchAggregator
import io.github.julystar.musicapp.feature.search.domain.SearchHistoryRepository
import io.github.julystar.musicapp.feature.search.domain.SearchLibraryUseCase
import io.github.julystar.musicapp.feature.search.domain.SearchSuggestionsUseCase
import io.github.julystar.musicapp.feature.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchFeatureDiModule = module {
    single<SearchAggregator> { MusicSourceSearchAggregator(get(), get()) }
    single<SearchHistoryRepository> { DataStoreSearchHistoryRepository(get()) }
    single { SearchLibraryUseCase(get()) }
    single { SearchSuggestionsUseCase(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
}

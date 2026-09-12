package io.github.julystar.musicapp.feature.search.di

import io.github.julystar.musicapp.feature.search.data.DataStoreSearchHistoryRepository
import io.github.julystar.musicapp.feature.search.data.MusicSourceSearchAggregator
import io.github.julystar.musicapp.core.domain.search.SearchAggregator
import io.github.julystar.musicapp.core.domain.search.SearchHistoryRepository
import io.github.julystar.musicapp.core.domain.search.SearchLibraryUseCase
import io.github.julystar.musicapp.core.domain.search.SearchSuggestionsUseCase
import io.github.julystar.musicapp.feature.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchDomainModule = module {
    single<SearchAggregator> { MusicSourceSearchAggregator(get(), get()) }
    single<SearchHistoryRepository> { DataStoreSearchHistoryRepository(get()) }
    single { SearchLibraryUseCase(get()) }
    single { SearchSuggestionsUseCase(get(), get(), get()) }
}

val searchPresentationModule = module {
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
}

val searchFeatureDiModule = module {
    includes(searchDomainModule, searchPresentationModule)
}

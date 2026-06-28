package com.github.tidetunes.di

import com.github.tidetunes.feature.search.data.DataStoreSearchHistoryRepository
import com.github.tidetunes.feature.search.data.MusicSourceSearchAggregator
import com.github.tidetunes.feature.search.data.RoomSearchRepository
import com.github.tidetunes.feature.search.data.StorageSearchSourceAccountProvider
import com.github.tidetunes.feature.search.domain.SearchAggregator
import com.github.tidetunes.feature.search.domain.SearchHistoryRepository
import com.github.tidetunes.feature.search.domain.SearchLibraryUseCase
import com.github.tidetunes.feature.search.domain.SearchRepository
import com.github.tidetunes.feature.search.domain.SearchSourceAccountProvider
import com.github.tidetunes.feature.search.domain.SearchSuggestionsUseCase
import com.github.tidetunes.feature.search.di.searchFeatureDiModule
import org.koin.dsl.module

val searchFeatureModule = module {
    includes(searchFeatureDiModule)
    single { RoomSearchRepository(get(), get()) }
    single<SearchRepository> { get<RoomSearchRepository>() }
    single<SearchAggregator> { MusicSourceSearchAggregator(get(), get()) }
    single<SearchSourceAccountProvider> { StorageSearchSourceAccountProvider(get()) }
    single<SearchHistoryRepository> { DataStoreSearchHistoryRepository(get()) }
    single { SearchLibraryUseCase(get()) }
    single { SearchSuggestionsUseCase(get(), get(), get()) }
}

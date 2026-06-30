package com.github.tidetunes.di

import com.github.tidetunes.feature.search.data.RoomSearchRepository
import com.github.tidetunes.feature.search.data.StorageSearchSourceAccountProvider
import com.github.tidetunes.feature.search.domain.SearchRepository
import com.github.tidetunes.feature.search.domain.SearchSourceAccountProvider
import com.github.tidetunes.feature.search.di.searchFeatureDiModule
import org.koin.dsl.module

val searchFeatureModule = module {
    includes(searchFeatureDiModule)
    single { RoomSearchRepository(get(), get()) }
    single<SearchRepository> { get<RoomSearchRepository>() }
    single<SearchSourceAccountProvider> { StorageSearchSourceAccountProvider(get()) }
}

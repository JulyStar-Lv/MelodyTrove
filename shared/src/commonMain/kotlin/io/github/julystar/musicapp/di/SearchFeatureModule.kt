package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.search.data.RoomSearchRepository
import io.github.julystar.musicapp.feature.search.data.StorageSearchSourceAccountProvider
import io.github.julystar.musicapp.feature.search.domain.SearchRepository
import io.github.julystar.musicapp.feature.search.domain.SearchSourceAccountProvider
import io.github.julystar.musicapp.feature.search.di.searchFeatureDiModule
import org.koin.dsl.module

val searchFeatureModule = module {
    includes(searchFeatureDiModule)
    single { RoomSearchRepository(get(), get(), get(), get()) }
    single<SearchRepository> { get<RoomSearchRepository>() }
    single<SearchSourceAccountProvider> { StorageSearchSourceAccountProvider(get()) }
}

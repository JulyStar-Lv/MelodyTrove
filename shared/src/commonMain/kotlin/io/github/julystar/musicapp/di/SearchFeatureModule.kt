package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.search.data.RoomSearchRepository
import io.github.julystar.musicapp.feature.search.data.StorageSearchSourceAccountProvider
import io.github.julystar.musicapp.core.domain.search.SearchRepository
import io.github.julystar.musicapp.core.domain.search.SearchSourceAccountProvider
import io.github.julystar.musicapp.feature.search.di.searchDomainModule
import io.github.julystar.musicapp.feature.search.di.searchPresentationModule
import org.koin.dsl.module

val searchDataModule = module {
    includes(searchDomainModule)
    single { RoomSearchRepository(get(), get(), get(), get()) }
    single<SearchRepository> { get<RoomSearchRepository>() }
    single<SearchSourceAccountProvider> { StorageSearchSourceAccountProvider(get()) }
}

val searchFeatureModule = module {
    includes(searchDataModule, searchPresentationModule)
}

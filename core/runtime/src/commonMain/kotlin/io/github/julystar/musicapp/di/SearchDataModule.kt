package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.core.domain.search.SearchRepository
import io.github.julystar.musicapp.core.domain.search.SearchSourceAccountProvider
import io.github.julystar.musicapp.feature.search.data.RoomSearchRepository
import io.github.julystar.musicapp.feature.search.data.StorageSearchSourceAccountProvider
import org.koin.dsl.module

val searchDataModule = module {
    single { RoomSearchRepository(get(), get(), get(), get()) }
    single<SearchRepository> { get<RoomSearchRepository>() }
    single<SearchSourceAccountProvider> { StorageSearchSourceAccountProvider(get()) }
}

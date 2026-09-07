package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.home.data.RoomHomeHistoryRepository
import io.github.julystar.musicapp.feature.home.domain.HomeHistoryRepository
import org.koin.dsl.module

val homePresentationDataModule = module {
    single<HomeHistoryRepository> { RoomHomeHistoryRepository(get(), get(), get()) }
}

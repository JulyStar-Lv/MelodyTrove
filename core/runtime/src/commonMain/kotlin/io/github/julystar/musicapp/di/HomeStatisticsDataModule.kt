package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.core.domain.repository.HomeStatisticsRepository
import io.github.julystar.musicapp.feature.home.data.RoomHomeStatisticsRepository
import org.koin.dsl.module

val homeStatisticsDataModule = module {
    single<HomeStatisticsRepository> { RoomHomeStatisticsRepository(get(), get(), get(), get()) }
}

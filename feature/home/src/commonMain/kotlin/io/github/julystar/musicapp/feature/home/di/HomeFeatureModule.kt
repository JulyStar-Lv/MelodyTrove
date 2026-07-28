package io.github.julystar.musicapp.feature.home.di

import io.github.julystar.musicapp.feature.home.data.DataStoreHomePinnedRepository
import io.github.julystar.musicapp.feature.home.domain.HomePinnedRepository
import io.github.julystar.musicapp.feature.home.presentation.HomeViewModel
import io.github.julystar.musicapp.feature.home.presentation.ListeningViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeFeatureModule = module {
    single<HomePinnedRepository> { DataStoreHomePinnedRepository(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { ListeningViewModel(get(), get()) }
}

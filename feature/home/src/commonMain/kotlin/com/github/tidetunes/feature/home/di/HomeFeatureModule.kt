package com.github.tidetunes.feature.home.di

import com.github.tidetunes.feature.home.data.DataStoreHomePinnedRepository
import com.github.tidetunes.feature.home.domain.HomePinnedRepository
import com.github.tidetunes.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeFeatureModule = module {
    single<HomePinnedRepository> { DataStoreHomePinnedRepository(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
}

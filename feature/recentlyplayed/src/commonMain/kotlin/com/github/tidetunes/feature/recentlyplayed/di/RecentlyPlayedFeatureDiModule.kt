package com.github.tidetunes.feature.recentlyplayed.di

import com.github.tidetunes.feature.recentlyplayed.presentation.RecentlyPlayedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recentlyPlayedFeatureDiModule = module {
    viewModelOf(::RecentlyPlayedViewModel)
}

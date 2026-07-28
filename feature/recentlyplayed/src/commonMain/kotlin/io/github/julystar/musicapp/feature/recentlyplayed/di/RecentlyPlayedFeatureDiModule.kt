package io.github.julystar.musicapp.feature.recentlyplayed.di

import io.github.julystar.musicapp.feature.recentlyplayed.presentation.RecentlyPlayedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recentlyPlayedFeatureDiModule = module {
    viewModelOf(::RecentlyPlayedViewModel)
}

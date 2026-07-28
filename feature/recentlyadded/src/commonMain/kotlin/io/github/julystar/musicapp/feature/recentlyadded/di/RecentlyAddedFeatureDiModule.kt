package io.github.julystar.musicapp.feature.recentlyadded.di

import io.github.julystar.musicapp.feature.recentlyadded.presentation.RecentlyAddedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recentlyAddedFeatureDiModule = module {
    viewModelOf(::RecentlyAddedViewModel)
}

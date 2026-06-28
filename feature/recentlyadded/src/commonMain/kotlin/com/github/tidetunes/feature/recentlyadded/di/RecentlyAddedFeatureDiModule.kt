package com.github.tidetunes.feature.recentlyadded.di

import com.github.tidetunes.feature.recentlyadded.presentation.RecentlyAddedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recentlyAddedFeatureDiModule = module {
    viewModelOf(::RecentlyAddedViewModel)
}

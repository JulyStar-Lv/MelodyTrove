package com.github.tidetunes.feature.browse.di

import com.github.tidetunes.feature.browse.presentation.BrowseViewModel
import com.github.tidetunes.feature.browse.presentation.GenreTracksViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val browseFeatureDiModule = module {
    viewModelOf(::BrowseViewModel)
    viewModelOf(::GenreTracksViewModel)
}

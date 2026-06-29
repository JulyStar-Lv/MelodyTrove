package com.github.tidetunes.feature.album.di

import com.github.tidetunes.feature.album.presentation.AlbumViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val albumFeatureDiModule = module {
    viewModelOf(::AlbumViewModel)
}

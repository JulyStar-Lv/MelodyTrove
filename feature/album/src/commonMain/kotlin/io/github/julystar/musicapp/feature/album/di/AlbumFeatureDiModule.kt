package io.github.julystar.musicapp.feature.album.di

import io.github.julystar.musicapp.feature.album.presentation.AlbumViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val albumFeatureDiModule = module {
    viewModelOf(::AlbumViewModel)
}

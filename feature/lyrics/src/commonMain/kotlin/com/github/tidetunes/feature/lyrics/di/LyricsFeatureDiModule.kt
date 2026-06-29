package com.github.tidetunes.feature.lyrics.di

import com.github.tidetunes.feature.lyrics.presentation.LyricsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val lyricsFeatureDiModule = module {
    viewModelOf(::LyricsViewModel)
}

package io.github.julystar.musicapp.feature.lyrics.di

import io.github.julystar.musicapp.feature.lyrics.presentation.LyricsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val lyricsFeatureDiModule = module {
    viewModelOf(::LyricsViewModel)
}

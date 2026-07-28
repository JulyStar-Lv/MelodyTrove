package io.github.julystar.musicapp.feature.sources.di

import io.github.julystar.musicapp.feature.sources.presentation.EditStorageVM
import io.github.julystar.musicapp.feature.sources.presentation.SourcesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sourcesFeatureModule = module {
    viewModelOf(::SourcesViewModel)
    viewModelOf(::EditStorageVM)
}

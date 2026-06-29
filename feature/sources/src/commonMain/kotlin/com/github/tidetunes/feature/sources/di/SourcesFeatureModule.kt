package com.github.tidetunes.feature.sources.di

import com.github.tidetunes.feature.sources.presentation.EditStorageVM
import com.github.tidetunes.feature.sources.presentation.SourcesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sourcesFeatureModule = module {
    viewModelOf(::SourcesViewModel)
    viewModelOf(::EditStorageVM)
}

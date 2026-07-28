package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.importing.di.importingFeatureDiModule
import org.koin.dsl.module

val importFeatureModule = module {
    includes(importingFeatureDiModule)
}

package com.github.tidetunes.di

import com.github.tidetunes.feature.importing.di.importingFeatureDiModule
import org.koin.dsl.module

val importFeatureModule = module {
    includes(importingFeatureDiModule)
}

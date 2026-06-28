package com.github.tidetunes.di

import com.github.tidetunes.feature.downloads.di.downloadsFeatureModule
import org.koin.dsl.module

val appModule = module {
    includes(
        platformModule,
        coreDataModule,
        sourceDataModule,
        playbackModule,
        downloadModule,
        downloadsFeatureModule,
        librarySyncModule,
        libraryFeatureModule,
        searchFeatureModule,
        importFeatureModule,
        settingsFeatureModule,
    )
}

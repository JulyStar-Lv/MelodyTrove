package com.github.tidetunes.di

import com.github.tidetunes.feature.downloads.di.downloadsFeatureModule
import com.github.tidetunes.feature.home.di.homeFeatureModule
import com.github.tidetunes.feature.sources.di.sourcesFeatureModule
import org.koin.dsl.module

val appModule = module {
    includes(
        platformModule,
        coreDataModule,
        sourceDataModule,
        playbackModule,
        downloadModule,
        downloadsFeatureModule,
        homeFeatureModule,
        sourcesFeatureModule,
        librarySyncModule,
        libraryFeatureModule,
        searchFeatureModule,
        importFeatureModule,
        settingsFeatureModule,
    )
}

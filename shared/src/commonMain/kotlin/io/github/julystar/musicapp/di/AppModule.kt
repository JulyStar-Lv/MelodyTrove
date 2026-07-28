package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.downloads.di.downloadsFeatureModule
import io.github.julystar.musicapp.feature.home.di.homeFeatureModule
import io.github.julystar.musicapp.feature.sources.di.sourcesFeatureModule
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

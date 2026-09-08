package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.downloads.di.downloadsFeatureModule
import io.github.julystar.musicapp.feature.home.di.homeFeatureModule
import io.github.julystar.musicapp.feature.sources.di.sourcesFeatureModule
import org.koin.dsl.module

val appModule = module {
    includes(
        playbackModule,
        downloadsFeatureModule,
        homePresentationDataModule,
        homeFeatureModule,
        sourcesFeatureModule,
        libraryFeatureModule,
        searchFeatureModule,
        importFeatureModule,
        settingsFeatureModule,
    )
}

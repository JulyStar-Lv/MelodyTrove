package io.github.julystar.musicapp.runtime

import io.github.julystar.musicapp.di.coreDataModule
import io.github.julystar.musicapp.di.downloadModule
import io.github.julystar.musicapp.di.homeStatisticsDataModule
import io.github.julystar.musicapp.di.libraryDataModule
import io.github.julystar.musicapp.di.librarySyncModule
import io.github.julystar.musicapp.di.platformModule
import io.github.julystar.musicapp.di.playbackRuntimeModule
import io.github.julystar.musicapp.di.searchDataModule
import io.github.julystar.musicapp.di.sourceDataModule
import org.koin.core.module.Module

val runtimeModules: List<Module> = listOf(
    platformModule,
    coreDataModule,
    sourceDataModule,
    downloadModule,
    homeStatisticsDataModule,
    librarySyncModule,
    libraryDataModule,
    searchDataModule,
    playbackRuntimeModule,
)

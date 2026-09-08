package io.github.julystar.musicapp.di

import io.github.julystar.musicapp.feature.search.di.searchDomainModule
import io.github.julystar.musicapp.feature.search.di.searchPresentationModule
import org.koin.dsl.module

val searchFeatureModule = module {
    includes(searchDomainModule, searchPresentationModule)
}

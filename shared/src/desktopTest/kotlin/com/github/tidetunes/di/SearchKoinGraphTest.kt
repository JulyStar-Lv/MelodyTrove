package com.github.tidetunes.di

import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.feature.search.domain.SearchLibraryUseCase
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.storage.LegacyStorageLookup
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class SearchKoinGraphTest {
    @Test
    fun resolvesSearchLibraryUseCaseWithCoreDaoBindings() {
        val originalHome = System.getProperty("user.home")
        val tempHome = Files.createTempDirectory("tidetunes-koin-").toFile()
        var koinApplication: KoinApplication? = null

        try {
            System.setProperty("user.home", tempHome.absolutePath)
            koinApplication = koinApplication(createEagerInstances = false) {
                modules(
                    coreDataModule,
                    module {
                        single<LegacyStorageLookup> { LegacyStorageLookup { null } }
                        single { MusicSourceRegistry(emptyList()) }
                    },
                    searchFeatureModule,
                )
            }

            assertNotNull(koinApplication.koin.get<SearchLibraryUseCase>())
        } finally {
            koinApplication?.koin?.getOrNull<TideTunesDatabase>()?.close()
            koinApplication?.close()
            System.setProperty("user.home", originalHome)
            tempHome.deleteRecursively()
        }
    }
}

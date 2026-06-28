package com.github.tidetunes.feature.search.data

import com.github.tidetunes.core.data.datastore.createAppDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStoreSearchHistoryRepositoryTest {
    @Test
    fun persistsOrderedSearchHistoryInDataStore() = runBlocking {
        val file = File.createTempFile("tidetunes-search-history-", ".preferences_pb").apply {
            delete()
        }

        try {
            val dataStore = createAppDataStore { file.absolutePath.toPath() }
            val repository = DataStoreSearchHistoryRepository(dataStore)

            repository.remember("moon")
            repository.remember("sun")
            repository.remember("MOON")

            assertEquals(
                listOf("MOON", "sun"),
                withTimeout(5_000) { repository.history.first() },
            )

            val reloaded = DataStoreSearchHistoryRepository(dataStore)
            assertEquals(
                listOf("MOON", "sun"),
                withTimeout(5_000) { reloaded.history.first() },
            )

            reloaded.clear()
            assertEquals(emptyList(), withTimeout(5_000) { repository.history.first() })
        } finally {
            file.delete()
        }
    }
}

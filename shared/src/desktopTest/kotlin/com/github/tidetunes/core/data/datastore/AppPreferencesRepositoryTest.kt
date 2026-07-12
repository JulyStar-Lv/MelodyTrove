package com.github.tidetunes.core.data.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import uniffi.tidetunes_backend.PlayMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AppPreferencesRepositoryTest {
    @Test
    fun persistsPlayModeInDataStore() = runBlocking {
        val file = File.createTempFile("tidetunes-preferences-", ".preferences_pb").apply {
            delete()
        }

        try {
            val repository = AppPreferencesRepository(
                createAppDataStore { file.absolutePath.toPath() }
            )

            assertEquals(PlayMode.SINGLE, withTimeout(5_000) { repository.playMode.first() })

            repository.setPlayMode(PlayMode.LIST_LOOP)

            assertEquals(PlayMode.LIST_LOOP, withTimeout(5_000) { repository.playMode.first() })
        } finally {
            file.delete()
        }
    }
}

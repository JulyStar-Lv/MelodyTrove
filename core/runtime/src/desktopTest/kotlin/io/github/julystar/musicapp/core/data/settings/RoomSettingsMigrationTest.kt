package io.github.julystar.musicapp.core.data.settings

import androidx.room.Room
import androidx.datastore.preferences.core.edit
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.julystar.musicapp.core.data.datastore.createAppDataStore
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.database.ProviderTypes
import io.github.julystar.musicapp.database.SourceAccountEntity
import io.github.julystar.musicapp.database.AppDatabase
import io.github.julystar.musicapp.database.AppDatabaseConstructor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomSettingsMigrationTest {
    @Test
    fun newUsersDefaultToStandardAndExistingWebDavUsersMigrateToFullOnce() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase> {
            AppDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            withDataStore { dataStore ->
                RoomSettingsMigration(dataStore, database.sourceAccountDao()).migrate()
                assertEquals(
                    MetadataScanMode.Standard,
                    DataStoreSettingsRepository(dataStore).settings.first().webDavMetadataScanMode,
                )
            }

            database.sourceAccountDao().upsert(
                SourceAccountEntity(
                    id = 1,
                    providerType = ProviderTypes.WebDav,
                    displayName = "Existing WebDAV",
                    endpoint = "https://example.invalid/dav",
                    externalAccountId = null,
                    credentialRef = "credential",
                    priority = 0,
                    enabled = true,
                    createdAt = 1,
                    updatedAt = 1,
                )
            )
            withDataStore { dataStore ->
                val migration = RoomSettingsMigration(dataStore, database.sourceAccountDao())
                val repository = DataStoreSettingsRepository(dataStore)
                migration.migrate()
                assertEquals(MetadataScanMode.Full, repository.settings.first().webDavMetadataScanMode)

                repository.setWebDavMetadataScanMode(MetadataScanMode.Fast)
                migration.migrate()
                assertEquals(MetadataScanMode.Fast, repository.settings.first().webDavMetadataScanMode)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun legacyWebDavRootPathMovesToRoomAndIsRemovedFromPreferences() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase> {
            AppDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            database.sourceAccountDao().upsert(
                SourceAccountEntity(
                    id = 7,
                    providerType = ProviderTypes.WebDav,
                    displayName = "Legacy WebDAV",
                    endpoint = "https://example.invalid/dav",
                    externalAccountId = null,
                    credentialRef = "credential",
                    priority = 0,
                    enabled = true,
                    createdAt = 1,
                    updatedAt = 1,
                )
            )
            withDataStore { dataStore ->
                val serialized = Json.encodeToString(
                    MapSerializer(String.serializer(), String.serializer()),
                    mapOf("storage:7" to "Music/Albums"),
                )
                dataStore.edit { preferences -> preferences[WEB_DAV_ROOT_PATHS_KEY] = serialized }

                RoomSettingsMigration(dataStore, database.sourceAccountDao()).migrate()

                assertEquals("/Music/Albums", database.sourceAccountDao().get(7)?.rootPath)
                assertNull(dataStore.data.first()[WEB_DAV_ROOT_PATHS_KEY])
            }
        } finally {
            database.close()
        }
    }

    private suspend fun withDataStore(
        block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit,
    ) {
        val file = File.createTempFile("musicapp-settings-migration-", ".preferences_pb")
            .apply { delete() }
        try {
            block(createAppDataStore { file.absolutePath.toPath() })
        } finally {
            file.delete()
        }
    }
}

package io.github.julystar.musicapp.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.julystar.musicapp.migration.IosLegacyDataMigration
import io.github.julystar.musicapp.platform.getAppDataDirectory
import okio.Path.Companion.toPath

actual fun createAppDataStore(): DataStore<Preferences> {
    val dataDirectory = getAppDataDirectory()
    IosLegacyDataMigration.ensureMigrated(dataDirectory)
    return createAppDataStore {
        "$dataDirectory/$APP_DATA_STORE_FILE_NAME".toPath()
    }
}

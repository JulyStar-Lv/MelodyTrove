package io.github.julystar.musicapp.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.julystar.musicapp.migration.AndroidLegacyDataMigration
import io.github.julystar.musicapp.platform.appContext
import okio.Path.Companion.toPath

actual fun createAppDataStore(): DataStore<Preferences> {
    AndroidLegacyDataMigration.ensureMigrated()
    return createAppDataStore {
        appContext.filesDir.resolve(APP_DATA_STORE_FILE_NAME).absolutePath.toPath()
    }
}

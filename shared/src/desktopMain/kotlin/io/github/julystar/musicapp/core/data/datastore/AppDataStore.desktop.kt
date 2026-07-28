package io.github.julystar.musicapp.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.julystar.musicapp.platform.getAppDataDirectory
import okio.Path.Companion.toPath
import java.io.File

actual fun createAppDataStore(): DataStore<Preferences> {
    return createAppDataStore {
        File(getAppDataDirectory()).apply { mkdirs() }.resolve(APP_DATA_STORE_FILE_NAME).absolutePath.toPath()
    }
}

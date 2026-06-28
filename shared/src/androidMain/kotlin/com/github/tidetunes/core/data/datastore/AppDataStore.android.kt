package com.github.tidetunes.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.github.tidetunes.platform.appContext
import okio.Path.Companion.toPath

actual fun createAppDataStore(): DataStore<Preferences> {
    return createAppDataStore {
        appContext.filesDir.resolve(APP_DATA_STORE_FILE_NAME).absolutePath.toPath()
    }
}

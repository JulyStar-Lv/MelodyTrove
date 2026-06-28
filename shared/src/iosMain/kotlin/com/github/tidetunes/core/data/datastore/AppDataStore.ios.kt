package com.github.tidetunes.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.github.tidetunes.platform.getAppDocumentDir
import okio.Path.Companion.toPath

actual fun createAppDataStore(): DataStore<Preferences> {
    return createAppDataStore {
        "${getAppDocumentDir()}/$APP_DATA_STORE_FILE_NAME".toPath()
    }
}

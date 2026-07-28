package io.github.julystar.musicapp.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.julystar.musicapp.migration.AppIdentifiers
import okio.Path

internal const val APP_DATA_STORE_FILE_NAME = AppIdentifiers.PREFERENCES_FILE

fun createAppDataStore(producePath: () -> Path): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(produceFile = producePath)
}

expect fun createAppDataStore(): DataStore<Preferences>

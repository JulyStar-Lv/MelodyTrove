package com.github.tidetune.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path

internal const val APP_DATA_STORE_FILE_NAME = "tidetune.preferences_pb"

fun createAppDataStore(producePath: () -> Path): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(produceFile = producePath)
}

expect fun createAppDataStore(): DataStore<Preferences>

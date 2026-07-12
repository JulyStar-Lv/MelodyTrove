package com.github.tidetunes.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uniffi.tidetunes_backend.PlayMode

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val playMode: Flow<PlayMode> = dataStore.data.map { preferences ->
        preferences[PLAY_MODE_KEY]
            ?.let { value -> runCatching { PlayMode.valueOf(value) }.getOrNull() }
            ?: PlayMode.SINGLE
    }

    suspend fun setPlayMode(playMode: PlayMode) {
        dataStore.edit { preferences ->
            preferences[PLAY_MODE_KEY] = playMode.name
        }
    }
}

private val PLAY_MODE_KEY = stringPreferencesKey("playMode")

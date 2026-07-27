package com.github.tidetunes.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uniffi.tidetunes_backend.PlayMode

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val favoriteTrackIds: Flow<Set<Long>> = dataStore.data.map { preferences ->
        preferences[FAVORITE_TRACK_IDS_KEY]
            .orEmpty()
            .mapNotNullTo(mutableSetOf(), String::toLongOrNull)
    }

    val playMode: Flow<PlayMode> = dataStore.data.map { preferences ->
        preferences[PLAY_MODE_KEY]
            ?.let { value -> runCatching { PlayMode.valueOf(value) }.getOrNull() }
            ?: PlayMode.SINGLE
    }

    val playbackSession: Flow<PersistedPlaybackSession?> = dataStore.data.map { preferences ->
        val trackId = preferences[LAST_TRACK_ID_KEY] ?: return@map null
        val playlistId = preferences[LAST_PLAYLIST_ID_KEY] ?: return@map null
        PersistedPlaybackSession(
            trackId = trackId,
            playlistId = playlistId,
            positionMs = preferences[LAST_POSITION_MS_KEY] ?: 0L,
            wasPlaying = preferences[LAST_WAS_PLAYING_KEY] ?: false,
        )
    }

    suspend fun setPlayMode(playMode: PlayMode) {
        dataStore.edit { preferences ->
            preferences[PLAY_MODE_KEY] = playMode.name
        }
    }

    suspend fun savePlaybackSession(session: PersistedPlaybackSession) {
        dataStore.edit { preferences ->
            preferences[LAST_TRACK_ID_KEY] = session.trackId
            preferences[LAST_PLAYLIST_ID_KEY] = session.playlistId
            preferences[LAST_POSITION_MS_KEY] = session.positionMs.coerceAtLeast(0L)
            preferences[LAST_WAS_PLAYING_KEY] = session.wasPlaying
        }
    }

    suspend fun toggleFavoriteTrack(trackId: Long): Boolean {
        var isFavorite = false
        dataStore.edit { preferences ->
            val trackIds = preferences[FAVORITE_TRACK_IDS_KEY].orEmpty().toMutableSet()
            val value = trackId.toString()
            isFavorite = if (value in trackIds) {
                trackIds.remove(value)
                false
            } else {
                trackIds.add(value)
                true
            }
            preferences[FAVORITE_TRACK_IDS_KEY] = trackIds
        }
        return isFavorite
    }
}

data class PersistedPlaybackSession(
    val trackId: Long,
    val playlistId: Long,
    val positionMs: Long,
    val wasPlaying: Boolean,
)

private val PLAY_MODE_KEY = stringPreferencesKey("playMode")
private val LAST_TRACK_ID_KEY = longPreferencesKey("playback.lastTrackId")
private val LAST_PLAYLIST_ID_KEY = longPreferencesKey("playback.lastPlaylistId")
private val LAST_POSITION_MS_KEY = longPreferencesKey("playback.lastPositionMs")
private val LAST_WAS_PLAYING_KEY = booleanPreferencesKey("playback.lastWasPlaying")
private val FAVORITE_TRACK_IDS_KEY = stringSetPreferencesKey("library.favoriteTrackIds")

package io.github.julystar.musicapp.feature.search.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.julystar.musicapp.feature.search.domain.MAX_SEARCH_HISTORY_SIZE
import io.github.julystar.musicapp.feature.search.domain.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreSearchHistoryRepository(
    private val dataStore: DataStore<Preferences>,
) : SearchHistoryRepository {
    override val history: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[SEARCH_HISTORY_KEY].toSearchHistory()
    }

    override suspend fun remember(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        dataStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY_KEY].toSearchHistory()
            preferences[SEARCH_HISTORY_KEY] = Json.encodeToString(
                current.rememberSearchQuery(normalized)
            )
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY_KEY)
        }
    }
}

internal fun List<String>.rememberSearchQuery(
    query: String,
    limit: Int = MAX_SEARCH_HISTORY_SIZE,
): List<String> {
    val normalized = query.trim()
    if (normalized.isBlank()) return this
    return (listOf(normalized) + filterNot { it.equals(normalized, ignoreCase = true) })
        .distinct()
        .take(limit)
}

private fun String?.toSearchHistory(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return try {
        Json.decodeFromString<List<String>>(this)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(MAX_SEARCH_HISTORY_SIZE)
    } catch (_: SerializationException) {
        emptyList()
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}

private val SEARCH_HISTORY_KEY = stringPreferencesKey("searchHistory")

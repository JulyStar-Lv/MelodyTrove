package com.github.tidetunes.feature.home.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.tidetunes.feature.home.domain.HomePinnedRepository
import com.github.tidetunes.feature.home.domain.PinnedHomeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataStoreHomePinnedRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) : HomePinnedRepository {

    override val pinnedItems: StateFlow<List<PinnedHomeItem>> = dataStore.data
        .map { prefs -> prefs[PINNED_ITEMS_KEY].toPinnedItems() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    override suspend fun pinItem(item: PinnedHomeItem) {
        dataStore.edit { prefs ->
            val current = prefs[PINNED_ITEMS_KEY].toPinnedItems().toMutableList()
            current.removeAll { it.id == item.id }
            current.add(item.copy(order = current.size))
            prefs[PINNED_ITEMS_KEY] = Json.encodeToString(current)
        }
    }

    override suspend fun unpinItem(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[PINNED_ITEMS_KEY].toPinnedItems().toMutableList()
            current.removeAll { it.id == id }
            prefs[PINNED_ITEMS_KEY] = Json.encodeToString(current)
        }
    }

    override suspend fun reorder(orderedIds: List<String>) {
        dataStore.edit { prefs ->
            val current = prefs[PINNED_ITEMS_KEY].toPinnedItems()
            val idToItem = current.associateBy { it.id }
            val reordered = orderedIds.mapIndexed { index, id ->
                idToItem[id]?.copy(order = index)
            }
            if (reordered.all { it != null }) {
                prefs[PINNED_ITEMS_KEY] = Json.encodeToString(reordered.filterNotNull())
            }
        }
    }

    private companion object {
        val PINNED_ITEMS_KEY = stringPreferencesKey("homePinnedItems")

        fun String?.toPinnedItems(): List<PinnedHomeItem> {
            if (isNullOrBlank()) return emptyList()
            return try {
                Json.decodeFromString<List<PinnedHomeItem>>(this)
                    .sortedBy { it.order }
            } catch (_: SerializationException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}

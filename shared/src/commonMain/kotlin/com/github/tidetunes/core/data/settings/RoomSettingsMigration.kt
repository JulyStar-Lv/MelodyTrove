package com.github.tidetunes.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.repository.SettingsMigration
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.SourceAccountDao
import com.github.tidetunes.platform.currentTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class RoomSettingsMigration(
    private val dataStore: DataStore<Preferences>,
    private val sourceAccountDao: SourceAccountDao,
) : SettingsMigration {

    override suspend fun migrate() {
        val snapshot = dataStore.data.first()
        migrateSourceEnabledState(snapshot)
        migrateWebDavRootPaths(snapshot)
        migrateWebDavMetadataScanMode(snapshot)
        migrateRenamedPreferences()
    }

    private suspend fun migrateWebDavMetadataScanMode(preferences: Preferences) {
        if (preferences[WEB_DAV_METADATA_SCAN_MODE_MIGRATED_KEY] == true) return
        val fallback = if (sourceAccountDao.listAll().any { it.providerType == ProviderTypes.WebDav }) {
            MetadataScanMode.Full
        } else {
            MetadataScanMode.Standard
        }
        dataStore.edit { mutable ->
            if (mutable[WEB_DAV_METADATA_SCAN_MODE_KEY] == null) {
                mutable[WEB_DAV_METADATA_SCAN_MODE_KEY] = fallback.name
            }
            mutable[WEB_DAV_METADATA_SCAN_MODE_MIGRATED_KEY] = true
        }
    }

    private suspend fun migrateSourceEnabledState(preferences: Preferences) {
        val now = currentTimeMillis()
        preferences[LOCAL_MUSIC_ENABLED_KEY]?.let { enabled ->
            sourceAccountDao.setEnabledByProviderType(ProviderTypes.Local, enabled, now)
        }
        preferences[WEB_DAV_ENABLED_KEY]?.let { enabled ->
            sourceAccountDao.setEnabledByProviderType(ProviderTypes.WebDav, enabled, now)
        }
    }

    private suspend fun migrateWebDavRootPaths(preferences: Preferences) {
        val legacyValue = preferences[WEB_DAV_ROOT_PATHS_KEY] ?: return
        val rootPaths = legacyValue.decodeRootPaths()
        val accountsById = sourceAccountDao.listAll().associateBy { it.id }
        val now = currentTimeMillis()
        rootPaths.forEach { (legacyAccountId, rootPath) ->
            val id = legacyAccountId
                .removePrefix(STORAGE_ACCOUNT_PREFIX)
                .toLongOrNull()
                ?: return@forEach
            val account = accountsById[id] ?: return@forEach
            if (account.providerType == ProviderTypes.WebDav) {
                sourceAccountDao.setRootPath(id, rootPath.normalizedRootPath(), now)
            }
        }
        dataStore.edit { mutable -> mutable.remove(WEB_DAV_ROOT_PATHS_KEY) }
    }

    private suspend fun migrateRenamedPreferences() {
        dataStore.edit { preferences ->
            if (preferences[AUDIO_FOCUS_MODE_KEY] == null) {
                preferences[ALLOW_MIXED_PLAYBACK_KEY]?.let { allowMixed ->
                    preferences[AUDIO_FOCUS_MODE_KEY] = if (allowMixed) {
                        AudioFocusMode.Mix.name
                    } else {
                        AudioFocusMode.Pause.name
                    }
                }
            }
            if (preferences[MINIMUM_AUDIO_DURATION_MS_KEY] == null) {
                preferences[IGNORE_SHORT_AUDIO_KEY]?.let { ignoreShort ->
                    preferences[MINIMUM_AUDIO_DURATION_MS_KEY] = if (ignoreShort) 30_000L else 0L
                }
            }
            if (preferences[SCAN_SUBDIRECTORIES_KEY] == null) {
                val legacyValue = preferences[LOCAL_SCAN_SUBDIRECTORIES_KEY]
                    ?: preferences[WEB_DAV_SCAN_SUBDIRECTORIES_KEY]
                if (legacyValue != null) preferences[SCAN_SUBDIRECTORIES_KEY] = legacyValue
            }
            preferences.remove(ALLOW_MIXED_PLAYBACK_KEY)
            preferences.remove(IGNORE_SHORT_AUDIO_KEY)
            preferences.remove(LOCAL_SCAN_SUBDIRECTORIES_KEY)
            preferences.remove(WEB_DAV_SCAN_SUBDIRECTORIES_KEY)
            preferences.remove(LOCAL_MUSIC_ENABLED_KEY)
            preferences.remove(WEB_DAV_ENABLED_KEY)
        }
    }
}

private val migrationJson = Json { ignoreUnknownKeys = true }
private val rootPathSerializer = MapSerializer(String.serializer(), String.serializer())

private fun String?.decodeRootPaths(): Map<String, String> {
    if (isNullOrBlank()) return emptyMap()
    return runCatching {
        migrationJson.decodeFromString(rootPathSerializer, this)
    }.getOrDefault(emptyMap())
}

private fun String.normalizedRootPath(): String {
    val trimmed = trim().ifBlank { "/" }
    return if (trimmed.startsWith('/')) trimmed else "/$trimmed"
}

private const val STORAGE_ACCOUNT_PREFIX = "storage:"

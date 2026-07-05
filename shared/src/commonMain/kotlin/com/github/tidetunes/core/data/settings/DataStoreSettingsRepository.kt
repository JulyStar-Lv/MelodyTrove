package com.github.tidetunes.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.normalizeAudioCacheLimitBytes
import com.github.tidetunes.core.domain.repository.SettingsRepository
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[THEME_MODE_KEY].enumOrDefault(AppThemeMode.System),
            dynamicColorEnabled = preferences[DYNAMIC_COLOR_ENABLED_KEY] ?: true,
            languageMode = preferences[LANGUAGE_MODE_KEY].enumOrDefault(AppLanguageMode.System),
            pauseOnDisconnect = preferences[PAUSE_ON_DISCONNECT_KEY] ?: true,
            allowMixedPlayback = preferences[ALLOW_MIXED_PLAYBACK_KEY] ?: false,
            keepScreenOnInPlayer = preferences[KEEP_SCREEN_ON_IN_PLAYER_KEY] ?: false,
            localMusicEnabled = preferences[LOCAL_MUSIC_ENABLED_KEY] ?: true,
            localScanSubdirectories = preferences[LOCAL_SCAN_SUBDIRECTORIES_KEY] ?: true,
            ignoreShortAudio = preferences[IGNORE_SHORT_AUDIO_KEY] ?: true,
            webDavEnabled = preferences[WEB_DAV_ENABLED_KEY] ?: false,
            webDavScanSubdirectories = preferences[WEB_DAV_SCAN_SUBDIRECTORIES_KEY] ?: true,
            webDavRootPaths = preferences[WEB_DAV_ROOT_PATHS_KEY].decodeRootPaths(),
            audioCacheLimitBytes = normalizeAudioCacheLimitBytes(
                preferences[AUDIO_CACHE_LIMIT_BYTES_KEY] ?: AppSettings.Default.audioCacheLimitBytes
            ),
        )
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DYNAMIC_COLOR_ENABLED_KEY] = enabled }
    }

    override suspend fun setLanguageMode(mode: AppLanguageMode) {
        dataStore.edit { preferences -> preferences[LANGUAGE_MODE_KEY] = mode.name }
    }

    override suspend fun setPauseOnDisconnect(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PAUSE_ON_DISCONNECT_KEY] = enabled }
    }

    override suspend fun setAllowMixedPlayback(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[ALLOW_MIXED_PLAYBACK_KEY] = enabled }
    }

    override suspend fun setKeepScreenOnInPlayer(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[KEEP_SCREEN_ON_IN_PLAYER_KEY] = enabled }
    }

    override suspend fun setLocalMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[LOCAL_MUSIC_ENABLED_KEY] = enabled }
    }

    override suspend fun setLocalScanSubdirectories(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[LOCAL_SCAN_SUBDIRECTORIES_KEY] = enabled }
    }

    override suspend fun setIgnoreShortAudio(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[IGNORE_SHORT_AUDIO_KEY] = enabled }
    }

    override suspend fun setWebDavEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[WEB_DAV_ENABLED_KEY] = enabled }
    }

    override suspend fun setWebDavScanSubdirectories(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[WEB_DAV_SCAN_SUBDIRECTORIES_KEY] = enabled }
    }

    override suspend fun setWebDavRootPath(accountId: SourceAccountId, rootPath: String) {
        dataStore.edit { preferences ->
            val next = preferences[WEB_DAV_ROOT_PATHS_KEY]
                .decodeRootPaths()
                .toMutableMap()
                .apply { put(accountId.value, rootPath.normalizedRootPath()) }
            preferences[WEB_DAV_ROOT_PATHS_KEY] = next.encodeRootPaths()
        }
    }

    override suspend fun removeWebDavRootPath(accountId: SourceAccountId) {
        dataStore.edit { preferences ->
            val next = preferences[WEB_DAV_ROOT_PATHS_KEY]
                .decodeRootPaths()
                .toMutableMap()
                .apply { remove(accountId.value) }
            preferences[WEB_DAV_ROOT_PATHS_KEY] = next.encodeRootPaths()
        }
    }

    override suspend fun setAudioCacheLimitBytes(bytes: Long) {
        dataStore.edit { preferences ->
            preferences[AUDIO_CACHE_LIMIT_BYTES_KEY] = normalizeAudioCacheLimitBytes(bytes)
        }
    }
}

private val settingsJson = Json { ignoreUnknownKeys = true }
private val rootPathSerializer = MapSerializer(String.serializer(), String.serializer())

private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { value -> value.name == this } ?: default
}

private fun String?.decodeRootPaths(): Map<String, String> {
    if (isNullOrBlank()) return emptyMap()
    return runCatching {
        settingsJson.decodeFromString(rootPathSerializer, this)
            .mapValues { (_, value) -> value.normalizedRootPath() }
    }.getOrDefault(emptyMap())
}

private fun Map<String, String>.encodeRootPaths(): String {
    return settingsJson.encodeToString(rootPathSerializer, this)
}

private fun String.normalizedRootPath(): String {
    val trimmed = trim().ifBlank { "/" }
    return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
}

private val THEME_MODE_KEY = stringPreferencesKey("settings.themeMode")
private val DYNAMIC_COLOR_ENABLED_KEY = booleanPreferencesKey("settings.dynamicColorEnabled")
private val LANGUAGE_MODE_KEY = stringPreferencesKey("settings.languageMode")
private val PAUSE_ON_DISCONNECT_KEY = booleanPreferencesKey("settings.pauseOnDisconnect")
private val ALLOW_MIXED_PLAYBACK_KEY = booleanPreferencesKey("settings.allowMixedPlayback")
private val KEEP_SCREEN_ON_IN_PLAYER_KEY = booleanPreferencesKey("settings.keepScreenOnInPlayer")
private val LOCAL_MUSIC_ENABLED_KEY = booleanPreferencesKey("settings.localMusicEnabled")
private val LOCAL_SCAN_SUBDIRECTORIES_KEY = booleanPreferencesKey("settings.localScanSubdirectories")
private val IGNORE_SHORT_AUDIO_KEY = booleanPreferencesKey("settings.ignoreShortAudio")
private val WEB_DAV_ENABLED_KEY = booleanPreferencesKey("settings.webDavEnabled")
private val WEB_DAV_SCAN_SUBDIRECTORIES_KEY = booleanPreferencesKey("settings.webDavScanSubdirectories")
private val WEB_DAV_ROOT_PATHS_KEY = stringPreferencesKey("settings.webDavRootPaths")
private val AUDIO_CACHE_LIMIT_BYTES_KEY = longPreferencesKey("settings.audioCacheLimitBytes")

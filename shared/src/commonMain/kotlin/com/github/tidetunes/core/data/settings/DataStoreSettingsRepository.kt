package com.github.tidetunes.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.normalizeAudioCacheLimitBytes
import com.github.tidetunes.core.domain.model.normalizeAudioPreloadBytes
import com.github.tidetunes.core.domain.model.normalizeConnectionTimeoutSeconds
import com.github.tidetunes.core.domain.model.normalizeImageCacheLimitBytes
import com.github.tidetunes.core.domain.model.normalizeMinimumAudioDurationMs
import com.github.tidetunes.core.domain.model.normalizeNetworkRetryCount
import com.github.tidetunes.core.domain.repository.SettingsRepository
import com.github.tidetunes.platform.applyAppLanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val applyLanguageMode: (AppLanguageMode) -> Unit = ::applyAppLanguageMode,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[THEME_MODE_KEY].enumOrDefault(AppThemeMode.Dark),
            dynamicColorEnabled = preferences[DYNAMIC_COLOR_ENABLED_KEY] ?: true,
            languageMode = preferences[LANGUAGE_MODE_KEY].enumOrDefault(AppLanguageMode.System),
            audioFocusMode = preferences[AUDIO_FOCUS_MODE_KEY]
                .enumOrNull<AudioFocusMode>()
                ?: preferences[ALLOW_MIXED_PLAYBACK_KEY].toLegacyAudioFocusMode()
                ?: AppSettings.Default.audioFocusMode,
            pauseOnDisconnect = preferences[PAUSE_ON_DISCONNECT_KEY] ?: true,
            gaplessPlaybackEnabled = preferences[GAPLESS_PLAYBACK_ENABLED_KEY] ?: false,
            retryPlaybackOnFailure = preferences[RETRY_PLAYBACK_ON_FAILURE_KEY] ?: true,
            resumePlaybackAfterNetworkRecovery =
                preferences[RESUME_PLAYBACK_AFTER_NETWORK_RECOVERY_KEY] ?: true,
            keepScreenOnInPlayer = preferences[KEEP_SCREEN_ON_IN_PLAYER_KEY] ?: false,
            autoScanMode = preferences[AUTO_SCAN_MODE_KEY].enumOrDefault(AutoScanMode.Off),
            backgroundScanEnabled = preferences[BACKGROUND_SCAN_ENABLED_KEY] ?: false,
            scanOnlyOnUnmeteredNetwork =
                preferences[SCAN_ONLY_ON_UNMETERED_NETWORK_KEY] ?: true,
            scanSubdirectories = preferences[SCAN_SUBDIRECTORIES_KEY]
                ?: preferences[LOCAL_SCAN_SUBDIRECTORIES_KEY]
                ?: preferences[WEB_DAV_SCAN_SUBDIRECTORIES_KEY]
                ?: true,
            webDavMetadataScanMode = preferences[WEB_DAV_METADATA_SCAN_MODE_KEY]
                .enumOrDefault(MetadataScanMode.Standard),
            minimumAudioDurationMs = normalizeMinimumAudioDurationMs(
                preferences[MINIMUM_AUDIO_DURATION_MS_KEY]
                    ?: preferences[IGNORE_SHORT_AUDIO_KEY].toLegacyMinimumDurationMs()
                    ?: AppSettings.Default.minimumAudioDurationMs,
            ),
            missingFilePolicy = preferences[MISSING_FILE_POLICY_KEY]
                .enumOrDefault(MissingFilePolicy.MarkUnavailable),
            duplicateTrackPolicy = preferences[DUPLICATE_TRACK_POLICY_KEY]
                .enumOrDefault(DuplicateTrackPolicy.SeparateBySource),
            allowMeteredStreaming = preferences[ALLOW_METERED_STREAMING_KEY] ?: true,
            backgroundSyncOnlyOnUnmeteredNetwork =
                preferences[BACKGROUND_SYNC_ONLY_ON_UNMETERED_NETWORK_KEY] ?: true,
            networkRetryCount = normalizeNetworkRetryCount(
                preferences[NETWORK_RETRY_COUNT_KEY] ?: AppSettings.Default.networkRetryCount,
            ),
            connectionTimeoutSeconds = normalizeConnectionTimeoutSeconds(
                preferences[CONNECTION_TIMEOUT_SECONDS_KEY]
                    ?: AppSettings.Default.connectionTimeoutSeconds,
            ),
            audioPreloadBytes = normalizeAudioPreloadBytes(
                preferences[AUDIO_PRELOAD_BYTES_KEY] ?: AppSettings.Default.audioPreloadBytes,
            ),
            audioCacheLimitBytes = normalizeAudioCacheLimitBytes(
                preferences[AUDIO_CACHE_LIMIT_BYTES_KEY] ?: AppSettings.Default.audioCacheLimitBytes,
            ),
            imageCacheLimitBytes = normalizeImageCacheLimitBytes(
                preferences[IMAGE_CACHE_LIMIT_BYTES_KEY] ?: AppSettings.Default.imageCacheLimitBytes,
            ),
        )
    }

    override suspend fun setThemeMode(mode: AppThemeMode) = set(THEME_MODE_KEY, mode.name)

    override suspend fun setDynamicColorEnabled(enabled: Boolean) =
        set(DYNAMIC_COLOR_ENABLED_KEY, enabled)

    override suspend fun setLanguageMode(mode: AppLanguageMode) {
        set(LANGUAGE_MODE_KEY, mode.name)
        applyLanguageMode(mode)
    }

    override suspend fun setAudioFocusMode(mode: AudioFocusMode) {
        dataStore.edit { preferences ->
            preferences[AUDIO_FOCUS_MODE_KEY] = mode.name
            preferences.remove(ALLOW_MIXED_PLAYBACK_KEY)
        }
    }

    override suspend fun setPauseOnDisconnect(enabled: Boolean) =
        set(PAUSE_ON_DISCONNECT_KEY, enabled)

    override suspend fun setGaplessPlaybackEnabled(enabled: Boolean) =
        set(GAPLESS_PLAYBACK_ENABLED_KEY, enabled)

    override suspend fun setRetryPlaybackOnFailure(enabled: Boolean) =
        set(RETRY_PLAYBACK_ON_FAILURE_KEY, enabled)

    override suspend fun setResumePlaybackAfterNetworkRecovery(enabled: Boolean) =
        set(RESUME_PLAYBACK_AFTER_NETWORK_RECOVERY_KEY, enabled)

    override suspend fun setKeepScreenOnInPlayer(enabled: Boolean) =
        set(KEEP_SCREEN_ON_IN_PLAYER_KEY, enabled)

    override suspend fun setAutoScanMode(mode: AutoScanMode) = set(AUTO_SCAN_MODE_KEY, mode.name)

    override suspend fun setBackgroundScanEnabled(enabled: Boolean) =
        set(BACKGROUND_SCAN_ENABLED_KEY, enabled)

    override suspend fun setScanOnlyOnUnmeteredNetwork(enabled: Boolean) =
        set(SCAN_ONLY_ON_UNMETERED_NETWORK_KEY, enabled)

    override suspend fun setScanSubdirectories(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SCAN_SUBDIRECTORIES_KEY] = enabled
            preferences.remove(LOCAL_SCAN_SUBDIRECTORIES_KEY)
            preferences.remove(WEB_DAV_SCAN_SUBDIRECTORIES_KEY)
        }
    }

    override suspend fun setWebDavMetadataScanMode(mode: MetadataScanMode) =
        set(WEB_DAV_METADATA_SCAN_MODE_KEY, mode.name)

    override suspend fun setMinimumAudioDurationMs(value: Long) {
        dataStore.edit { preferences ->
            preferences[MINIMUM_AUDIO_DURATION_MS_KEY] = normalizeMinimumAudioDurationMs(value)
            preferences.remove(IGNORE_SHORT_AUDIO_KEY)
        }
    }

    override suspend fun setMissingFilePolicy(policy: MissingFilePolicy) =
        set(MISSING_FILE_POLICY_KEY, policy.name)

    override suspend fun setDuplicateTrackPolicy(policy: DuplicateTrackPolicy) =
        set(DUPLICATE_TRACK_POLICY_KEY, policy.name)

    override suspend fun setAllowMeteredStreaming(enabled: Boolean) =
        set(ALLOW_METERED_STREAMING_KEY, enabled)

    override suspend fun setBackgroundSyncOnlyOnUnmeteredNetwork(enabled: Boolean) =
        set(BACKGROUND_SYNC_ONLY_ON_UNMETERED_NETWORK_KEY, enabled)

    override suspend fun setNetworkRetryCount(value: Int) =
        set(NETWORK_RETRY_COUNT_KEY, normalizeNetworkRetryCount(value))

    override suspend fun setConnectionTimeoutSeconds(value: Int) =
        set(CONNECTION_TIMEOUT_SECONDS_KEY, normalizeConnectionTimeoutSeconds(value))

    override suspend fun setAudioPreloadBytes(bytes: Long) =
        set(AUDIO_PRELOAD_BYTES_KEY, normalizeAudioPreloadBytes(bytes))

    override suspend fun setAudioCacheLimitBytes(bytes: Long) =
        set(AUDIO_CACHE_LIMIT_BYTES_KEY, normalizeAudioCacheLimitBytes(bytes))

    override suspend fun setImageCacheLimitBytes(bytes: Long) =
        set(IMAGE_CACHE_LIMIT_BYTES_KEY, normalizeImageCacheLimitBytes(bytes))

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            SETTINGS_KEYS.forEach(preferences::removeUntyped)
        }
        applyLanguageMode(AppSettings.Default.languageMode)
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences -> preferences[key] = value }
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutablePreferences.removeUntyped(key: Preferences.Key<*>) {
    remove(key as Preferences.Key<Any>)
}

private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? {
    return enumValues<T>().firstOrNull { value -> value.name == this }
}

private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T {
    return enumOrNull() ?: default
}

private fun Boolean?.toLegacyAudioFocusMode(): AudioFocusMode? = when (this) {
    true -> AudioFocusMode.Mix
    false -> AudioFocusMode.Pause
    null -> null
}

private fun Boolean?.toLegacyMinimumDurationMs(): Long? = when (this) {
    true -> 30_000L
    false -> 0L
    null -> null
}

internal val THEME_MODE_KEY = stringPreferencesKey("settings.themeMode")
internal val DYNAMIC_COLOR_ENABLED_KEY = booleanPreferencesKey("settings.dynamicColorEnabled")
internal val LANGUAGE_MODE_KEY = stringPreferencesKey("settings.languageMode")
internal val AUDIO_FOCUS_MODE_KEY = stringPreferencesKey("settings.audioFocusMode")
internal val PAUSE_ON_DISCONNECT_KEY = booleanPreferencesKey("settings.pauseOnDisconnect")
internal val GAPLESS_PLAYBACK_ENABLED_KEY = booleanPreferencesKey("settings.gaplessPlaybackEnabled")
internal val RETRY_PLAYBACK_ON_FAILURE_KEY = booleanPreferencesKey("settings.retryPlaybackOnFailure")
internal val RESUME_PLAYBACK_AFTER_NETWORK_RECOVERY_KEY =
    booleanPreferencesKey("settings.resumePlaybackAfterNetworkRecovery")
internal val KEEP_SCREEN_ON_IN_PLAYER_KEY = booleanPreferencesKey("settings.keepScreenOnInPlayer")
internal val AUTO_SCAN_MODE_KEY = stringPreferencesKey("settings.autoScanMode")
internal val BACKGROUND_SCAN_ENABLED_KEY = booleanPreferencesKey("settings.backgroundScanEnabled")
internal val SCAN_ONLY_ON_UNMETERED_NETWORK_KEY =
    booleanPreferencesKey("settings.scanOnlyOnUnmeteredNetwork")
internal val SCAN_SUBDIRECTORIES_KEY = booleanPreferencesKey("settings.scanSubdirectories")
internal val WEB_DAV_METADATA_SCAN_MODE_KEY =
    stringPreferencesKey("settings.webDavMetadataScanMode")
internal val WEB_DAV_METADATA_SCAN_MODE_MIGRATED_KEY =
    booleanPreferencesKey("settings.webDavMetadataScanModeMigrated")
internal val MINIMUM_AUDIO_DURATION_MS_KEY = longPreferencesKey("settings.minimumAudioDurationMs")
internal val MISSING_FILE_POLICY_KEY = stringPreferencesKey("settings.missingFilePolicy")
internal val DUPLICATE_TRACK_POLICY_KEY = stringPreferencesKey("settings.duplicateTrackPolicy")
internal val ALLOW_METERED_STREAMING_KEY = booleanPreferencesKey("settings.allowMeteredStreaming")
internal val BACKGROUND_SYNC_ONLY_ON_UNMETERED_NETWORK_KEY =
    booleanPreferencesKey("settings.backgroundSyncOnlyOnUnmeteredNetwork")
internal val NETWORK_RETRY_COUNT_KEY = intPreferencesKey("settings.networkRetryCount")
internal val CONNECTION_TIMEOUT_SECONDS_KEY = intPreferencesKey("settings.connectionTimeoutSeconds")
internal val AUDIO_PRELOAD_BYTES_KEY = longPreferencesKey("settings.audioPreloadBytes")
internal val AUDIO_CACHE_LIMIT_BYTES_KEY = longPreferencesKey("settings.audioCacheLimitBytes")
internal val IMAGE_CACHE_LIMIT_BYTES_KEY = longPreferencesKey("settings.imageCacheLimitBytes")

internal val ALLOW_MIXED_PLAYBACK_KEY = booleanPreferencesKey("settings.allowMixedPlayback")
internal val LOCAL_MUSIC_ENABLED_KEY = booleanPreferencesKey("settings.localMusicEnabled")
internal val LOCAL_SCAN_SUBDIRECTORIES_KEY = booleanPreferencesKey("settings.localScanSubdirectories")
internal val IGNORE_SHORT_AUDIO_KEY = booleanPreferencesKey("settings.ignoreShortAudio")
internal val WEB_DAV_ENABLED_KEY = booleanPreferencesKey("settings.webDavEnabled")
internal val WEB_DAV_SCAN_SUBDIRECTORIES_KEY = booleanPreferencesKey("settings.webDavScanSubdirectories")
internal val WEB_DAV_ROOT_PATHS_KEY = stringPreferencesKey("settings.webDavRootPaths")

private val SETTINGS_KEYS = setOf(
    THEME_MODE_KEY,
    DYNAMIC_COLOR_ENABLED_KEY,
    LANGUAGE_MODE_KEY,
    AUDIO_FOCUS_MODE_KEY,
    PAUSE_ON_DISCONNECT_KEY,
    GAPLESS_PLAYBACK_ENABLED_KEY,
    RETRY_PLAYBACK_ON_FAILURE_KEY,
    RESUME_PLAYBACK_AFTER_NETWORK_RECOVERY_KEY,
    KEEP_SCREEN_ON_IN_PLAYER_KEY,
    AUTO_SCAN_MODE_KEY,
    BACKGROUND_SCAN_ENABLED_KEY,
    SCAN_ONLY_ON_UNMETERED_NETWORK_KEY,
    SCAN_SUBDIRECTORIES_KEY,
    WEB_DAV_METADATA_SCAN_MODE_KEY,
    MINIMUM_AUDIO_DURATION_MS_KEY,
    MISSING_FILE_POLICY_KEY,
    DUPLICATE_TRACK_POLICY_KEY,
    ALLOW_METERED_STREAMING_KEY,
    BACKGROUND_SYNC_ONLY_ON_UNMETERED_NETWORK_KEY,
    NETWORK_RETRY_COUNT_KEY,
    CONNECTION_TIMEOUT_SECONDS_KEY,
    AUDIO_PRELOAD_BYTES_KEY,
    AUDIO_CACHE_LIMIT_BYTES_KEY,
    IMAGE_CACHE_LIMIT_BYTES_KEY,
    ALLOW_MIXED_PLAYBACK_KEY,
    LOCAL_MUSIC_ENABLED_KEY,
    LOCAL_SCAN_SUBDIRECTORIES_KEY,
    IGNORE_SHORT_AUDIO_KEY,
    WEB_DAV_ENABLED_KEY,
    WEB_DAV_SCAN_SUBDIRECTORIES_KEY,
)

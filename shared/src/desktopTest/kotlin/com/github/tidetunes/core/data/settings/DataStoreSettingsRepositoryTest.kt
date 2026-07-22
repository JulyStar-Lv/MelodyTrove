package com.github.tidetunes.core.data.settings

import androidx.datastore.preferences.core.edit
import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.AutoScanMode
import com.github.tidetunes.core.domain.model.DuplicateTrackPolicy
import com.github.tidetunes.core.domain.model.MAX_AUDIO_CACHE_LIMIT_BYTES
import com.github.tidetunes.core.domain.model.MAX_IMAGE_CACHE_LIMIT_BYTES
import com.github.tidetunes.core.domain.model.LyricTextAlignment
import com.github.tidetunes.core.domain.model.MissingFilePolicy
import com.github.tidetunes.core.domain.model.MetadataScanMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreSettingsRepositoryTest {
    @Test
    fun persistsAndReloadsSettings() = withRepository { dataStore, repository ->
        assertEquals(AppSettings.Default, repository.settingsValue())

        repository.setThemeMode(AppThemeMode.Light)
        repository.setDynamicColorEnabled(false)
        repository.setLanguageMode(AppLanguageMode.English)
        repository.setAudioFocusMode(AudioFocusMode.Duck)
        repository.setPauseOnDisconnect(false)
        repository.setGaplessPlaybackEnabled(true)
        repository.setRetryPlaybackOnFailure(false)
        repository.setResumePlaybackAfterNetworkRecovery(false)
        repository.setKeepScreenOnInPlayer(true)
        repository.setLyricTextAlignment(LyricTextAlignment.Right)
        repository.setLyricPrimaryFontScalePercent(125)
        repository.setLyricPrimaryFontSizeSp(42)
        repository.setLyricSecondaryFontScalePercent(135)
        repository.setLyricSecondaryFontSizeSp(24)
        repository.setLyricTranslationVisible(false)
        repository.setLyricWordLiftEnabled(false)
        repository.setLyricBlurEffectEnabled(false)
        repository.setLyricPerspectiveEffectEnabled(true)
        repository.setLyricPerspectiveAngleDegrees(35)
        repository.setLyricTapToSeekEnabled(false)
        repository.setAutoScanMode(AutoScanMode.OnStartup)
        repository.setBackgroundScanEnabled(true)
        repository.setScanOnlyOnUnmeteredNetwork(false)
        repository.setScanSubdirectories(false)
        repository.setWebDavMetadataScanMode(MetadataScanMode.Full)
        repository.setMinimumAudioDurationMs(47_000L)
        repository.setMissingFilePolicy(MissingFilePolicy.RemoveOnScan)
        repository.setDuplicateTrackPolicy(DuplicateTrackPolicy.KeepAll)
        repository.setAllowMeteredStreaming(false)
        repository.setBackgroundSyncOnlyOnUnmeteredNetwork(false)
        repository.setNetworkRetryCount(4)
        repository.setConnectionTimeoutSeconds(45)
        repository.setAudioPreloadBytes(8L * 1024L * 1024L)
        repository.setAudioCacheLimitBytes(512L * 1024L * 1024L)
        repository.setImageCacheLimitBytes(128L * 1024L * 1024L)
        repository.setPlayerInteractionSettings(
            repository.settingsValue().playerInteraction.copy(immersiveAlbumCover = true)
        )

        val settings = DataStoreSettingsRepository(dataStore).settingsValue()
        assertEquals(AppThemeMode.Light, settings.themeMode)
        assertFalse(settings.dynamicColorEnabled)
        assertEquals(AppLanguageMode.English, settings.languageMode)
        assertEquals(AudioFocusMode.Duck, settings.audioFocusMode)
        assertFalse(settings.pauseOnDisconnect)
        assertTrue(settings.gaplessPlaybackEnabled)
        assertFalse(settings.retryPlaybackOnFailure)
        assertFalse(settings.resumePlaybackAfterNetworkRecovery)
        assertTrue(settings.keepScreenOnInPlayer)
        assertEquals(LyricTextAlignment.Right, settings.lyrics.textAlignment)
        assertEquals(125, settings.lyrics.primaryFontScalePercent)
        assertEquals(42, settings.lyrics.primaryFontSizeSp)
        assertEquals(135, settings.lyrics.secondaryFontScalePercent)
        assertEquals(24, settings.lyrics.secondaryFontSizeSp)
        assertFalse(settings.lyrics.showTranslation)
        assertFalse(settings.lyrics.wordLiftEnabled)
        assertFalse(settings.lyrics.blurEffectEnabled)
        assertTrue(settings.lyrics.perspectiveEffectEnabled)
        assertEquals(35, settings.lyrics.perspectiveAngleDegrees)
        assertFalse(settings.lyrics.tapToSeekEnabled)
        assertEquals(AutoScanMode.OnStartup, settings.autoScanMode)
        assertTrue(settings.backgroundScanEnabled)
        assertFalse(settings.scanOnlyOnUnmeteredNetwork)
        assertFalse(settings.scanSubdirectories)
        assertEquals(MetadataScanMode.Full, settings.webDavMetadataScanMode)
        assertEquals(47_000L, settings.minimumAudioDurationMs)
        assertEquals(MissingFilePolicy.RemoveOnScan, settings.missingFilePolicy)
        assertEquals(DuplicateTrackPolicy.KeepAll, settings.duplicateTrackPolicy)
        assertFalse(settings.allowMeteredStreaming)
        assertFalse(settings.backgroundSyncOnlyOnUnmeteredNetwork)
        assertEquals(4, settings.networkRetryCount)
        assertEquals(45, settings.connectionTimeoutSeconds)
        assertEquals(8L * 1024L * 1024L, settings.audioPreloadBytes)
        assertEquals(512L * 1024L * 1024L, settings.audioCacheLimitBytes)
        assertEquals(128L * 1024L * 1024L, settings.imageCacheLimitBytes)
        assertTrue(settings.playerInteraction.immersiveAlbumCover)
    }

    @Test
    fun migratesLegacyPlaybackAndScanValues() = withRepository { dataStore, repository ->
        dataStore.edit { preferences ->
            preferences[ALLOW_MIXED_PLAYBACK_KEY] = true
            preferences[IGNORE_SHORT_AUDIO_KEY] = false
            preferences[LOCAL_SCAN_SUBDIRECTORIES_KEY] = false
        }

        val migrated = repository.settingsValue()
        assertEquals(AudioFocusMode.Mix, migrated.audioFocusMode)
        assertEquals(0L, migrated.minimumAudioDurationMs)
        assertFalse(migrated.scanSubdirectories)

        dataStore.edit { preferences ->
            preferences[ALLOW_MIXED_PLAYBACK_KEY] = false
            preferences[IGNORE_SHORT_AUDIO_KEY] = true
        }
        val second = repository.settingsValue()
        assertEquals(AudioFocusMode.Pause, second.audioFocusMode)
        assertEquals(30_000L, second.minimumAudioDurationMs)
    }

    @Test
    fun invalidValuesFallBackOrClampAndResetClearsSettings() = withRepository { dataStore, repository ->
        dataStore.edit { preferences ->
            preferences[AUDIO_FOCUS_MODE_KEY] = "invalid"
            preferences[AUTO_SCAN_MODE_KEY] = "invalid"
            preferences[WEB_DAV_METADATA_SCAN_MODE_KEY] = "invalid"
            preferences[NETWORK_RETRY_COUNT_KEY] = 99
            preferences[CONNECTION_TIMEOUT_SECONDS_KEY] = -1
            preferences[AUDIO_CACHE_LIMIT_BYTES_KEY] = Long.MAX_VALUE
            preferences[IMAGE_CACHE_LIMIT_BYTES_KEY] = Long.MAX_VALUE
            preferences[LYRIC_TEXT_ALIGNMENT_KEY] = "invalid"
            preferences[LYRIC_PRIMARY_FONT_SCALE_PERCENT_KEY] = Int.MAX_VALUE
            preferences[LYRIC_PRIMARY_FONT_SIZE_SP_KEY] = Int.MAX_VALUE
            preferences[LYRIC_SECONDARY_FONT_SCALE_PERCENT_KEY] = Int.MIN_VALUE
            preferences[LYRIC_SECONDARY_FONT_SIZE_SP_KEY] = Int.MIN_VALUE
            preferences[LYRIC_PERSPECTIVE_ANGLE_DEGREES_KEY] = Int.MAX_VALUE
        }

        val normalized = repository.settingsValue()
        assertEquals(AudioFocusMode.Pause, normalized.audioFocusMode)
        assertEquals(AutoScanMode.Off, normalized.autoScanMode)
        assertEquals(MetadataScanMode.Standard, normalized.webDavMetadataScanMode)
        assertEquals(5, normalized.networkRetryCount)
        assertEquals(5, normalized.connectionTimeoutSeconds)
        assertEquals(MAX_AUDIO_CACHE_LIMIT_BYTES, normalized.audioCacheLimitBytes)
        assertEquals(MAX_IMAGE_CACHE_LIMIT_BYTES, normalized.imageCacheLimitBytes)
        assertEquals(LyricTextAlignment.Left, normalized.lyrics.textAlignment)
        assertEquals(175, normalized.lyrics.primaryFontScalePercent)
        assertEquals(54, normalized.lyrics.primaryFontSizeSp)
        assertEquals(75, normalized.lyrics.secondaryFontScalePercent)
        assertEquals(12, normalized.lyrics.secondaryFontSizeSp)
        assertEquals(45, normalized.lyrics.perspectiveAngleDegrees)

        repository.setThemeMode(AppThemeMode.Light)
        repository.resetToDefaults()
        assertEquals(AppSettings.Default, repository.settingsValue())
    }

    private fun withRepository(
        block: suspend (
            androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
            DataStoreSettingsRepository,
        ) -> Unit,
    ) = runBlocking {
        val file = File.createTempFile("tidetunes-settings-", ".preferences_pb").apply { delete() }
        try {
            val dataStore = createAppDataStore { file.absolutePath.toPath() }
            block(dataStore, DataStoreSettingsRepository(dataStore, applyLanguageMode = {}))
        } finally {
            file.delete()
        }
    }

    private suspend fun DataStoreSettingsRepository.settingsValue(): AppSettings {
        return withTimeout(5_000) { settings.first() }
    }
}

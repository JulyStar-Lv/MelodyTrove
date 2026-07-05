package com.github.tidetunes.core.data.settings

import com.github.tidetunes.core.data.datastore.createAppDataStore
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppSettings
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DataStoreSettingsRepositoryTest {
    @Test
    fun persistsSettingsInDataStore() = runBlocking {
        val file = File.createTempFile("tidetunes-settings-", ".preferences_pb").apply {
            delete()
        }

        try {
            val dataStore = createAppDataStore { file.absolutePath.toPath() }
            val repository = DataStoreSettingsRepository(dataStore)

            assertEquals(
                AppSettings.Default,
                withTimeout(5_000) { repository.settings.first() },
            )

            repository.setThemeMode(AppThemeMode.Dark)
            repository.setDynamicColorEnabled(false)
            repository.setLanguageMode(AppLanguageMode.English)
            repository.setPauseOnDisconnect(false)
            repository.setAllowMixedPlayback(true)
            repository.setKeepScreenOnInPlayer(true)
            repository.setLocalMusicEnabled(false)
            repository.setLocalScanSubdirectories(false)
            repository.setIgnoreShortAudio(false)
            repository.setWebDavEnabled(true)
            repository.setWebDavScanSubdirectories(false)
            repository.setWebDavRootPath(SourceAccountId("storage:2"), "/Music")
            repository.setAudioCacheLimitBytes(512L * 1024L * 1024L)

            val reloaded = DataStoreSettingsRepository(dataStore)
            val settings = withTimeout(5_000) { reloaded.settings.first() }
            assertEquals(AppThemeMode.Dark, settings.themeMode)
            assertFalse(settings.dynamicColorEnabled)
            assertEquals(AppLanguageMode.English, settings.languageMode)
            assertFalse(settings.pauseOnDisconnect)
            assertEquals(true, settings.allowMixedPlayback)
            assertEquals(true, settings.keepScreenOnInPlayer)
            assertFalse(settings.localMusicEnabled)
            assertFalse(settings.localScanSubdirectories)
            assertFalse(settings.ignoreShortAudio)
            assertEquals(true, settings.webDavEnabled)
            assertFalse(settings.webDavScanSubdirectories)
            assertEquals("/Music", settings.webDavRootPaths["storage:2"])
            assertEquals(512L * 1024L * 1024L, settings.audioCacheLimitBytes)
        } finally {
            file.delete()
        }
    }
}

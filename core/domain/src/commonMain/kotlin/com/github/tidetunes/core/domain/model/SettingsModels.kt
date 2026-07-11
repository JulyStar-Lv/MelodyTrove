package com.github.tidetunes.core.domain.model

enum class AppThemeMode {
    System,
    Light,
    Dark,
}

enum class AppLanguageMode {
    System,
    Chinese,
    English,
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val dynamicColorEnabled: Boolean = true,
    val languageMode: AppLanguageMode = AppLanguageMode.System,
    val pauseOnDisconnect: Boolean = true,
    val allowMixedPlayback: Boolean = false,
    val keepScreenOnInPlayer: Boolean = false,
    val localMusicEnabled: Boolean = true,
    val localScanSubdirectories: Boolean = true,
    val ignoreShortAudio: Boolean = true,
    val webDavEnabled: Boolean = false,
    val webDavScanSubdirectories: Boolean = true,
    val webDavRootPaths: Map<String, String> = emptyMap(),
    val audioCacheLimitBytes: Long = DEFAULT_AUDIO_CACHE_LIMIT_BYTES,
) {
    companion object {
        val Default = AppSettings()
    }
}

data class SettingsCapabilities(
    val dynamicColorSupported: Boolean = false,
)

data class StorageUsage(
    val audioBytes: Long? = null,
    val imageBytes: Long? = null,
    val databaseBytes: Long? = null,
    val logBytes: Long? = null,
    val totalBytes: Long? = null,
) {
    companion object {
        val Unknown = StorageUsage()
    }
}

data class LocalMusicDirectory(
    val id: String,
    val accountId: SourceAccountId,
    val displayName: String,
    val path: String,
    val lastScannedAtEpochMs: Long?,
)

const val AUDIO_CACHE_LIMIT_DISABLED_BYTES = 0L
const val DEFAULT_AUDIO_CACHE_LIMIT_BYTES = 1_073_741_824L
const val MAX_AUDIO_CACHE_LIMIT_BYTES = 10_737_418_240L
const val MIN_SCANNED_AUDIO_DURATION_MS = 30_000L

val SUPPORTED_AUDIO_EXTENSIONS = listOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav")
val DEFAULT_IGNORED_SOURCE_DIRECTORIES = listOf(".cache", ".trash", "@eaDir", "__MACOSX")

fun normalizeAudioCacheLimitBytes(bytes: Long): Long {
    return bytes.coerceIn(AUDIO_CACHE_LIMIT_DISABLED_BYTES, MAX_AUDIO_CACHE_LIMIT_BYTES)
}

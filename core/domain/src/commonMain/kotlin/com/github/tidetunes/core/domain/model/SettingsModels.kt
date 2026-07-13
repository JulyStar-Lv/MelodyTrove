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

enum class AudioFocusMode {
    Pause,
    Duck,
    Mix,
}

enum class AutoScanMode {
    Off,
    OnStartup,
    Periodic,
}

enum class MissingFilePolicy {
    MarkUnavailable,
    RemoveOnScan,
}

enum class DuplicateTrackPolicy {
    KeepAll,
    SeparateBySource,
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.Dark,
    val dynamicColorEnabled: Boolean = true,
    val languageMode: AppLanguageMode = AppLanguageMode.System,
    val audioFocusMode: AudioFocusMode = AudioFocusMode.Pause,
    val pauseOnDisconnect: Boolean = true,
    val gaplessPlaybackEnabled: Boolean = false,
    val retryPlaybackOnFailure: Boolean = true,
    val resumePlaybackAfterNetworkRecovery: Boolean = true,
    val keepScreenOnInPlayer: Boolean = false,
    val autoScanMode: AutoScanMode = AutoScanMode.Off,
    val backgroundScanEnabled: Boolean = false,
    val scanOnlyOnUnmeteredNetwork: Boolean = true,
    val scanSubdirectories: Boolean = true,
    val webDavMetadataScanMode: MetadataScanMode = MetadataScanMode.Standard,
    val minimumAudioDurationMs: Long = DEFAULT_MINIMUM_AUDIO_DURATION_MS,
    val missingFilePolicy: MissingFilePolicy = MissingFilePolicy.MarkUnavailable,
    val duplicateTrackPolicy: DuplicateTrackPolicy = DuplicateTrackPolicy.SeparateBySource,
    val allowMeteredStreaming: Boolean = true,
    val backgroundSyncOnlyOnUnmeteredNetwork: Boolean = true,
    val networkRetryCount: Int = DEFAULT_NETWORK_RETRY_COUNT,
    val connectionTimeoutSeconds: Int = DEFAULT_CONNECTION_TIMEOUT_SECONDS,
    val audioPreloadBytes: Long = DEFAULT_AUDIO_PRELOAD_BYTES,
    val audioCacheLimitBytes: Long = DEFAULT_AUDIO_CACHE_LIMIT_BYTES,
    val imageCacheLimitBytes: Long = DEFAULT_IMAGE_CACHE_LIMIT_BYTES,
) {
    companion object {
        val Default = AppSettings()
    }
}

fun AppSettings.metadataScanModeFor(isWebDav: Boolean): MetadataScanMode {
    return if (isWebDav) webDavMetadataScanMode else MetadataScanMode.Full
}

data class SettingsCapabilities(
    val dynamicColorSupported: Boolean = false,
    val backgroundScanSupported: Boolean = false,
    val customMusicDirectorySupported: Boolean = false,
    val customCacheDirectorySupported: Boolean = false,
    val secureCredentialStoreSupported: Boolean = false,
    val systemEqualizerSupported: Boolean = false,
    val floatingLyricsSupported: Boolean = false,
    val inAppUpdateSupported: Boolean = false,
    val desktopMediaKeysSupported: Boolean = false,
    val audioFocusSupported: Boolean = false,
    val deviceDisconnectSupported: Boolean = false,
    val gaplessPlaybackSupported: Boolean = false,
    val networkStatusSupported: Boolean = false,
    val audioPreloadSupported: Boolean = false,
    val diagnosticsExportSupported: Boolean = false,
)

data class StorageUsage(
    val audioBytes: Long? = null,
    val imageBytes: Long? = null,
    val downloadBytes: Long? = null,
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

data class NetworkStatus(
    val isOnline: Boolean,
    val isMetered: Boolean,
) {
    companion object {
        val Unknown = NetworkStatus(isOnline = true, isMetered = false)
    }
}

data class DiagnosticsReport(
    val generatedAtEpochMs: Long,
    val appVersion: String,
    val buildInfo: String,
    val gitCommitSha: String,
    val platformInfo: String,
    val databaseVersion: Int,
    val sourceCount: Int,
    val trackCount: Long,
    val recentScanSummary: String?,
    val playerStateSummary: String,
    val storageUsage: StorageUsage,
    val recentErrors: List<String>,
)

sealed interface DiagnosticsExportResult {
    data class Success(val path: String) : DiagnosticsExportResult
    data class Failure(val message: String) : DiagnosticsExportResult
}

data class LibraryRebuildState(
    val status: LibraryRebuildStatus = LibraryRebuildStatus.Idle,
    val completedSources: Int = 0,
    val totalSources: Int = 0,
    val failureMessage: String? = null,
)

enum class LibraryRebuildStatus {
    Idle,
    Clearing,
    Scanning,
    Completed,
    Failed,
}

const val AUDIO_CACHE_LIMIT_DISABLED_BYTES = 0L
const val DEFAULT_AUDIO_CACHE_LIMIT_BYTES = 1_073_741_824L
const val MAX_AUDIO_CACHE_LIMIT_BYTES = 10_737_418_240L
const val DEFAULT_IMAGE_CACHE_LIMIT_BYTES = 268_435_456L
const val MAX_IMAGE_CACHE_LIMIT_BYTES = 2_147_483_648L
const val DEFAULT_AUDIO_PRELOAD_BYTES = 4_194_304L
const val MAX_AUDIO_PRELOAD_BYTES = 67_108_864L
const val DEFAULT_MINIMUM_AUDIO_DURATION_MS = 30_000L
const val MAX_MINIMUM_AUDIO_DURATION_MS = 86_400_000L
const val DEFAULT_NETWORK_RETRY_COUNT = 2
const val MAX_NETWORK_RETRY_COUNT = 5
const val DEFAULT_CONNECTION_TIMEOUT_SECONDS = 20
const val MIN_CONNECTION_TIMEOUT_SECONDS = 5
const val MAX_CONNECTION_TIMEOUT_SECONDS = 120
const val MIN_SCANNED_AUDIO_DURATION_MS = DEFAULT_MINIMUM_AUDIO_DURATION_MS

val AUDIO_CACHE_LIMIT_PRESETS_BYTES = listOf(
    AUDIO_CACHE_LIMIT_DISABLED_BYTES,
    536_870_912L,
    DEFAULT_AUDIO_CACHE_LIMIT_BYTES,
    2_147_483_648L,
    4_294_967_296L,
)

val IMAGE_CACHE_LIMIT_PRESETS_BYTES = listOf(
    AUDIO_CACHE_LIMIT_DISABLED_BYTES,
    134_217_728L,
    DEFAULT_IMAGE_CACHE_LIMIT_BYTES,
    536_870_912L,
    1_073_741_824L,
)

val SUPPORTED_AUDIO_EXTENSIONS = listOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav")
val DEFAULT_IGNORED_SOURCE_DIRECTORIES = listOf(".cache", ".trash", "@eaDir", "__MACOSX")

fun normalizeAudioCacheLimitBytes(bytes: Long): Long {
    return bytes.coerceIn(AUDIO_CACHE_LIMIT_DISABLED_BYTES, MAX_AUDIO_CACHE_LIMIT_BYTES)
}

fun normalizeImageCacheLimitBytes(bytes: Long): Long {
    return bytes.coerceIn(AUDIO_CACHE_LIMIT_DISABLED_BYTES, MAX_IMAGE_CACHE_LIMIT_BYTES)
}

fun normalizeNetworkRetryCount(value: Int): Int = value.coerceIn(0, MAX_NETWORK_RETRY_COUNT)

fun normalizeConnectionTimeoutSeconds(value: Int): Int {
    return value.coerceIn(MIN_CONNECTION_TIMEOUT_SECONDS, MAX_CONNECTION_TIMEOUT_SECONDS)
}

fun normalizeAudioPreloadBytes(bytes: Long): Long {
    return bytes.coerceIn(AUDIO_CACHE_LIMIT_DISABLED_BYTES, MAX_AUDIO_PRELOAD_BYTES)
}

fun normalizeMinimumAudioDurationMs(value: Long): Long {
    return value.coerceIn(0L, MAX_MINIMUM_AUDIO_DURATION_MS)
}

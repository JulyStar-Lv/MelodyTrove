package io.github.julystar.musicapp.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppThemeMode {
    System,
    Light,
    Dark,
}

@Serializable
enum class AppLanguageMode {
    System,
    Chinese,
    English,
}

@Serializable
enum class AudioFocusMode {
    Pause,
    Duck,
    Mix,
}

@Serializable
enum class AutoScanMode {
    Off,
    OnStartup,
    Periodic,
}

@Serializable
enum class MissingFilePolicy {
    MarkUnavailable,
    RemoveOnScan,
}

@Serializable
enum class DuplicateTrackPolicy {
    KeepAll,
    SeparateBySource,
}

@Serializable
enum class LyricTextAlignment {
    Left,
    Center,
    Right,
}

@Serializable
enum class LyricSourceMode {
    Auto,
    Embedded,
    External,
}

@Serializable
enum class LyricSourceKind {
    EmbeddedTtml,
    EmbeddedPlain,
    ExternalTtml,
    ExternalPlain,
}

@Serializable
enum class LyricFontChoice {
    System,
    AppSans,
    AppCjk,
    Monospace,
}

@Serializable
data class LyricFontSettings(
    val westernFont: LyricFontChoice = LyricFontChoice.AppSans,
    val cjkFont: LyricFontChoice = LyricFontChoice.AppCjk,
    val weight: Int = DEFAULT_LYRIC_FONT_WEIGHT,
    val applyToLyricsPage: Boolean = true,
    val applyToFloatingLyrics: Boolean = true,
    val applyToShareCard: Boolean = false,
) {
    companion object {
        val Default = LyricFontSettings()
    }
}

@Serializable
data class LyricDisplaySettings(
    val textAlignment: LyricTextAlignment = LyricTextAlignment.Left,
    val primaryFontScalePercent: Int = DEFAULT_LYRIC_FONT_SCALE_PERCENT,
    val primaryFontSizeSp: Int = DEFAULT_LYRIC_PRIMARY_FONT_SIZE_SP,
    val secondaryFontScalePercent: Int = DEFAULT_LYRIC_FONT_SCALE_PERCENT,
    val secondaryFontSizeSp: Int = DEFAULT_LYRIC_SECONDARY_FONT_SIZE_SP,
    val showTranslation: Boolean = true,
    val wordLiftEnabled: Boolean = true,
    val blurEffectEnabled: Boolean = true,
    val perspectiveEffectEnabled: Boolean = false,
    val perspectiveAngleDegrees: Int = DEFAULT_LYRIC_PERSPECTIVE_ANGLE_DEGREES,
    val tapToSeekEnabled: Boolean = true,
    val sourceMode: LyricSourceMode = LyricSourceMode.Auto,
    val sourcePriority: List<LyricSourceKind> = DEFAULT_LYRIC_SOURCE_PRIORITY,
    val ignoreHeaderTags: Boolean = true,
    val lineBlacklist: List<String> = emptyList(),
    val font: LyricFontSettings = LyricFontSettings.Default,
) {
    companion object {
        val Default = LyricDisplaySettings()
    }
}

@Serializable
enum class StartupPlaybackMode {
    Off,
    ResumeLastQueue,
    ShuffleLibrary,
}

@Serializable
enum class PreviousButtonBehavior {
    PreviousTrack,
    RestartCurrentTrack,
}

@Serializable
enum class PlayNextMode {
    FirstRequestedFirst,
    LastRequestedFirst,
}

@Serializable
enum class ShuffleStrategy {
    QueueOrder,
    TrueRandom,
}

@Serializable
enum class ReplayGainMode {
    Off,
    Track,
    Album,
    Auto,
}

@Serializable
data class PlaybackAdvancedSettings(
    val crossfadeDurationMs: Int = 0,
    val replayGainMode: ReplayGainMode = ReplayGainMode.Off,
    val replayGainPreampTenthsDb: Int = 0,
    val resumePlaybackPosition: Boolean = true,
    val startupPlaybackMode: StartupPlaybackMode = StartupPlaybackMode.Off,
    val previousButtonBehavior: PreviousButtonBehavior = PreviousButtonBehavior.PreviousTrack,
    val playNextMode: PlayNextMode = PlayNextMode.FirstRequestedFirst,
    val shuffleStrategy: ShuffleStrategy = ShuffleStrategy.QueueOrder,
) {
    companion object {
        val Default = PlaybackAdvancedSettings()
    }
}

@Serializable
data class PlayerInteractionSettings(
    val openPlayerOnPlay: Boolean = false,
    val coverSwipeEnabled: Boolean = true,
    val tapProgressToSeekEnabled: Boolean = true,
    val showTotalDuration: Boolean = false,
    val showSongAnnotation: Boolean = true,
    val desktopShortcutsEnabled: Boolean = true,
    val metadataEditor: MetadataEditorApp = MetadataEditorApp.AskEveryTime,
    val lyricTimingEditor: LyricTimingEditorApp = LyricTimingEditorApp.AskEveryTime,
) {
    companion object {
        val Default = PlayerInteractionSettings()
    }
}

@Serializable
enum class MetadataEditorApp {
    AskEveryTime,
    Lyrico,
    LunaBeat,
    MusicTag,
}

@Serializable
enum class LyricTimingEditorApp {
    AskEveryTime,
    LunaBeat,
}

@Serializable
data class MetadataParsingSettings(
    val artistSeparators: String = DEFAULT_ARTIST_SEPARATORS,
    val artistProtectedNames: String = "",
    val genreSeparators: String = DEFAULT_GENRE_SEPARATORS,
    val genreProtectedNames: String = "",
    val ignoreTagCase: Boolean = false,
) {
    companion object {
        val Default = MetadataParsingSettings()
    }
}

@Serializable
enum class ReverbPreset {
    None,
    SmallRoom,
    MediumRoom,
    LargeRoom,
    Hall,
    Plate,
}

@Serializable
data class AudioEffectSettings(
    val enabled: Boolean = false,
    val eqBandGainsDb: List<Int> = DEFAULT_EQ_BAND_GAINS_DB,
    val eqQHundredths: Int = DEFAULT_EQ_Q_HUNDREDTHS,
    val bassDb: Int = 0,
    val trebleDb: Int = 0,
    val compressorEnabled: Boolean = false,
    val compressorThresholdDb: Int = DEFAULT_COMPRESSOR_THRESHOLD_DB,
    val compressorRatio: Int = DEFAULT_COMPRESSOR_RATIO,
    val compressorMakeupDb: Int = 0,
    val stereoWidthPercent: Int = DEFAULT_STEREO_WIDTH_PERCENT,
    val reverbPreset: ReverbPreset = ReverbPreset.None,
) {
    companion object {
        val Default = AudioEffectSettings()
    }
}

@Serializable
enum class SecondaryLyricContent {
    Off,
    Translation,
    Pronunciation,
}

@Serializable
data class LyricOutputSettings(
    val floatingLyricsEnabled: Boolean = false,
    val notificationLyricsEnabled: Boolean = false,
    val bluetoothLyricsEnabled: Boolean = false,
    val lyriconEnabled: Boolean = false,
    val superLyricEnabled: Boolean = false,
    val lyricGetterEnabled: Boolean = false,
    val flymeStatusLyricsEnabled: Boolean = false,
    val colorOsLockScreenLyricsEnabled: Boolean = false,
    val secondaryContent: SecondaryLyricContent = SecondaryLyricContent.Translation,
) {
    companion object {
        val Default = LyricOutputSettings()
    }
}

@Serializable
enum class BackupSchedule {
    Off,
    Daily,
    Weekly,
}

@Serializable
data class SettingsBackupSelection(
    val appearance: Boolean = true,
    val playback: Boolean = true,
    val lyrics: Boolean = true,
    val libraryAndMetadata: Boolean = true,
    val networkAndCache: Boolean = true,
) {
    companion object {
        val All = SettingsBackupSelection()
    }
}

@Serializable
data class SettingsBackupSettings(
    val selection: SettingsBackupSelection = SettingsBackupSelection.All,
    val schedule: BackupSchedule = BackupSchedule.Off,
    val webDavAccountId: Long? = null,
    val remoteDirectory: String = "/MelodyTrove/Backups",
) {
    companion object {
        val Default = SettingsBackupSettings()
    }
}

@Serializable
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
    val lyrics: LyricDisplaySettings = LyricDisplaySettings.Default,
    val playbackAdvanced: PlaybackAdvancedSettings = PlaybackAdvancedSettings.Default,
    val playerInteraction: PlayerInteractionSettings = PlayerInteractionSettings.Default,
    val metadataParsing: MetadataParsingSettings = MetadataParsingSettings.Default,
    val audioEffects: AudioEffectSettings = AudioEffectSettings.Default,
    val lyricOutput: LyricOutputSettings = LyricOutputSettings.Default,
    val backup: SettingsBackupSettings = SettingsBackupSettings.Default,
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
    val crossfadeSupported: Boolean = false,
    val replayGainSupported: Boolean = false,
    val audioEffectsSupported: Boolean = false,
    val lyricFontSelectionSupported: Boolean = true,
    val networkStatusSupported: Boolean = false,
    val audioPreloadSupported: Boolean = false,
    val diagnosticsExportSupported: Boolean = false,
    val diagnosticsCenterSupported: Boolean = false,
    val safeModeSupported: Boolean = false,
    val platformExitInfoSupported: Boolean = false,
    val historicalAnrTraceSupported: Boolean = false,
    val incidentRecoverySupported: Boolean = false,
    val fileShareSupported: Boolean = false,
    val notificationLyricsSupported: Boolean = false,
    val bluetoothLyricsSupported: Boolean = false,
    val lyriconSupported: Boolean = false,
    val superLyricSupported: Boolean = false,
    val lyricGetterSupported: Boolean = false,
    val flymeStatusLyricsSupported: Boolean = false,
    val colorOsLockScreenLyricsSupported: Boolean = false,
    val externalEditorSupported: Boolean = false,
    val desktopShortcutsSupported: Boolean = false,
    val settingsBackupSupported: Boolean = false,
    val scheduledBackupSupported: Boolean = false,
)

sealed interface SettingsBackupResult {
    data class Success(val path: String) : SettingsBackupResult
    data class Failure(val message: String) : SettingsBackupResult
}

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
const val MIN_LYRIC_FONT_SCALE_PERCENT = 75
const val DEFAULT_LYRIC_FONT_SCALE_PERCENT = 100
const val MAX_LYRIC_FONT_SCALE_PERCENT = 175
const val MIN_LYRIC_PRIMARY_FONT_SIZE_SP = 20
const val DEFAULT_LYRIC_PRIMARY_FONT_SIZE_SP = 32
const val MAX_LYRIC_PRIMARY_FONT_SIZE_SP = 54
const val MIN_LYRIC_SECONDARY_FONT_SIZE_SP = 12
const val DEFAULT_LYRIC_SECONDARY_FONT_SIZE_SP = 19
const val MAX_LYRIC_SECONDARY_FONT_SIZE_SP = 30
const val MIN_LYRIC_PERSPECTIVE_ANGLE_DEGREES = 0
const val DEFAULT_LYRIC_PERSPECTIVE_ANGLE_DEGREES = 25
const val MAX_LYRIC_PERSPECTIVE_ANGLE_DEGREES = 45
const val DEFAULT_LYRIC_FONT_WEIGHT = 700
const val MIN_LYRIC_FONT_WEIGHT = 100
const val MAX_LYRIC_FONT_WEIGHT = 900
const val MAX_LYRIC_LINE_BLACKLIST_SIZE = 256
const val MAX_CROSSFADE_DURATION_MS = 30_000
const val MIN_REPLAY_GAIN_PREAMP_TENTHS_DB = -200
const val MAX_REPLAY_GAIN_PREAMP_TENTHS_DB = 200
const val DEFAULT_ARTIST_SEPARATORS = ";,/&、，"
const val DEFAULT_GENRE_SEPARATORS = ";,/、，"
const val EQ_BAND_COUNT = 10
const val MIN_EQ_BAND_GAIN_DB = -12
const val MAX_EQ_BAND_GAIN_DB = 12
const val DEFAULT_EQ_Q_HUNDREDTHS = 100
const val MIN_EQ_Q_HUNDREDTHS = 25
const val MAX_EQ_Q_HUNDREDTHS = 400
const val DEFAULT_COMPRESSOR_THRESHOLD_DB = -18
const val DEFAULT_COMPRESSOR_RATIO = 4
const val DEFAULT_STEREO_WIDTH_PERCENT = 100
const val MIN_SCANNED_AUDIO_DURATION_MS = DEFAULT_MINIMUM_AUDIO_DURATION_MS

val DEFAULT_LYRIC_SOURCE_PRIORITY = LyricSourceKind.entries.toList()
val DEFAULT_EQ_BAND_GAINS_DB = List(EQ_BAND_COUNT) { 0 }

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

fun normalizeLyricFontScalePercent(value: Int): Int {
    return value.coerceIn(MIN_LYRIC_FONT_SCALE_PERCENT, MAX_LYRIC_FONT_SCALE_PERCENT)
}

fun normalizeLyricPrimaryFontSizeSp(value: Int): Int {
    return value.coerceIn(MIN_LYRIC_PRIMARY_FONT_SIZE_SP, MAX_LYRIC_PRIMARY_FONT_SIZE_SP)
}

fun normalizeLyricSecondaryFontSizeSp(value: Int): Int {
    return value.coerceIn(MIN_LYRIC_SECONDARY_FONT_SIZE_SP, MAX_LYRIC_SECONDARY_FONT_SIZE_SP)
}

fun normalizeLyricPerspectiveAngleDegrees(value: Int): Int {
    return value.coerceIn(
        MIN_LYRIC_PERSPECTIVE_ANGLE_DEGREES,
        MAX_LYRIC_PERSPECTIVE_ANGLE_DEGREES,
    )
}

fun normalizeLyricSourcePriority(value: List<LyricSourceKind>): List<LyricSourceKind> {
    val unique = value.distinct()
    return unique + DEFAULT_LYRIC_SOURCE_PRIORITY.filterNot(unique::contains)
}

fun normalizeLyricLineBlacklist(value: List<String>): List<String> {
    return value
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
        .take(MAX_LYRIC_LINE_BLACKLIST_SIZE)
}

fun normalizeLyricFontSettings(value: LyricFontSettings): LyricFontSettings {
    val normalizedWeight = (value.weight.coerceIn(MIN_LYRIC_FONT_WEIGHT, MAX_LYRIC_FONT_WEIGHT) / 100) * 100
    return value.copy(weight = normalizedWeight)
}

fun normalizePlaybackAdvancedSettings(value: PlaybackAdvancedSettings): PlaybackAdvancedSettings {
    return value.copy(
        crossfadeDurationMs = value.crossfadeDurationMs.coerceIn(0, MAX_CROSSFADE_DURATION_MS),
        replayGainPreampTenthsDb = value.replayGainPreampTenthsDb.coerceIn(
            MIN_REPLAY_GAIN_PREAMP_TENTHS_DB,
            MAX_REPLAY_GAIN_PREAMP_TENTHS_DB,
        ),
    )
}

fun normalizeAudioEffectSettings(value: AudioEffectSettings): AudioEffectSettings {
    val gains = value.eqBandGainsDb
        .take(EQ_BAND_COUNT)
        .map { it.coerceIn(MIN_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB) }
        .let { it + List(EQ_BAND_COUNT - it.size) { 0 } }
    return value.copy(
        eqBandGainsDb = gains,
        eqQHundredths = value.eqQHundredths.coerceIn(
            MIN_EQ_Q_HUNDREDTHS,
            MAX_EQ_Q_HUNDREDTHS,
        ),
        bassDb = value.bassDb.coerceIn(MIN_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB),
        trebleDb = value.trebleDb.coerceIn(MIN_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB),
        compressorThresholdDb = value.compressorThresholdDb.coerceIn(-60, 0),
        compressorRatio = value.compressorRatio.coerceIn(1, 20),
        compressorMakeupDb = value.compressorMakeupDb.coerceIn(0, 24),
        stereoWidthPercent = value.stereoWidthPercent.coerceIn(0, 200),
    )
}

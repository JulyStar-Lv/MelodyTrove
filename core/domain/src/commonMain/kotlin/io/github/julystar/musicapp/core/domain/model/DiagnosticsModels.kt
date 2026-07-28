package io.github.julystar.musicapp.core.domain.model

enum class DiagnosticLogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
    Fatal,
}

enum class DiagnosticLogCategory {
    App,
    Startup,
    Crash,
    Anr,
    Playback,
    Dsp,
    LibraryScan,
    Metadata,
    Lyrics,
    LocalSource,
    WebDav,
    Smb,
    Plugin,
    Database,
    Network,
    Cache,
    Backup,
    Security,
    Unknown,
}

data class DiagnosticLogEntry(
    val schemaVersion: Int,
    val timestampEpochMs: Long,
    val level: DiagnosticLogLevel,
    val category: DiagnosticLogCategory,
    val target: String,
    val message: String,
    val detail: String?,
    val sessionId: String,
    val correlationId: String?,
    val startupAttemptId: String?,
    val thread: String?,
    val platform: String,
    val fields: Map<String, String>,
)

data class DiagnosticLogSession(
    val sessionId: String,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val platform: String,
    val appVersion: String,
    val buildInfo: String,
    val gitCommitSha: String,
    val processName: String,
    val startupAttemptId: String,
    val gracefulShutdown: Boolean,
    val current: Boolean,
    val logPaths: List<String>,
    val logBytes: Long,
)

data class DiagnosticLogFilter(
    val sessionIds: Set<String> = emptySet(),
    val levels: Set<DiagnosticLogLevel> = emptySet(),
    val categories: Set<DiagnosticLogCategory> = emptySet(),
    val keyword: String? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val correlationId: String? = null,
    val offset: Long = 0,
    val limit: Long = DEFAULT_DIAGNOSTIC_PAGE_SIZE,
)

data class DiagnosticLogPage(
    val entries: List<DiagnosticLogEntry>,
    val offset: Long,
    val limit: Long,
    val totalMatched: Long,
    val hasMore: Boolean,
    val warnings: List<String>,
)

data class DiagnosticLogSessionPage(
    val sessions: List<DiagnosticLogSession>,
    val offset: Long,
    val limit: Long,
    val total: Long,
    val hasMore: Boolean,
)

data class DiagnosticLogRetentionPolicy(
    val retentionDays: Long = 7,
    val maxSessions: Long = 30,
    val maxTotalBytes: Long = 50L * 1024 * 1024,
    val maxSessionBytes: Long = 10L * 1024 * 1024,
)

enum class DiagnosticIncidentType {
    KotlinUncaught,
    RustPanic,
    NativeCrash,
    AndroidAnr,
    OutOfMemory,
    StartupFailure,
    DatabaseOpenFailure,
    DatabaseMigrationFailure,
    PluginBootFailure,
    PlaybackBackendFailure,
    UiEventLoopStall,
    UnknownAbnormalExit,
}

enum class DiagnosticIncidentSeverity {
    Info,
    Warning,
    Error,
    Fatal,
}

enum class DiagnosticIncidentState {
    Detected,
    PendingReview,
    Acknowledged,
    Exported,
    RecoveryAttempted,
    Resolved,
    Ignored,
}

enum class DiagnosticStartupStage {
    ProcessStarted,
    PathsReady,
    DiagnosticsReady,
    PlatformExitsCollected,
    SettingsLoading,
    SettingsReady,
    DatabaseOpening,
    DatabaseReady,
    BackendCreating,
    BackendReady,
    PluginsLoading,
    PluginsReady,
    PlaybackRestoring,
    PlaybackReady,
    SourceTasksScheduling,
    UiCompositionStarted,
    FirstFrameRendered,
    StartupStable,
    ShutdownStarted,
    ShutdownComplete,
    ;

    val isBeforeStable: Boolean
        get() = ordinal < StartupStable.ordinal
}

enum class DiagnosticFaultInjection {
    KotlinUncaught,
    RustPanic,
    AndroidAnr,
    DatabaseOpenFailure,
    DatabaseMigrationFailure,
    PluginBootFailure,
    PlaybackBackendFailure,
    IncompleteStartupAttempt,
    RepeatedFatalIncident,
}

data class DiagnosticIncident(
    val id: String,
    val type: DiagnosticIncidentType,
    val severity: DiagnosticIncidentSeverity,
    val state: DiagnosticIncidentState,
    val detectedAtEpochMs: Long,
    val lastSeenAtEpochMs: Long,
    val processName: String?,
    val sessionId: String?,
    val startupAttemptId: String?,
    val startupStage: DiagnosticStartupStage?,
    val fingerprint: String?,
    val summary: String,
    val detail: String?,
    val artifactPaths: List<String>,
    val relatedLogSessionIds: List<String>,
    val occurrenceCount: Long,
    val occurrenceTimestampsEpochMs: List<Long>,
    val requiresRecovery: Boolean,
)

data class DiagnosticIncidentFilter(
    val types: Set<DiagnosticIncidentType> = emptySet(),
    val severities: Set<DiagnosticIncidentSeverity> = emptySet(),
    val states: Set<DiagnosticIncidentState> = emptySet(),
    val requiresRecovery: Boolean? = null,
    val offset: Long = 0,
    val limit: Long = DEFAULT_DIAGNOSTIC_PAGE_SIZE,
)

data class DiagnosticIncidentPage(
    val incidents: List<DiagnosticIncident>,
    val offset: Long,
    val limit: Long,
    val totalMatched: Long,
    val hasMore: Boolean,
)

data class DiagnosticIncidentDraft(
    val type: DiagnosticIncidentType,
    val severity: DiagnosticIncidentSeverity,
    val summary: String,
    val detail: String?,
    val fingerprintMaterial: String?,
    val requiresRecovery: Boolean,
)

data class DiagnosticPlatformExit(
    val exitKey: String,
    val type: DiagnosticIncidentType,
    val severity: DiagnosticIncidentSeverity,
    val timestampEpochMs: Long,
    val processName: String,
    val pid: Long,
    val reason: Long,
    val status: Long,
    val importance: Long,
    val pssKb: Long,
    val rssKb: Long,
    val description: String?,
    val trace: ByteArray?,
    val traceTruncated: Boolean,
    val requiresRecovery: Boolean,
    val environmentSummary: String,
    val startupAttemptId: String?,
    val startupStage: DiagnosticStartupStage?,
)

data class DiagnosticStartupAttempt(
    val attemptId: String,
    val startedAtEpochMs: Long,
    val lastUpdatedAtEpochMs: Long,
    val lastStage: DiagnosticStartupStage,
    val stable: Boolean,
    val gracefulShutdown: Boolean,
    val safeMode: Boolean,
    val safeModeReason: String?,
    val recoveryAttempted: Boolean,
    val disabledComponents: List<String>,
)

data class DiagnosticPendingRecovery(
    val incidentId: String,
    val fingerprint: String?,
    val incidentType: DiagnosticIncidentType,
    val startupAttemptId: String?,
    val startupStage: DiagnosticStartupStage?,
    val occurrenceCount: Long,
    val failedRecoveryAttempts: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class DiagnosticRuntimeSnapshot(
    val diagnosticsRoot: String,
    val sessionId: String,
    val startupAttempt: DiagnosticStartupAttempt,
    val previousStartupAttempt: DiagnosticStartupAttempt?,
    val pendingRecovery: DiagnosticPendingRecovery?,
    val safeModeSuggested: Boolean,
    val safeModeReason: String?,
)

data class DiagnosticStorageBreakdown(
    val logBytes: Long,
    val incidentBytes: Long,
    val startupBytes: Long,
    val exportBytes: Long,
    val totalBytes: Long,
)

data class DiagnosticExportBundleRequest(
    val summary: String,
    val environmentJson: String,
    val playbackSummaryJson: String,
    val scanSummaryJson: String,
    val pluginSummaryJson: String,
    val sourceSummaryJson: String,
    val storageSummaryJson: String,
    val includeResolvedIncidents: Boolean = false,
    val incidentIds: Set<String> = emptySet(),
)

data class DiagnosticExportBundle(
    val path: String,
    val fileName: String,
    val bytes: Long,
    val includedLogSessions: Long,
    val includedIncidents: Long,
)

const val DEFAULT_DIAGNOSTIC_PAGE_SIZE = 100L
const val MAX_DIAGNOSTIC_PAGE_SIZE = 500L

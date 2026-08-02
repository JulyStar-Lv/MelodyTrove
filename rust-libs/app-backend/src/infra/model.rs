use std::collections::HashMap;

use serde::{Deserialize, Serialize};

pub const DIAGNOSTICS_SCHEMA_VERSION: i32 = 1;
pub const DEFAULT_LOG_PAGE_SIZE: i64 = 100;
pub const MAX_LOG_PAGE_SIZE: i64 = 500;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum DiagnosticLogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
    Fatal,
}

impl DiagnosticLogLevel {
    pub(crate) fn as_str(self) -> &'static str {
        match self {
            Self::Trace => "TRACE",
            Self::Debug => "DEBUG",
            Self::Info => "INFO",
            Self::Warn => "WARN",
            Self::Error => "ERROR",
            Self::Fatal => "FATAL",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum DiagnosticLogCategory {
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
    Webdav,
    Smb,
    Plugin,
    Database,
    Network,
    Cache,
    Backup,
    Security,
    Unknown,
}

impl DiagnosticLogCategory {
    pub(crate) fn as_str(self) -> &'static str {
        match self {
            Self::App => "APP",
            Self::Startup => "STARTUP",
            Self::Crash => "CRASH",
            Self::Anr => "ANR",
            Self::Playback => "PLAYBACK",
            Self::Dsp => "DSP",
            Self::LibraryScan => "LIBRARY_SCAN",
            Self::Metadata => "METADATA",
            Self::Lyrics => "LYRICS",
            Self::LocalSource => "LOCAL_SOURCE",
            Self::Webdav => "WEBDAV",
            Self::Smb => "SMB",
            Self::Plugin => "PLUGIN",
            Self::Database => "DATABASE",
            Self::Network => "NETWORK",
            Self::Cache => "CACHE",
            Self::Backup => "BACKUP",
            Self::Security => "SECURITY",
            Self::Unknown => "UNKNOWN",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogEvent {
    pub level: DiagnosticLogLevel,
    pub category: DiagnosticLogCategory,
    pub target: String,
    pub message: String,
    pub detail: Option<String>,
    pub correlation_id: Option<String>,
    pub fields: HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogEntry {
    pub schema_version: i32,
    pub timestamp_epoch_ms: i64,
    pub level: DiagnosticLogLevel,
    pub category: DiagnosticLogCategory,
    pub target: String,
    pub message: String,
    pub detail: Option<String>,
    pub session_id: String,
    pub correlation_id: Option<String>,
    pub startup_attempt_id: Option<String>,
    pub thread: Option<String>,
    pub platform: String,
    pub fields: HashMap<String, String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogSession {
    pub session_id: String,
    pub started_at_epoch_ms: i64,
    pub ended_at_epoch_ms: Option<i64>,
    pub platform: String,
    pub app_version: String,
    pub build_info: String,
    pub git_commit_sha: String,
    pub process_name: String,
    pub startup_attempt_id: String,
    pub graceful_shutdown: bool,
    pub current: bool,
    pub log_paths: Vec<String>,
    pub log_bytes: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogFilter {
    pub session_ids: Vec<String>,
    pub levels: Vec<DiagnosticLogLevel>,
    pub categories: Vec<DiagnosticLogCategory>,
    pub keyword: Option<String>,
    pub start_epoch_ms: Option<i64>,
    pub end_epoch_ms: Option<i64>,
    pub correlation_id: Option<String>,
    pub offset: i64,
    pub limit: i64,
}

impl Default for DiagnosticLogFilter {
    fn default() -> Self {
        Self {
            session_ids: Vec::new(),
            levels: Vec::new(),
            categories: Vec::new(),
            keyword: None,
            start_epoch_ms: None,
            end_epoch_ms: None,
            correlation_id: None,
            offset: 0,
            limit: DEFAULT_LOG_PAGE_SIZE,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogPage {
    pub entries: Vec<DiagnosticLogEntry>,
    pub offset: i64,
    pub limit: i64,
    pub total_matched: i64,
    pub has_more: bool,
    pub warnings: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogSessionPage {
    pub sessions: Vec<DiagnosticLogSession>,
    pub offset: i64,
    pub limit: i64,
    pub total: i64,
    pub has_more: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticLogRetentionPolicy {
    pub retention_days: i64,
    pub max_sessions: i64,
    pub max_total_bytes: i64,
    pub max_session_bytes: i64,
}

impl Default for DiagnosticLogRetentionPolicy {
    fn default() -> Self {
        Self {
            retention_days: 7,
            max_sessions: 30,
            max_total_bytes: 50 * 1024 * 1024,
            max_session_bytes: 10 * 1024 * 1024,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticStorageUsage {
    pub log_bytes: i64,
    pub incident_bytes: i64,
    pub startup_bytes: i64,
    pub export_bytes: i64,
    pub total_bytes: i64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum IncidentType {
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

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum IncidentSeverity {
    Info,
    Warning,
    Error,
    Fatal,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum IncidentState {
    Detected,
    PendingReview,
    Acknowledged,
    Exported,
    RecoveryAttempted,
    Resolved,
    Ignored,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum StartupStage {
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
}

impl StartupStage {
    pub(crate) fn rank(self) -> i32 {
        match self {
            Self::ProcessStarted => 0,
            Self::PathsReady => 1,
            Self::DiagnosticsReady => 2,
            Self::PlatformExitsCollected => 3,
            Self::SettingsLoading => 4,
            Self::SettingsReady => 5,
            Self::DatabaseOpening => 6,
            Self::DatabaseReady => 7,
            Self::BackendCreating => 8,
            Self::BackendReady => 9,
            Self::PluginsLoading => 10,
            Self::PluginsReady => 11,
            Self::PlaybackRestoring => 12,
            Self::PlaybackReady => 13,
            Self::SourceTasksScheduling => 14,
            Self::UiCompositionStarted => 15,
            Self::FirstFrameRendered => 16,
            Self::StartupStable => 17,
            Self::ShutdownStarted => 18,
            Self::ShutdownComplete => 19,
        }
    }

    pub(crate) fn is_before_stable(self) -> bool {
        self.rank() < Self::StartupStable.rank()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticIncident {
    pub schema_version: i32,
    pub id: String,
    pub incident_type: IncidentType,
    pub severity: IncidentSeverity,
    pub state: IncidentState,
    pub detected_at_epoch_ms: i64,
    pub last_seen_at_epoch_ms: i64,
    pub process_name: Option<String>,
    pub session_id: Option<String>,
    pub startup_attempt_id: Option<String>,
    pub startup_stage: Option<StartupStage>,
    pub fingerprint: Option<String>,
    pub summary: String,
    pub detail: Option<String>,
    pub artifact_paths: Vec<String>,
    pub related_log_session_ids: Vec<String>,
    pub occurrence_count: i64,
    #[serde(default)]
    pub occurrence_timestamps_epoch_ms: Vec<i64>,
    pub requires_recovery: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct IncidentFilter {
    pub types: Vec<IncidentType>,
    pub severities: Vec<IncidentSeverity>,
    pub states: Vec<IncidentState>,
    pub requires_recovery: Option<bool>,
    pub offset: i64,
    pub limit: i64,
}

impl Default for IncidentFilter {
    fn default() -> Self {
        Self {
            types: Vec::new(),
            severities: Vec::new(),
            states: Vec::new(),
            requires_recovery: None,
            offset: 0,
            limit: DEFAULT_LOG_PAGE_SIZE,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct IncidentPage {
    pub incidents: Vec<DiagnosticIncident>,
    pub offset: i64,
    pub limit: i64,
    pub total_matched: i64,
    pub has_more: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct IncidentDraft {
    pub incident_type: IncidentType,
    pub severity: IncidentSeverity,
    pub summary: String,
    pub detail: Option<String>,
    pub fingerprint_material: Option<String>,
    pub requires_recovery: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct StartupAttempt {
    pub schema_version: i32,
    pub attempt_id: String,
    pub started_at_epoch_ms: i64,
    pub last_updated_at_epoch_ms: i64,
    pub last_stage: StartupStage,
    pub stable: bool,
    pub graceful_shutdown: bool,
    pub safe_mode: bool,
    pub safe_mode_reason: Option<String>,
    pub recovery_attempted: bool,
    pub disabled_components: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct PendingRecovery {
    pub schema_version: i32,
    pub incident_id: String,
    pub fingerprint: Option<String>,
    pub incident_type: IncidentType,
    pub startup_attempt_id: Option<String>,
    pub startup_stage: Option<StartupStage>,
    pub occurrence_count: i64,
    pub failed_recovery_attempts: i64,
    pub created_at_epoch_ms: i64,
    pub updated_at_epoch_ms: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsRuntimeInit {
    pub app_document_dir: String,
    pub app_cache_dir: String,
    pub platform: String,
    pub app_version: String,
    pub build_info: String,
    pub git_commit_sha: String,
    pub process_name: String,
    pub user_forced_safe_mode: bool,
    pub last_user_requested_exit_at_epoch_ms: Option<i64>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticsRuntimeState {
    pub initialized: bool,
    pub diagnostics_root: String,
    pub session_id: String,
    pub startup_attempt: StartupAttempt,
    pub previous_startup_attempt: Option<StartupAttempt>,
    pub pending_recovery: Option<PendingRecovery>,
    pub safe_mode_suggested: bool,
    pub safe_mode_reason: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct PlatformExitRecord {
    pub exit_key: String,
    pub incident_type: IncidentType,
    pub severity: IncidentSeverity,
    pub timestamp_epoch_ms: i64,
    pub process_name: String,
    pub pid: i64,
    pub reason: i64,
    pub status: i64,
    pub importance: i64,
    pub pss_kb: i64,
    pub rss_kb: i64,
    pub description: Option<String>,
    pub trace: Option<Vec<u8>>,
    pub trace_truncated: bool,
    pub requires_recovery: bool,
    pub environment_summary: String,
    pub startup_attempt_id: Option<String>,
    pub startup_stage: Option<StartupStage>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticExportRequest {
    pub summary: String,
    pub environment_json: String,
    pub playback_summary_json: String,
    pub scan_summary_json: String,
    pub plugin_summary_json: String,
    pub source_summary_json: String,
    pub storage_summary_json: String,
    pub include_resolved_incidents: bool,
    pub incident_ids: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
#[serde(rename_all = "camelCase")]
pub struct DiagnosticExportResult {
    pub path: String,
    pub file_name: String,
    pub bytes: i64,
    pub included_log_sessions: i64,
    pub included_incidents: i64,
}

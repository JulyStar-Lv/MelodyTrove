use std::{
    collections::HashMap,
    fs::{self, File, OpenOptions},
    io::{self, BufRead, BufReader, Write},
    path::{Path, PathBuf},
    sync::{Arc, Mutex, MutexGuard},
};

use serde::{Deserialize, Serialize};
use tracing::{field::Visit, Event, Level, Subscriber};
use tracing_subscriber::{layer::Context, Layer};

use super::{
    file_ops::{atomic_write_json, new_id, now_epoch_ms, quarantine_corrupt_file, read_json},
    model::{
        DiagnosticLogCategory, DiagnosticLogEntry, DiagnosticLogLevel,
        DiagnosticLogRetentionPolicy, DiagnosticLogSession, DiagnosticsRuntimeInit,
        DIAGNOSTICS_SCHEMA_VERSION,
    },
    redaction::{redact_text, sanitize_fields},
};

const SESSIONS_MANIFEST_FILE: &str = "sessions.json";
const RETENTION_POLICY_FILE: &str = "retention-policy.json";

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionsManifest {
    pub schema_version: i32,
    pub sessions: Vec<DiagnosticLogSession>,
}

pub(crate) struct LogStore {
    diagnostics_root: PathBuf,
    sessions_dir: PathBuf,
    manifest_path: PathBuf,
    retention_path: PathBuf,
    corrupt_dir: PathBuf,
    app_document_dir: PathBuf,
    app_cache_dir: PathBuf,
    platform: String,
    startup_attempt_id: String,
    current: Mutex<SessionWriter>,
    manifest_lock: Mutex<()>,
    retention: Mutex<DiagnosticLogRetentionPolicy>,
}

struct SessionWriter {
    metadata: DiagnosticLogSession,
    file: File,
    current_part: i64,
    current_bytes: i64,
}

impl LogStore {
    pub(crate) fn start(
        diagnostics_root: &Path,
        init: &DiagnosticsRuntimeInit,
        startup_attempt_id: &str,
    ) -> io::Result<Arc<Self>> {
        let sessions_dir = diagnostics_root.join("logs/sessions");
        fs::create_dir_all(&sessions_dir)?;
        let manifest_path = diagnostics_root.join("logs").join(SESSIONS_MANIFEST_FILE);
        let retention_path = diagnostics_root.join("logs").join(RETENTION_POLICY_FILE);
        let corrupt_dir = diagnostics_root.join("state/corrupt");
        let retention = match read_json(&retention_path) {
            Ok(policy) => policy,
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                DiagnosticLogRetentionPolicy::default()
            }
            Err(_) => {
                let _ = quarantine_corrupt_file(&retention_path, &corrupt_dir);
                DiagnosticLogRetentionPolicy::default()
            }
        };
        let session_id = new_id("session");
        let log_path = sessions_dir.join(format!("{session_id}.jsonl"));
        let file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&log_path)?;
        let metadata = DiagnosticLogSession {
            session_id,
            started_at_epoch_ms: now_epoch_ms(),
            ended_at_epoch_ms: None,
            platform: init.platform.clone(),
            app_version: init.app_version.clone(),
            build_info: init.build_info.clone(),
            git_commit_sha: init.git_commit_sha.clone(),
            process_name: init.process_name.clone(),
            startup_attempt_id: startup_attempt_id.to_string(),
            graceful_shutdown: false,
            current: true,
            log_paths: vec![log_path.to_string_lossy().into_owned()],
            log_bytes: 0,
        };
        let store = Arc::new(Self {
            diagnostics_root: diagnostics_root.to_path_buf(),
            sessions_dir,
            manifest_path,
            retention_path,
            corrupt_dir,
            app_document_dir: PathBuf::from(&init.app_document_dir),
            app_cache_dir: PathBuf::from(&init.app_cache_dir),
            platform: init.platform.clone(),
            startup_attempt_id: startup_attempt_id.to_string(),
            current: Mutex::new(SessionWriter {
                metadata: metadata.clone(),
                file,
                current_part: 0,
                current_bytes: 0,
            }),
            manifest_lock: Mutex::new(()),
            retention: Mutex::new(retention),
        });
        store.upsert_session(metadata)?;
        store.migrate_legacy_logs()?;
        Ok(store)
    }

    pub(crate) fn current_session_id(&self) -> String {
        self.current
            .lock()
            .map(|writer| writer.metadata.session_id.clone())
            .unwrap_or_default()
    }

    pub(crate) fn layer(self: &Arc<Self>) -> StructuredLogLayer {
        StructuredLogLayer {
            store: Arc::clone(self),
        }
    }

    pub(crate) fn write_entry(&self, mut entry: DiagnosticLogEntry) -> io::Result<()> {
        entry.message = self.redact(&entry.message);
        entry.target = self.redact(&entry.target);
        entry.detail = entry.detail.as_deref().map(|value| self.redact(value));
        entry.correlation_id = entry
            .correlation_id
            .as_deref()
            .map(|value| self.redact(value));
        entry.fields = sanitize_fields(
            &entry.fields,
            Some(&self.app_document_dir),
            Some(&self.app_cache_dir),
        );
        let mut line = serde_json::to_vec(&entry).map_err(io::Error::other)?;
        line.push(b'\n');

        let policy = self.retention_policy();
        let mut writer = self.current_guard()?;
        let mut rotated = false;
        if writer.current_bytes > 0
            && writer.current_bytes.saturating_add(line.len() as i64)
                > policy.max_session_bytes.max(1)
        {
            writer.file.flush()?;
            writer.file.sync_all()?;
            writer.current_part += 1;
            let path = self.sessions_dir.join(format!(
                "{}-part-{:03}.jsonl",
                writer.metadata.session_id, writer.current_part
            ));
            writer.file = OpenOptions::new().create(true).append(true).open(&path)?;
            writer.current_bytes = 0;
            writer
                .metadata
                .log_paths
                .push(path.to_string_lossy().into_owned());
            rotated = true;
        }
        writer.file.write_all(&line)?;
        writer.current_bytes = writer.current_bytes.saturating_add(line.len() as i64);
        writer.metadata.log_bytes = writer.metadata.log_bytes.saturating_add(line.len() as i64);
        if matches!(
            entry.level,
            DiagnosticLogLevel::Warn | DiagnosticLogLevel::Error | DiagnosticLogLevel::Fatal
        ) || entry.category == DiagnosticLogCategory::Startup
        {
            writer.file.flush()?;
            writer.file.sync_data()?;
        }
        let metadata = rotated.then(|| writer.metadata.clone());
        drop(writer);
        if let Some(metadata) = metadata {
            self.upsert_session(metadata)?;
        }
        Ok(())
    }

    pub(crate) fn flush(&self) -> io::Result<()> {
        let mut writer = self.current_guard()?;
        writer.file.flush()?;
        writer.file.sync_all()?;
        writer.metadata.log_bytes = writer
            .metadata
            .log_paths
            .iter()
            .map(|path| {
                fs::metadata(path)
                    .map(|metadata| metadata.len().min(i64::MAX as u64) as i64)
                    .unwrap_or(0)
            })
            .sum();
        let metadata = writer.metadata.clone();
        drop(writer);
        self.upsert_session(metadata)
    }

    pub(crate) fn shutdown(&self) -> io::Result<()> {
        let mut writer = self.current_guard()?;
        writer.file.flush()?;
        writer.file.sync_all()?;
        writer.metadata.ended_at_epoch_ms = Some(now_epoch_ms());
        writer.metadata.graceful_shutdown = true;
        writer.metadata.current = false;
        let metadata = writer.metadata.clone();
        drop(writer);
        self.upsert_session(metadata)
    }

    pub(crate) fn retention_policy(&self) -> DiagnosticLogRetentionPolicy {
        self.retention
            .lock()
            .map(|policy| policy.clone())
            .unwrap_or_default()
    }

    pub(crate) fn set_retention_policy(
        &self,
        policy: DiagnosticLogRetentionPolicy,
    ) -> io::Result<DiagnosticLogRetentionPolicy> {
        let normalized = DiagnosticLogRetentionPolicy {
            retention_days: policy.retention_days.clamp(1, 365),
            max_sessions: policy.max_sessions.clamp(2, 500),
            max_total_bytes: policy
                .max_total_bytes
                .clamp(1024 * 1024, 2 * 1024 * 1024 * 1024),
            max_session_bytes: policy
                .max_session_bytes
                .clamp(256 * 1024, 256 * 1024 * 1024),
        };
        atomic_write_json(&self.retention_path, &normalized)?;
        *self
            .retention
            .lock()
            .map_err(|_| io::Error::other("retention policy lock poisoned"))? = normalized.clone();
        Ok(normalized)
    }

    pub(crate) fn read_manifest(&self) -> SessionsManifest {
        let Ok(_guard) = self.manifest_lock.lock() else {
            return empty_manifest();
        };
        self.read_manifest_unlocked()
    }

    pub(crate) fn replace_manifest(&self, manifest: &SessionsManifest) -> io::Result<()> {
        let _guard = self
            .manifest_lock
            .lock()
            .map_err(|_| io::Error::other("sessions manifest lock poisoned"))?;
        atomic_write_json(&self.manifest_path, manifest)
    }

    pub(crate) fn sessions_dir(&self) -> &Path {
        &self.sessions_dir
    }

    pub(crate) fn diagnostics_root(&self) -> &Path {
        &self.diagnostics_root
    }

    pub(crate) fn snapshot_log_file(&self, path: &str) -> io::Result<Vec<u8>> {
        let path = Path::new(path);
        if !path.starts_with(&self.sessions_dir) {
            return Err(io::Error::new(
                io::ErrorKind::PermissionDenied,
                "log path is outside the sessions directory",
            ));
        }
        let mut writer = self.current_guard()?;
        writer.file.flush()?;
        fs::read(path)
    }

    fn upsert_session(&self, session: DiagnosticLogSession) -> io::Result<()> {
        let _guard = self
            .manifest_lock
            .lock()
            .map_err(|_| io::Error::other("sessions manifest lock poisoned"))?;
        let mut manifest = self.read_manifest_unlocked();
        manifest
            .sessions
            .iter_mut()
            .filter(|existing| existing.current && existing.session_id != session.session_id)
            .for_each(|existing| existing.current = false);
        if let Some(existing) = manifest
            .sessions
            .iter_mut()
            .find(|existing| existing.session_id == session.session_id)
        {
            *existing = session;
        } else {
            manifest.sessions.push(session);
        }
        manifest
            .sessions
            .sort_by_key(|session| std::cmp::Reverse(session.started_at_epoch_ms));
        atomic_write_json(&self.manifest_path, &manifest)
    }

    fn read_manifest_unlocked(&self) -> SessionsManifest {
        match read_json(&self.manifest_path) {
            Ok(manifest) => manifest,
            Err(error) if error.kind() == io::ErrorKind::NotFound => empty_manifest(),
            Err(_) => {
                let _ = quarantine_corrupt_file(&self.manifest_path, &self.corrupt_dir);
                self.recover_manifest_from_session_files()
            }
        }
    }

    fn recover_manifest_from_session_files(&self) -> SessionsManifest {
        let Ok(entries) = fs::read_dir(&self.sessions_dir) else {
            return empty_manifest();
        };
        let current_id = self.current_session_id();
        let mut sessions = HashMap::<String, DiagnosticLogSession>::new();
        for entry in entries.flatten() {
            let path = entry.path();
            if path.extension().and_then(|extension| extension.to_str()) != Some("jsonl") {
                continue;
            }
            let Some(stem) = path.file_stem().and_then(|name| name.to_str()) else {
                continue;
            };
            let session_id = stem
                .split_once("-part-")
                .map_or(stem, |(session, _)| session)
                .to_string();
            let metadata = fs::metadata(&path).ok();
            let started_at_epoch_ms = metadata
                .as_ref()
                .and_then(|metadata| metadata.modified().ok())
                .and_then(|time| time.duration_since(std::time::UNIX_EPOCH).ok())
                .map(|duration| duration.as_millis().min(i64::MAX as u128) as i64)
                .unwrap_or_else(now_epoch_ms);
            let bytes = metadata
                .map(|metadata| metadata.len().min(i64::MAX as u64) as i64)
                .unwrap_or(0);
            let session =
                sessions
                    .entry(session_id.clone())
                    .or_insert_with(|| DiagnosticLogSession {
                        session_id: session_id.clone(),
                        started_at_epoch_ms,
                        ended_at_epoch_ms: None,
                        platform: self.platform.clone(),
                        app_version: "unknown".to_string(),
                        build_info: "recovered from damaged session manifest".to_string(),
                        git_commit_sha: String::new(),
                        process_name: String::new(),
                        startup_attempt_id: String::new(),
                        graceful_shutdown: false,
                        current: session_id == current_id,
                        log_paths: Vec::new(),
                        log_bytes: 0,
                    });
            session.started_at_epoch_ms = session.started_at_epoch_ms.min(started_at_epoch_ms);
            session.log_bytes = session.log_bytes.saturating_add(bytes);
            session.log_paths.push(path.to_string_lossy().into_owned());
        }
        let mut recovered = sessions.into_values().collect::<Vec<_>>();
        recovered
            .iter_mut()
            .for_each(|session| session.log_paths.sort());
        recovered.sort_by_key(|session| std::cmp::Reverse(session.started_at_epoch_ms));
        SessionsManifest {
            schema_version: DIAGNOSTICS_SCHEMA_VERSION,
            sessions: recovered,
        }
    }

    fn migrate_legacy_logs(&self) -> io::Result<()> {
        let legacy_dir = self.app_document_dir.join("logs");
        let Ok(entries) = fs::read_dir(&legacy_dir) else {
            return Ok(());
        };
        let known = self
            .read_manifest()
            .sessions
            .into_iter()
            .map(|session| session.session_id)
            .collect::<std::collections::HashSet<_>>();
        for entry in entries.flatten() {
            let source_path = entry.path();
            if !source_path.is_file() {
                continue;
            }
            let Some(stem) = source_path.file_stem().and_then(|name| name.to_str()) else {
                continue;
            };
            let session_id = format!(
                "legacy-{}",
                stem.chars()
                    .filter(|character| character.is_ascii_alphanumeric() || *character == '-')
                    .collect::<String>()
            );
            if known.contains(&session_id) {
                continue;
            }
            let destination = self.sessions_dir.join(format!("{session_id}.jsonl"));
            let mut output = OpenOptions::new()
                .create_new(true)
                .write(true)
                .open(&destination)?;
            let started_at = fs::metadata(&source_path)
                .ok()
                .and_then(|metadata| metadata.modified().ok())
                .and_then(|time| time.duration_since(std::time::UNIX_EPOCH).ok())
                .map(|duration| duration.as_millis().min(i64::MAX as u128) as i64)
                .unwrap_or_else(now_epoch_ms);
            for line in BufReader::new(File::open(&source_path)?)
                .lines()
                .map_while(Result::ok)
            {
                let entry = DiagnosticLogEntry {
                    schema_version: DIAGNOSTICS_SCHEMA_VERSION,
                    timestamp_epoch_ms: started_at,
                    level: infer_legacy_level(&line),
                    category: infer_legacy_category(&line),
                    target: "legacy".to_string(),
                    message: self.redact(&line),
                    detail: None,
                    session_id: session_id.clone(),
                    correlation_id: None,
                    startup_attempt_id: None,
                    thread: None,
                    platform: self.platform.clone(),
                    fields: HashMap::new(),
                };
                serde_json::to_writer(&mut output, &entry).map_err(io::Error::other)?;
                output.write_all(b"\n")?;
            }
            output.flush()?;
            output.sync_all()?;
            let bytes = output.metadata()?.len().min(i64::MAX as u64) as i64;
            self.upsert_session(DiagnosticLogSession {
                session_id,
                started_at_epoch_ms: started_at,
                ended_at_epoch_ms: Some(started_at),
                platform: self.platform.clone(),
                app_version: "legacy".to_string(),
                build_info: "migrated legacy text log".to_string(),
                git_commit_sha: String::new(),
                process_name: String::new(),
                startup_attempt_id: String::new(),
                graceful_shutdown: false,
                current: false,
                log_paths: vec![destination.to_string_lossy().into_owned()],
                log_bytes: bytes,
            })?;
        }
        Ok(())
    }

    fn redact(&self, value: &str) -> String {
        redact_text(
            value,
            Some(&self.app_document_dir),
            Some(&self.app_cache_dir),
        )
    }

    fn current_guard(&self) -> io::Result<MutexGuard<'_, SessionWriter>> {
        self.current
            .lock()
            .map_err(|_| io::Error::other("log writer lock poisoned"))
    }
}

#[derive(Clone)]
pub(crate) struct StructuredLogLayer {
    store: Arc<LogStore>,
}

impl<S> Layer<S> for StructuredLogLayer
where
    S: Subscriber,
{
    fn on_event(&self, event: &Event<'_>, _context: Context<'_, S>) {
        let mut visitor = DiagnosticVisitor::default();
        event.record(&mut visitor);
        let metadata = event.metadata();
        let level = visitor
            .diagnostic_level
            .as_deref()
            .and_then(parse_level)
            .unwrap_or_else(|| tracing_level(metadata.level()));
        let message = visitor
            .message
            .unwrap_or_else(|| metadata.name().to_string());
        let category = visitor
            .category
            .as_deref()
            .and_then(parse_category)
            .unwrap_or_else(|| infer_legacy_category(&message));
        let target = visitor
            .target
            .unwrap_or_else(|| metadata.target().to_string());
        let thread = std::thread::current().name().map(str::to_string);
        let fields = visitor
            .fields_json
            .as_deref()
            .and_then(|json| serde_json::from_str::<HashMap<String, String>>(json).ok())
            .unwrap_or_default();
        let entry = DiagnosticLogEntry {
            schema_version: DIAGNOSTICS_SCHEMA_VERSION,
            timestamp_epoch_ms: now_epoch_ms(),
            level,
            category,
            target,
            message,
            detail: visitor.detail,
            session_id: self.store.current_session_id(),
            correlation_id: visitor.correlation_id,
            startup_attempt_id: Some(self.store.startup_attempt_id.clone()),
            thread,
            platform: self.store.platform.clone(),
            fields,
        };
        let _ = self.store.write_entry(entry);
    }
}

#[derive(Default)]
struct DiagnosticVisitor {
    message: Option<String>,
    diagnostic_level: Option<String>,
    category: Option<String>,
    target: Option<String>,
    detail: Option<String>,
    correlation_id: Option<String>,
    fields_json: Option<String>,
}

impl Visit for DiagnosticVisitor {
    fn record_str(&mut self, field: &tracing::field::Field, value: &str) {
        self.record_value(field.name(), value.to_string());
    }

    fn record_debug(&mut self, field: &tracing::field::Field, value: &dyn std::fmt::Debug) {
        let mut text = format!("{value:?}");
        if text.starts_with('"') && text.ends_with('"') && text.len() >= 2 {
            text = text[1..text.len() - 1].to_string();
        }
        self.record_value(field.name(), text);
    }
}

impl DiagnosticVisitor {
    fn record_value(&mut self, name: &str, value: String) {
        match name {
            "message" => self.message = Some(value),
            "diagnostic_level" => self.diagnostic_level = Some(value),
            "diagnostic_category" => self.category = Some(value),
            "diagnostic_target" => self.target = Some(value),
            "diagnostic_detail" => self.detail = Some(value),
            "diagnostic_correlation_id" => self.correlation_id = Some(value),
            "diagnostic_fields_json" => self.fields_json = Some(value),
            _ => {}
        }
    }
}

fn empty_manifest() -> SessionsManifest {
    SessionsManifest {
        schema_version: DIAGNOSTICS_SCHEMA_VERSION,
        sessions: Vec::new(),
    }
}

fn tracing_level(level: &Level) -> DiagnosticLogLevel {
    match *level {
        Level::TRACE => DiagnosticLogLevel::Trace,
        Level::DEBUG => DiagnosticLogLevel::Debug,
        Level::INFO => DiagnosticLogLevel::Info,
        Level::WARN => DiagnosticLogLevel::Warn,
        Level::ERROR => DiagnosticLogLevel::Error,
    }
}

fn parse_level(value: &str) -> Option<DiagnosticLogLevel> {
    match value.to_ascii_uppercase().as_str() {
        "TRACE" => Some(DiagnosticLogLevel::Trace),
        "DEBUG" => Some(DiagnosticLogLevel::Debug),
        "INFO" => Some(DiagnosticLogLevel::Info),
        "WARN" | "WARNING" => Some(DiagnosticLogLevel::Warn),
        "ERROR" => Some(DiagnosticLogLevel::Error),
        "FATAL" => Some(DiagnosticLogLevel::Fatal),
        _ => None,
    }
}

fn parse_category(value: &str) -> Option<DiagnosticLogCategory> {
    match value.to_ascii_uppercase().as_str() {
        "APP" => Some(DiagnosticLogCategory::App),
        "STARTUP" => Some(DiagnosticLogCategory::Startup),
        "CRASH" => Some(DiagnosticLogCategory::Crash),
        "ANR" => Some(DiagnosticLogCategory::Anr),
        "PLAYBACK" => Some(DiagnosticLogCategory::Playback),
        "DSP" => Some(DiagnosticLogCategory::Dsp),
        "LIBRARY_SCAN" => Some(DiagnosticLogCategory::LibraryScan),
        "METADATA" => Some(DiagnosticLogCategory::Metadata),
        "LYRICS" => Some(DiagnosticLogCategory::Lyrics),
        "LOCAL_SOURCE" => Some(DiagnosticLogCategory::LocalSource),
        "WEBDAV" => Some(DiagnosticLogCategory::Webdav),
        "SMB" => Some(DiagnosticLogCategory::Smb),
        "PLUGIN" => Some(DiagnosticLogCategory::Plugin),
        "DATABASE" => Some(DiagnosticLogCategory::Database),
        "NETWORK" => Some(DiagnosticLogCategory::Network),
        "CACHE" => Some(DiagnosticLogCategory::Cache),
        "BACKUP" => Some(DiagnosticLogCategory::Backup),
        "SECURITY" => Some(DiagnosticLogCategory::Security),
        "UNKNOWN" => Some(DiagnosticLogCategory::Unknown),
        _ => None,
    }
}

fn infer_legacy_level(message: &str) -> DiagnosticLogLevel {
    let lower = message.to_ascii_lowercase();
    if lower.contains("fatal") || lower.contains("panic") {
        DiagnosticLogLevel::Fatal
    } else if lower.contains("error") || lower.contains("exception") {
        DiagnosticLogLevel::Error
    } else if lower.contains("warn") {
        DiagnosticLogLevel::Warn
    } else if lower.contains("debug") {
        DiagnosticLogLevel::Debug
    } else {
        DiagnosticLogLevel::Info
    }
}

fn infer_legacy_category(message: &str) -> DiagnosticLogCategory {
    let lower = message.to_ascii_lowercase();
    if lower.contains("panic") || lower.contains("crash") || lower.contains("exception") {
        DiagnosticLogCategory::Crash
    } else if lower.contains("startup") || lower.contains("initialize") {
        DiagnosticLogCategory::Startup
    } else if lower.contains("play") || lower.contains("audio") || lower.contains("queue") {
        DiagnosticLogCategory::Playback
    } else if lower.contains("database") || lower.contains("room") || lower.contains("sqlite") {
        DiagnosticLogCategory::Database
    } else if lower.contains("scan") || lower.contains("library") {
        DiagnosticLogCategory::LibraryScan
    } else if lower.contains("webdav") {
        DiagnosticLogCategory::Webdav
    } else if lower.contains("smb") {
        DiagnosticLogCategory::Smb
    } else if lower.contains("network") || lower.contains("http") {
        DiagnosticLogCategory::Network
    } else {
        DiagnosticLogCategory::Unknown
    }
}

#[cfg(test)]
mod tests {
    use std::fs;

    use super::*;
    use crate::infra::{
        file_ops::{create_diagnostics_directories, temporary_test_directory},
        model::DiagnosticLogEvent,
    };

    fn init(root: &Path) -> DiagnosticsRuntimeInit {
        DiagnosticsRuntimeInit {
            app_document_dir: root.to_string_lossy().into_owned(),
            app_cache_dir: root.join("cache").to_string_lossy().into_owned(),
            platform: "test".into(),
            app_version: "1".into(),
            build_info: "debug".into(),
            git_commit_sha: "abc".into(),
            process_name: "test".into(),
            user_forced_safe_mode: false,
            last_user_requested_exit_at_epoch_ms: None,
        }
    }

    #[test]
    fn serializes_one_complete_json_object_per_line_and_rotates() {
        let root = temporary_test_directory("logging");
        let diagnostics = root.join("diagnostics");
        create_diagnostics_directories(&diagnostics).unwrap();
        let store = LogStore::start(&diagnostics, &init(&root), "attempt").unwrap();
        store
            .set_retention_policy(DiagnosticLogRetentionPolicy {
                max_session_bytes: 256 * 1024,
                ..DiagnosticLogRetentionPolicy::default()
            })
            .unwrap();
        for index in 0..3 {
            store
                .write_entry(DiagnosticLogEntry {
                    schema_version: 1,
                    timestamp_epoch_ms: index,
                    level: DiagnosticLogLevel::Info,
                    category: DiagnosticLogCategory::App,
                    target: "test".into(),
                    message: "x".repeat(120_000),
                    detail: None,
                    session_id: store.current_session_id(),
                    correlation_id: None,
                    startup_attempt_id: Some("attempt".into()),
                    thread: None,
                    platform: "test".into(),
                    fields: HashMap::new(),
                })
                .unwrap();
        }
        store.flush().unwrap();
        let session = store.read_manifest().sessions[0].clone();
        assert!(session.log_paths.len() >= 2);
        for path in session.log_paths {
            for line in BufReader::new(File::open(path).unwrap()).lines() {
                serde_json::from_str::<DiagnosticLogEntry>(&line.unwrap()).unwrap();
            }
        }
        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn structured_event_model_keeps_explicit_category() {
        let event = DiagnosticLogEvent {
            level: DiagnosticLogLevel::Info,
            category: DiagnosticLogCategory::Playback,
            target: "Player".into(),
            message: "started".into(),
            detail: None,
            correlation_id: None,
            fields: HashMap::new(),
        };
        assert_eq!(event.category.as_str(), "PLAYBACK");
    }

    #[test]
    fn damaged_manifest_is_quarantined_and_session_files_are_recovered() {
        let root = temporary_test_directory("logging-corrupt-manifest");
        let diagnostics = root.join("diagnostics");
        create_diagnostics_directories(&diagnostics).unwrap();
        let store = LogStore::start(&diagnostics, &init(&root), "attempt").unwrap();
        fs::write(&store.manifest_path, b"{damaged").unwrap();

        let manifest = store.read_manifest();

        assert!(manifest
            .sessions
            .iter()
            .any(|session| session.session_id == store.current_session_id()));
        assert!(fs::read_dir(diagnostics.join("state/corrupt"))
            .unwrap()
            .next()
            .is_some());
        fs::remove_dir_all(root).unwrap();
    }
}

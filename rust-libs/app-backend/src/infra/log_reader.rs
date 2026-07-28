use std::{
    collections::{HashSet, VecDeque},
    fs::{self, File},
    io::{self, BufRead, BufReader},
    path::Path,
};

use super::{
    logging::{LogStore, SessionsManifest},
    model::{
        DiagnosticLogEntry, DiagnosticLogFilter, DiagnosticLogPage, DiagnosticLogSession,
        DiagnosticLogSessionPage, DEFAULT_LOG_PAGE_SIZE, MAX_LOG_PAGE_SIZE,
    },
};

pub(crate) fn list_sessions(store: &LogStore, offset: i64, limit: i64) -> DiagnosticLogSessionPage {
    let mut sessions = store.read_manifest().sessions;
    refresh_sizes(&mut sessions);
    sessions.sort_by_key(|session| std::cmp::Reverse(session.started_at_epoch_ms));
    let total = sessions.len() as i64;
    let offset = offset.max(0) as usize;
    let limit = normalized_limit(limit) as usize;
    let page = sessions
        .into_iter()
        .skip(offset)
        .take(limit)
        .collect::<Vec<_>>();
    DiagnosticLogSessionPage {
        has_more: offset.saturating_add(page.len()) < total as usize,
        sessions: page,
        offset: offset as i64,
        limit: limit as i64,
        total,
    }
}

pub(crate) fn read_entries(store: &LogStore, filter: &DiagnosticLogFilter) -> DiagnosticLogPage {
    let manifest = store.read_manifest();
    let selected = select_sessions(&manifest, &filter.session_ids);
    let offset = filter.offset.max(0);
    let limit = normalized_limit(filter.limit);
    let mut entries = Vec::with_capacity(limit as usize);
    let mut total_matched = 0_i64;
    let mut warnings = Vec::new();

    for session in selected {
        for path in &session.log_paths {
            let Ok(file) = File::open(path) else {
                warnings.push(format!(
                    "Unable to read log file {}",
                    file_name_for_warning(path)
                ));
                continue;
            };
            for (line_number, line) in BufReader::new(file).lines().enumerate() {
                let Ok(line) = line else {
                    warnings.push(format!(
                        "Unreadable log line {}:{}",
                        file_name_for_warning(path),
                        line_number + 1
                    ));
                    continue;
                };
                let entry = match serde_json::from_str::<DiagnosticLogEntry>(&line) {
                    Ok(entry) => entry,
                    Err(_) => {
                        warnings.push(format!(
                            "Skipped damaged JSON log line {}:{}",
                            file_name_for_warning(path),
                            line_number + 1
                        ));
                        continue;
                    }
                };
                if !matches_filter(&entry, filter) {
                    continue;
                }
                if total_matched >= offset && entries.len() < limit as usize {
                    entries.push(entry);
                }
                total_matched = total_matched.saturating_add(1);
            }
        }
    }
    warnings.truncate(20);
    DiagnosticLogPage {
        has_more: offset.saturating_add(entries.len() as i64) < total_matched,
        entries,
        offset,
        limit,
        total_matched,
        warnings,
    }
}

pub(crate) fn read_tail(store: &LogStore, session_id: &str, limit: i64) -> DiagnosticLogPage {
    let limit = normalized_limit(limit);
    let manifest = store.read_manifest();
    let Some(session) = manifest
        .sessions
        .into_iter()
        .find(|session| session.session_id == session_id)
    else {
        return DiagnosticLogPage {
            entries: Vec::new(),
            offset: 0,
            limit,
            total_matched: 0,
            has_more: false,
            warnings: vec!["Log session not found".to_string()],
        };
    };
    let mut tail = VecDeque::with_capacity(limit as usize);
    let mut total = 0_i64;
    let mut warnings = Vec::new();
    for path in session.log_paths {
        let Ok(file) = File::open(&path) else {
            warnings.push(format!(
                "Unable to read log file {}",
                file_name_for_warning(&path)
            ));
            continue;
        };
        for (line_number, line) in BufReader::new(file).lines().enumerate() {
            let entry = match line
                .ok()
                .and_then(|line| serde_json::from_str::<DiagnosticLogEntry>(&line).ok())
            {
                Some(entry) => entry,
                None => {
                    warnings.push(format!(
                        "Skipped damaged JSON log line {}:{}",
                        file_name_for_warning(&path),
                        line_number + 1
                    ));
                    continue;
                }
            };
            total = total.saturating_add(1);
            if tail.len() == limit as usize {
                tail.pop_front();
            }
            tail.push_back(entry);
        }
    }
    let entries = tail.into_iter().collect::<Vec<_>>();
    warnings.truncate(20);
    DiagnosticLogPage {
        offset: total.saturating_sub(entries.len() as i64),
        entries,
        limit,
        total_matched: total,
        has_more: total > limit,
        warnings,
    }
}

pub(crate) fn clear_sessions(
    store: &LogStore,
    requested_ids: &[String],
    protected_ids: &HashSet<String>,
) -> io::Result<i64> {
    let requested = requested_ids.iter().collect::<HashSet<_>>();
    let current_id = store.current_session_id();
    let mut manifest = store.read_manifest();
    let mut removed = Vec::new();
    manifest.sessions.retain(|session| {
        let should_remove = requested.contains(&session.session_id)
            && session.session_id != current_id
            && !protected_ids.contains(&session.session_id);
        if should_remove {
            removed.push(session.clone());
        }
        !should_remove
    });
    store.replace_manifest(&manifest)?;
    for session in &removed {
        delete_session_files(store.sessions_dir(), session);
    }
    Ok(removed.len() as i64)
}

pub(crate) fn clear_all(store: &LogStore, protected_ids: &HashSet<String>) -> io::Result<i64> {
    let ids = store
        .read_manifest()
        .sessions
        .into_iter()
        .map(|session| session.session_id)
        .collect::<Vec<_>>();
    clear_sessions(store, &ids, protected_ids)
}

pub(crate) fn delete_session_files(sessions_dir: &Path, session: &DiagnosticLogSession) {
    for path in &session.log_paths {
        let path = Path::new(path);
        if path.starts_with(sessions_dir) {
            let _ = fs::remove_file(path);
        }
    }
}

fn select_sessions(
    manifest: &SessionsManifest,
    requested_ids: &[String],
) -> Vec<DiagnosticLogSession> {
    let requested = requested_ids.iter().collect::<HashSet<_>>();
    let mut sessions = manifest
        .sessions
        .iter()
        .filter(|session| requested.is_empty() || requested.contains(&session.session_id))
        .cloned()
        .collect::<Vec<_>>();
    sessions.sort_by_key(|session| std::cmp::Reverse(session.started_at_epoch_ms));
    sessions
}

fn matches_filter(entry: &DiagnosticLogEntry, filter: &DiagnosticLogFilter) -> bool {
    if !filter.levels.is_empty() && !filter.levels.contains(&entry.level) {
        return false;
    }
    if !filter.categories.is_empty() && !filter.categories.contains(&entry.category) {
        return false;
    }
    if filter
        .start_epoch_ms
        .is_some_and(|start| entry.timestamp_epoch_ms < start)
        || filter
            .end_epoch_ms
            .is_some_and(|end| entry.timestamp_epoch_ms > end)
    {
        return false;
    }
    if filter
        .correlation_id
        .as_deref()
        .is_some_and(|correlation| entry.correlation_id.as_deref() != Some(correlation))
    {
        return false;
    }
    let Some(keyword) = filter
        .keyword
        .as_deref()
        .map(str::trim)
        .filter(|keyword| !keyword.is_empty())
    else {
        return true;
    };
    let keyword = keyword.to_lowercase();
    entry.message.to_lowercase().contains(&keyword)
        || entry.target.to_lowercase().contains(&keyword)
        || entry
            .detail
            .as_deref()
            .is_some_and(|detail| detail.to_lowercase().contains(&keyword))
        || entry
            .correlation_id
            .as_deref()
            .is_some_and(|correlation| correlation.to_lowercase().contains(&keyword))
        || entry.fields.iter().any(|(key, value)| {
            key.to_lowercase().contains(&keyword) || value.to_lowercase().contains(&keyword)
        })
}

fn refresh_sizes(sessions: &mut [DiagnosticLogSession]) {
    for session in sessions {
        session.log_bytes = session
            .log_paths
            .iter()
            .map(|path| {
                fs::metadata(path)
                    .map(|metadata| metadata.len().min(i64::MAX as u64) as i64)
                    .unwrap_or(0)
            })
            .sum();
    }
}

fn normalized_limit(limit: i64) -> i64 {
    if limit <= 0 {
        DEFAULT_LOG_PAGE_SIZE
    } else {
        limit.min(MAX_LOG_PAGE_SIZE)
    }
}

fn file_name_for_warning(path: &str) -> String {
    Path::new(path)
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("unknown.jsonl")
        .to_string()
}

#[cfg(test)]
mod tests {
    use std::{
        collections::HashMap,
        fs::{self, OpenOptions},
        io::Write,
    };

    use super::*;
    use crate::infra::{
        file_ops::{create_diagnostics_directories, temporary_test_directory},
        logging::LogStore,
        model::{
            DiagnosticLogCategory, DiagnosticLogLevel, DiagnosticsRuntimeInit,
            DIAGNOSTICS_SCHEMA_VERSION,
        },
    };

    fn store(root: &Path) -> std::sync::Arc<LogStore> {
        let diagnostics = root.join("diagnostics");
        create_diagnostics_directories(&diagnostics).unwrap();
        LogStore::start(
            &diagnostics,
            &DiagnosticsRuntimeInit {
                app_document_dir: root.to_string_lossy().into_owned(),
                app_cache_dir: root.join("cache").to_string_lossy().into_owned(),
                platform: "test".into(),
                app_version: "1".into(),
                build_info: "debug".into(),
                git_commit_sha: "abc".into(),
                process_name: "test".into(),
                user_forced_safe_mode: false,
            },
            "attempt",
        )
        .unwrap()
    }

    #[test]
    fn paginates_filters_and_skips_damaged_lines() {
        let root = temporary_test_directory("log-reader");
        let store = store(&root);
        for index in 0..5 {
            store
                .write_entry(DiagnosticLogEntry {
                    schema_version: DIAGNOSTICS_SCHEMA_VERSION,
                    timestamp_epoch_ms: index,
                    level: if index == 3 {
                        DiagnosticLogLevel::Error
                    } else {
                        DiagnosticLogLevel::Info
                    },
                    category: DiagnosticLogCategory::Playback,
                    target: "Player".into(),
                    message: format!("entry {index}"),
                    detail: None,
                    session_id: store.current_session_id(),
                    correlation_id: Some("playback".into()),
                    startup_attempt_id: Some("attempt".into()),
                    thread: None,
                    platform: "test".into(),
                    fields: HashMap::new(),
                })
                .unwrap();
        }
        store.flush().unwrap();
        let path = store.read_manifest().sessions[0].log_paths[0].clone();
        let mut file = OpenOptions::new().append(true).open(path).unwrap();
        writeln!(file, "{{damaged").unwrap();
        let page = read_entries(
            &store,
            &DiagnosticLogFilter {
                levels: vec![DiagnosticLogLevel::Info],
                offset: 1,
                limit: 2,
                ..DiagnosticLogFilter::default()
            },
        );
        assert_eq!(page.entries.len(), 2);
        assert_eq!(page.total_matched, 4);
        assert!(page.has_more);
        assert_eq!(page.warnings.len(), 1);
        fs::remove_dir_all(root).unwrap();
    }
}

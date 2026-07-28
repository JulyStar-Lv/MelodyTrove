use std::{collections::HashSet, fs, io};

use super::{file_ops::now_epoch_ms, log_reader::delete_session_files, logging::LogStore};

const DAY_MS: i64 = 24 * 60 * 60 * 1_000;

pub(crate) fn enforce_retention(
    store: &LogStore,
    protected_ids: &HashSet<String>,
) -> io::Result<i64> {
    store.flush()?;
    let policy = store.retention_policy();
    let current_id = store.current_session_id();
    let mut manifest = store.read_manifest();
    for session in &mut manifest.sessions {
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
    manifest
        .sessions
        .sort_by_key(|session| session.started_at_epoch_ms);
    let cutoff = now_epoch_ms().saturating_sub(policy.retention_days.saturating_mul(DAY_MS));
    let mut total_bytes = manifest
        .sessions
        .iter()
        .map(|session| session.log_bytes)
        .sum::<i64>();
    let mut retained_count = manifest.sessions.len();
    let mut removed = Vec::new();
    manifest.sessions.retain(|session| {
        let protected = session.session_id == current_id
            || session.current
            || protected_ids.contains(&session.session_id);
        let expired = session.started_at_epoch_ms < cutoff;
        let over_count = retained_count > policy.max_sessions.max(1) as usize;
        let over_bytes = total_bytes > policy.max_total_bytes.max(1);
        let should_remove = !protected && (expired || over_count || over_bytes);
        if should_remove {
            retained_count = retained_count.saturating_sub(1);
            total_bytes = total_bytes.saturating_sub(session.log_bytes);
            removed.push(session.clone());
        }
        !should_remove
    });
    manifest
        .sessions
        .sort_by_key(|session| std::cmp::Reverse(session.started_at_epoch_ms));
    store.replace_manifest(&manifest)?;
    for session in &removed {
        delete_session_files(store.sessions_dir(), session);
    }
    Ok(removed.len() as i64)
}

#[cfg(test)]
mod tests {
    use std::{fs, path::Path};

    use super::*;
    use crate::infra::{
        file_ops::{create_diagnostics_directories, temporary_test_directory},
        logging::LogStore,
        model::{DiagnosticLogRetentionPolicy, DiagnosticLogSession, DiagnosticsRuntimeInit},
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
    fn retention_never_removes_current_or_incident_protected_session() {
        let root = temporary_test_directory("log-retention");
        let store = store(&root);
        let old_one_path = store.sessions_dir().join("old-one.jsonl");
        let old_two_path = store.sessions_dir().join("old-two.jsonl");
        fs::write(&old_one_path, b"one").unwrap();
        fs::write(&old_two_path, b"two").unwrap();
        let mut manifest = store.read_manifest();
        for (id, path) in [("old-one", old_one_path), ("old-two", old_two_path)] {
            manifest.sessions.push(DiagnosticLogSession {
                session_id: id.into(),
                started_at_epoch_ms: 1,
                ended_at_epoch_ms: Some(2),
                platform: "test".into(),
                app_version: "1".into(),
                build_info: String::new(),
                git_commit_sha: String::new(),
                process_name: String::new(),
                startup_attempt_id: String::new(),
                graceful_shutdown: false,
                current: false,
                log_paths: vec![path.to_string_lossy().into_owned()],
                log_bytes: 3,
            });
        }
        store.replace_manifest(&manifest).unwrap();
        store
            .set_retention_policy(DiagnosticLogRetentionPolicy {
                retention_days: 1,
                max_sessions: 2,
                max_total_bytes: 1024 * 1024,
                max_session_bytes: 1024 * 1024,
            })
            .unwrap();
        enforce_retention(&store, &HashSet::from(["old-one".to_string()])).unwrap();
        let retained = store
            .read_manifest()
            .sessions
            .into_iter()
            .map(|session| session.session_id)
            .collect::<HashSet<_>>();
        assert!(retained.contains(&store.current_session_id()));
        assert!(retained.contains("old-one"));
        assert!(!retained.contains("old-two"));
        fs::remove_dir_all(root).unwrap();
    }
}

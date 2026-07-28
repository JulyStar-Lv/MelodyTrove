use std::{
    fs, io,
    path::{Path, PathBuf},
    sync::{Mutex, MutexGuard},
};

use super::{
    file_ops::{atomic_write_json, new_id, now_epoch_ms, quarantine_corrupt_file, read_json},
    model::{StartupAttempt, StartupStage, DIAGNOSTICS_SCHEMA_VERSION},
};

const CURRENT_ATTEMPT_FILE: &str = "current-attempt.json";
const PREVIOUS_ATTEMPT_FILE: &str = "previous-attempt.json";

pub(crate) struct StartupJournal {
    startup_dir: PathBuf,
    corrupt_dir: PathBuf,
    current: Mutex<StartupAttempt>,
    previous: Option<StartupAttempt>,
}

impl StartupJournal {
    pub(crate) fn start(
        diagnostics_root: &Path,
        safe_mode: bool,
        safe_mode_reason: Option<String>,
    ) -> io::Result<Self> {
        let startup_dir = diagnostics_root.join("startup");
        let corrupt_dir = diagnostics_root.join("state/corrupt");
        fs::create_dir_all(startup_dir.join("history"))?;
        let current_path = startup_dir.join(CURRENT_ATTEMPT_FILE);
        let previous = if current_path.exists() {
            match read_json::<StartupAttempt>(&current_path) {
                Ok(attempt) => {
                    atomic_write_json(&startup_dir.join(PREVIOUS_ATTEMPT_FILE), &attempt)?;
                    atomic_write_json(
                        &startup_dir
                            .join("history")
                            .join(format!("{}.json", attempt.attempt_id)),
                        &attempt,
                    )?;
                    Some(attempt)
                }
                Err(_) => {
                    let _ = quarantine_corrupt_file(&current_path, &corrupt_dir);
                    None
                }
            }
        } else {
            read_json(&startup_dir.join(PREVIOUS_ATTEMPT_FILE)).ok()
        };

        let now = now_epoch_ms();
        let current = StartupAttempt {
            schema_version: DIAGNOSTICS_SCHEMA_VERSION,
            attempt_id: new_id("startup"),
            started_at_epoch_ms: now,
            last_updated_at_epoch_ms: now,
            last_stage: StartupStage::ProcessStarted,
            stable: false,
            graceful_shutdown: false,
            safe_mode,
            safe_mode_reason,
            recovery_attempted: false,
            disabled_components: if safe_mode {
                default_safe_mode_disabled_components()
            } else {
                Vec::new()
            },
        };
        atomic_write_json(&current_path, &current)?;
        Ok(Self {
            startup_dir,
            corrupt_dir,
            current: Mutex::new(current),
            previous,
        })
    }

    pub(crate) fn current(&self) -> StartupAttempt {
        self.current
            .lock()
            .map(|attempt| attempt.clone())
            .unwrap_or_else(|poisoned| poisoned.into_inner().clone())
    }

    pub(crate) fn previous(&self) -> Option<StartupAttempt> {
        self.previous.clone()
    }

    pub(crate) fn update_stage(&self, stage: StartupStage) -> io::Result<StartupAttempt> {
        let mut current = self.guard()?;
        if stage.rank() < current.last_stage.rank()
            && !matches!(
                stage,
                StartupStage::ShutdownStarted | StartupStage::ShutdownComplete
            )
        {
            return Err(io::Error::new(
                io::ErrorKind::InvalidInput,
                "startup stage cannot move backwards",
            ));
        }
        current.last_stage = stage;
        current.last_updated_at_epoch_ms = now_epoch_ms();
        if stage == StartupStage::StartupStable {
            current.stable = true;
        }
        if stage == StartupStage::ShutdownComplete {
            current.graceful_shutdown = true;
        }
        self.persist_locked(&current)?;
        Ok(current.clone())
    }

    pub(crate) fn mark_recovery_attempted(
        &self,
        disabled_components: Vec<String>,
    ) -> io::Result<StartupAttempt> {
        let mut current = self.guard()?;
        current.recovery_attempted = true;
        current.disabled_components = disabled_components;
        current.last_stage = StartupStage::PlatformExitsCollected;
        current.stable = false;
        current.graceful_shutdown = false;
        current.last_updated_at_epoch_ms = now_epoch_ms();
        self.persist_locked(&current)?;
        Ok(current.clone())
    }

    pub(crate) fn set_startup_mode(
        &self,
        enabled: bool,
        reason: Option<String>,
        disabled_components: Vec<String>,
    ) -> io::Result<StartupAttempt> {
        let mut current = self.guard()?;
        current.safe_mode = enabled;
        current.safe_mode_reason = reason;
        current.disabled_components = disabled_components;
        current.last_updated_at_epoch_ms = now_epoch_ms();
        self.persist_locked(&current)?;
        Ok(current.clone())
    }

    pub(crate) fn debug_mark_incomplete(&self) -> io::Result<StartupAttempt> {
        if !cfg!(debug_assertions) {
            return Err(io::Error::new(
                io::ErrorKind::PermissionDenied,
                "fault injection is disabled in release builds",
            ));
        }
        let mut current = self.guard()?;
        current.last_stage = StartupStage::BackendCreating;
        current.stable = false;
        current.graceful_shutdown = false;
        current.last_updated_at_epoch_ms = now_epoch_ms();
        self.persist_locked(&current)?;
        Ok(current.clone())
    }

    pub(crate) fn history(&self, limit: usize) -> Vec<StartupAttempt> {
        let history_dir = self.startup_dir.join("history");
        let Ok(entries) = fs::read_dir(history_dir) else {
            return Vec::new();
        };
        let mut attempts = entries
            .flatten()
            .filter_map(|entry| read_json::<StartupAttempt>(&entry.path()).ok())
            .collect::<Vec<_>>();
        attempts.sort_by_key(|attempt| std::cmp::Reverse(attempt.started_at_epoch_ms));
        attempts.truncate(limit);
        attempts
    }

    #[allow(dead_code)]
    pub(crate) fn corrupt_dir(&self) -> &Path {
        &self.corrupt_dir
    }

    fn persist_locked(&self, current: &StartupAttempt) -> io::Result<()> {
        atomic_write_json(&self.startup_dir.join(CURRENT_ATTEMPT_FILE), current)
    }

    fn guard(&self) -> io::Result<MutexGuard<'_, StartupAttempt>> {
        self.current
            .lock()
            .map_err(|_| io::Error::other("startup journal lock poisoned"))
    }
}

pub(crate) fn default_safe_mode_disabled_components() -> Vec<String> {
    [
        "application_backend",
        "database_migration",
        "playback_restore",
        "player",
        "dsp",
        "third_party_plugins",
        "automatic_scan",
        "background_sync",
        "metadata_refresh",
        "scheduled_backup",
    ]
    .into_iter()
    .map(str::to_string)
    .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::infra::file_ops::temporary_test_directory;

    #[test]
    fn stages_are_atomically_persisted_and_previous_attempt_is_preserved() {
        let root = temporary_test_directory("startup-journal");
        let diagnostics = root.join("diagnostics");
        fs::create_dir_all(diagnostics.join("state/corrupt")).unwrap();
        let journal = StartupJournal::start(&diagnostics, false, None).unwrap();
        let attempt_id = journal.current().attempt_id;
        journal
            .update_stage(StartupStage::DiagnosticsReady)
            .unwrap();
        journal.update_stage(StartupStage::StartupStable).unwrap();
        journal
            .update_stage(StartupStage::ShutdownComplete)
            .unwrap();

        let next = StartupJournal::start(&diagnostics, false, None).unwrap();
        let previous = next.previous().unwrap();
        assert_eq!(previous.attempt_id, attempt_id);
        assert!(previous.stable);
        assert!(previous.graceful_shutdown);
        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn stage_cannot_move_backwards() {
        let root = temporary_test_directory("startup-order");
        let diagnostics = root.join("diagnostics");
        fs::create_dir_all(diagnostics.join("state/corrupt")).unwrap();
        let journal = StartupJournal::start(&diagnostics, false, None).unwrap();
        journal.update_stage(StartupStage::BackendReady).unwrap();
        assert!(journal.update_stage(StartupStage::DatabaseOpening).is_err());
        fs::remove_dir_all(root).unwrap();
    }
}

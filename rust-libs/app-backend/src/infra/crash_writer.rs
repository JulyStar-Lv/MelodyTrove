use std::{
    io,
    sync::atomic::{AtomicBool, Ordering},
};

use super::{
    incidents::{IncidentContext, IncidentStore},
    logging::LogStore,
    model::{DiagnosticIncident, IncidentDraft, IncidentType},
};

static FATAL_WRITER_ACTIVE: AtomicBool = AtomicBool::new(false);

pub(crate) fn write_fatal_incident(
    incident_store: &IncidentStore,
    log_store: &LogStore,
    draft: IncidentDraft,
    context: &IncidentContext,
) -> io::Result<Option<DiagnosticIncident>> {
    let Some(_guard) = FatalWriterGuard::enter() else {
        return Ok(None);
    };
    let _ = log_store.flush();
    let artifact_name = artifact_name(draft.incident_type);
    let artifact = build_artifact(&draft, context);
    let incident =
        incident_store.create(draft, context, Some((artifact_name, artifact.as_bytes())))?;
    let _ = log_store.flush();
    Ok(Some(incident))
}

pub(crate) fn install_panic_hook<F>(handler: F)
where
    F: Fn(String, String, String) + Send + Sync + 'static,
{
    let previous = std::panic::take_hook();
    std::panic::set_hook(Box::new(move |info| {
        let message = if let Some(message) = info.payload().downcast_ref::<&str>() {
            (*message).to_string()
        } else if let Some(message) = info.payload().downcast_ref::<String>() {
            message.clone()
        } else {
            "Rust panic with non-string payload".to_string()
        };
        let location = info
            .location()
            .map(|location| {
                format!(
                    "{}:{}:{}",
                    location.file(),
                    location.line(),
                    location.column()
                )
            })
            .unwrap_or_else(|| "unknown".to_string());
        let backtrace = std::backtrace::Backtrace::force_capture().to_string();
        handler(message, location, backtrace);
        previous(info);
        #[cfg(not(test))]
        std::process::abort();
    }));
}

fn artifact_name(incident_type: IncidentType) -> &'static str {
    match incident_type {
        IncidentType::KotlinUncaught => "kotlin-crash.txt",
        IncidentType::RustPanic => "rust-panic.txt",
        IncidentType::DatabaseOpenFailure | IncidentType::DatabaseMigrationFailure => {
            "database-fatal.txt"
        }
        _ => "startup-fatal.txt",
    }
}

fn build_artifact(draft: &IncidentDraft, context: &IncidentContext) -> String {
    format!(
        "incidentType={:?}\nprocessName={}\nsessionId={}\nstartupAttemptId={}\nstartupStage={:?}\nsummary={}\ndetail={}\n",
        draft.incident_type,
        context.process_name,
        context.session_id,
        context.startup_attempt_id,
        context.startup_stage,
        draft.summary,
        draft.detail.as_deref().unwrap_or("none"),
    )
}

struct FatalWriterGuard;

impl FatalWriterGuard {
    fn enter() -> Option<Self> {
        FATAL_WRITER_ACTIVE
            .compare_exchange(false, true, Ordering::AcqRel, Ordering::Acquire)
            .ok()
            .map(|_| Self)
    }
}

impl Drop for FatalWriterGuard {
    fn drop(&mut self) {
        FATAL_WRITER_ACTIVE.store(false, Ordering::Release);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fatal_writer_reentry_guard_allows_only_one_writer() {
        let first = FatalWriterGuard::enter();
        assert!(first.is_some());
        assert!(FatalWriterGuard::enter().is_none());
        drop(first);
        assert!(FatalWriterGuard::enter().is_some());
    }
}

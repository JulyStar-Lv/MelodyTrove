use std::sync::Arc;

pub(crate) mod controllers;
pub(crate) mod ctx;
pub mod error;
pub mod infra;
mod objects;
mod order_key_api;
pub mod schema;
pub(crate) mod services;
pub(crate) mod utils;

pub use objects::*;
pub use schema::*;

use error::BResult;
pub use storage_backend::StreamFile;

pub use crate::services::{
    ArgAddMusicsToPlaylist, ArgCreatePlaylist, ArgInitializeApp, ArgRemoveMusicFromPlaylist,
    ArgUpdateMusicCover, ArgUpdateMusicDuration, ArgUpdateMusicLyric, ArgUpdatePlaylist,
    RemoteMusicScanSession, ToAddMusicEntry,
};
use crate::{
    ctx::BackendContext,
    infra::init_infra_compat,
    services::{app_bootstrap, app_destroy},
};

uniffi::setup_scaffolding!();

#[derive(uniffi::Object)]
pub struct Backend {
    arg: ArgInitializeApp,
    cx: Arc<BackendContext>,
}

impl Drop for Backend {
    fn drop(&mut self) {
        tracing::info!("drop Backend")
    }
}

#[uniffi::export]
impl Backend {
    pub fn init(&self) -> BResult<()> {
        app_bootstrap(&self.cx, self.arg.clone())?;
        Ok(())
    }

    pub fn deinit(&self) -> BResult<()> {
        app_destroy(&self.cx)?;
        Ok(())
    }
}

impl Backend {
    pub fn get_context(&self) -> &BackendContext {
        &self.cx
    }

    pub fn storage_path(&self) -> String {
        self.cx.get_storage_path()
    }
}

#[uniffi::export]
pub fn create_backend(arg: ArgInitializeApp) -> Arc<Backend> {
    let cx = Arc::new(BackendContext::new());
    let _ = init_infra_compat(&arg.app_document_dir);
    Arc::new(Backend { cx, arg })
}

#[uniffi::export]
pub fn backend_log(msg: &str) {
    if let Some(runtime) = infra::runtime_if_initialized() {
        let _ = runtime;
        let _ = infra::log_diagnostic_event(infra::DiagnosticLogEvent {
            level: infra::DiagnosticLogLevel::Info,
            category: infra::DiagnosticLogCategory::Unknown,
            target: "KotlinLegacy".to_string(),
            message: msg.to_string(),
            detail: None,
            correlation_id: None,
            fields: Default::default(),
        });
    }
}

#[uniffi::export]
pub fn backend_error(msg: &str) {
    if infra::runtime_if_initialized().is_some() {
        let _ = infra::log_diagnostic_event(infra::DiagnosticLogEvent {
            level: infra::DiagnosticLogLevel::Error,
            category: infra::DiagnosticLogCategory::Unknown,
            target: "KotlinLegacy".to_string(),
            message: msg.to_string(),
            detail: None,
            correlation_id: None,
            fields: Default::default(),
        });
    }
}

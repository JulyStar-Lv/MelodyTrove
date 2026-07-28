use crate::{ctx::BackendContext, error::BResult};

#[derive(Debug, Clone, uniffi::Record)]
pub struct ArgInitializeApp {
    pub app_document_dir: String,
    pub app_cache_dir: String,
    pub storage_path: String,
}

pub fn app_bootstrap(cx: &BackendContext, arg: ArgInitializeApp) -> BResult<()> {
    tracing::info!("app bootstrap: {:?}", arg);
    cx.set_storage_path(&arg.storage_path);
    cx.set_app_cache_dir(&arg.app_cache_dir);
    Ok(())
}

pub fn app_destroy(_cx: &BackendContext) -> BResult<()> {
    tracing::info!("app destroyed");
    Ok(())
}

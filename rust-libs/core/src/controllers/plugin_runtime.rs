use std::{
    fs,
    io::{Read, Write},
    path::{Component, Path, PathBuf},
    sync::Arc,
};
use tidetunes_plugin_runtime::{
    HostApi, HostApiOptions, PluginRuntime, PluginRuntimeError, PluginRuntimeOptions as RustOptions,
};

#[derive(Clone, uniffi::Record)]
pub struct PluginRuntimeOptions {
    pub plugin_id: String,
    pub plugin_name: String,
    pub app_name: String,
    pub package_name: String,
    pub app_version_name: String,
    pub app_version_code: u64,
    pub cache_directory: String,
    pub memory_limit_bytes: u64,
    pub stack_limit_bytes: u64,
    pub default_timeout_ms: u64,
    pub allow_http: bool,
    pub allow_https: bool,
    pub allow_private_network: bool,
    pub max_http_response_bytes: u64,
}
#[derive(Clone, uniffi::Record)]
pub struct PluginCallRequest {
    pub operation_id: u64,
    pub function_name: String,
    pub request_json: String,
    pub timeout_ms: u64,
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum PluginRuntimeException {
    #[error("closed: {0}")]
    Closed(String),
    #[error("initialization: {0}")]
    Initialization(String),
    #[error("script: {0}")]
    Script(String),
    #[error("function not found: {0}")]
    FunctionNotFound(String),
    #[error("invalid request: {0}")]
    InvalidRequest(String),
    #[error("timeout: {0}")]
    Timeout(String),
    #[error("cancelled: {0}")]
    Cancelled(String),
    #[error("out of memory: {0}")]
    OutOfMemory(String),
    #[error("host api: {0}")]
    HostApi(String),
    #[error("poisoned: {0}")]
    Poisoned(String),
    #[error("internal: {0}")]
    Internal(String),
}
impl From<PluginRuntimeError> for PluginRuntimeException {
    fn from(e: PluginRuntimeError) -> Self {
        let m = e.to_string();
        match e {
            PluginRuntimeError::Closed => Self::Closed(m),
            PluginRuntimeError::Initialization(_) => Self::Initialization(m),
            PluginRuntimeError::Script(_) => Self::Script(m),
            PluginRuntimeError::FunctionNotFound(_) => Self::FunctionNotFound(m),
            PluginRuntimeError::InvalidRequest(_) => Self::InvalidRequest(m),
            PluginRuntimeError::Timeout => Self::Timeout(m),
            PluginRuntimeError::Cancelled => Self::Cancelled(m),
            PluginRuntimeError::OutOfMemory => Self::OutOfMemory(m),
            PluginRuntimeError::HostApi(_) => Self::HostApi(m),
            PluginRuntimeError::Poisoned => Self::Poisoned(m),
            PluginRuntimeError::Internal(_) => Self::Internal(m),
        }
    }
}

#[derive(uniffi::Object)]
pub struct PluginRuntimeHandle {
    runtime: PluginRuntime,
}
#[uniffi::export]
impl PluginRuntimeHandle {
    pub fn load(&self, script: String, filename: String) -> Result<(), PluginRuntimeException> {
        self.runtime.load(script, filename).map_err(Into::into)
    }
    pub fn call_json(&self, request: PluginCallRequest) -> Result<String, PluginRuntimeException> {
        self.runtime
            .call_json(
                request.operation_id,
                request.function_name,
                request.request_json,
                request.timeout_ms,
            )
            .map_err(Into::into)
    }
    pub fn cancel_operation(&self, operation_id: u64) {
        self.runtime.cancel_operation(operation_id)
    }
    pub fn shutdown(&self) {
        self.runtime.close()
    }
}
#[uniffi::export]
pub fn create_plugin_runtime(
    options: PluginRuntimeOptions,
) -> Result<Arc<PluginRuntimeHandle>, PluginRuntimeException> {
    let host = HostApi::new(HostApiOptions {
        plugin_id: options.plugin_id,
        plugin_name: options.plugin_name,
        app_name: options.app_name,
        package_name: options.package_name,
        app_version_name: options.app_version_name,
        app_version_code: options.app_version_code,
        cache_directory: PathBuf::from(options.cache_directory),
        allow_http: options.allow_http,
        allow_https: options.allow_https,
        allow_private_network: options.allow_private_network,
        max_http_response_bytes: options.max_http_response_bytes as usize,
        ..Default::default()
    });
    let runtime = PluginRuntime::new(
        RustOptions {
            memory_limit_bytes: options.memory_limit_bytes,
            stack_limit_bytes: options.stack_limit_bytes,
            default_timeout_ms: options.default_timeout_ms,
        },
        Box::new(host),
    )
    .map_err(PluginRuntimeException::from)?;
    Ok(Arc::new(PluginRuntimeHandle { runtime }))
}

#[uniffi::export]
pub fn extract_plugin_zip(
    zip_path: String,
    destination_dir: String,
    max_files: u64,
    max_total_uncompressed_bytes: u64,
    max_depth: u64,
) -> Result<(), PluginRuntimeException> {
    let zip_path = PathBuf::from(zip_path);
    let destination_dir = PathBuf::from(destination_dir);
    extract_plugin_zip_impl(
        &zip_path,
        &destination_dir,
        max_files as usize,
        max_total_uncompressed_bytes,
        max_depth as usize,
    )
    .map_err(PluginRuntimeException::Internal)
}

fn extract_plugin_zip_impl(
    zip_path: &Path,
    destination_dir: &Path,
    max_files: usize,
    max_total_uncompressed_bytes: u64,
    max_depth: usize,
) -> Result<(), String> {
    fs::create_dir_all(destination_dir).map_err(|e| e.to_string())?;
    let destination_dir = destination_dir.canonicalize().map_err(|e| e.to_string())?;
    let file = fs::File::open(zip_path).map_err(|e| e.to_string())?;
    let mut archive = zip::ZipArchive::new(file).map_err(|e| e.to_string())?;
    if archive.len() > max_files {
        return Err("archive contains too many files".into());
    }

    let mut total = 0u64;
    let mut buffer = [0u8; 8192];
    for index in 0..archive.len() {
        let mut entry = archive.by_index(index).map_err(|e| e.to_string())?;
        if is_zip_symlink(&entry) {
            return Err(format!("zip entry is a symlink: {}", entry.name()));
        }
        let enclosed = entry
            .enclosed_name()
            .ok_or_else(|| format!("unsafe zip entry: {}", entry.name()))?
            .to_path_buf();
        let depth = enclosed
            .components()
            .filter(|component| matches!(component, Component::Normal(_)))
            .count();
        if depth == 0 || depth > max_depth {
            return Err(format!("zip entry depth is invalid: {}", entry.name()));
        }
        let output = destination_dir.join(&enclosed);
        if !is_under_or_same(&output, &destination_dir) {
            return Err(format!("unsafe zip entry: {}", entry.name()));
        }
        if entry.is_dir() {
            fs::create_dir_all(&output).map_err(|e| e.to_string())?;
            continue;
        }
        if let Some(parent) = output.parent() {
            fs::create_dir_all(parent).map_err(|e| e.to_string())?;
        }
        let mut out = fs::File::create(&output).map_err(|e| e.to_string())?;
        loop {
            let read = entry.read(&mut buffer).map_err(|e| e.to_string())?;
            if read == 0 {
                break;
            }
            total = total.saturating_add(read as u64);
            if total > max_total_uncompressed_bytes {
                return Err("archive is too large after extraction".into());
            }
            out.write_all(&buffer[..read]).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}

fn is_zip_symlink(entry: &zip::read::ZipFile<'_>) -> bool {
    entry
        .unix_mode()
        .map(|mode| mode & 0o170000 == 0o120000)
        .unwrap_or(false)
}

fn is_under_or_same(path: &Path, root: &Path) -> bool {
    path == root || path.starts_with(root)
}

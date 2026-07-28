use plugin_runtime::{
    HostApi, HostApiOptions, PluginRuntime, PluginRuntimeError, PluginRuntimeOptions as RustOptions,
};
use sha2::{Digest, Sha256};
use std::{
    fs,
    io::{Read, Write},
    path::{Component, Path, PathBuf},
    sync::Arc,
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
    pub load_timeout_ms: u64,
    pub allow_http: bool,
    pub allow_https: bool,
    pub allow_private_network: bool,
    pub max_http_request_bytes: u64,
    pub max_http_response_bytes: u64,
    pub max_plugin_cache_bytes: u64,
    pub max_inflate_bytes: u64,
}

#[derive(Clone, uniffi::Record)]
pub struct PluginLoadRequest {
    pub operation_id: u64,
    pub script: String,
    pub filename: String,
    pub timeout_ms: u64,
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
    fn from(error: PluginRuntimeError) -> Self {
        let message = error.to_string();
        match error {
            PluginRuntimeError::Closed => Self::Closed(message),
            PluginRuntimeError::Initialization(_) => Self::Initialization(message),
            PluginRuntimeError::Script(_) => Self::Script(message),
            PluginRuntimeError::FunctionNotFound(_) => Self::FunctionNotFound(message),
            PluginRuntimeError::InvalidRequest(_) => Self::InvalidRequest(message),
            PluginRuntimeError::Timeout => Self::Timeout(message),
            PluginRuntimeError::Cancelled => Self::Cancelled(message),
            PluginRuntimeError::OutOfMemory => Self::OutOfMemory(message),
            PluginRuntimeError::HostApi(_) => Self::HostApi(message),
            PluginRuntimeError::Poisoned => Self::Poisoned(message),
            PluginRuntimeError::Internal(_) => Self::Internal(message),
        }
    }
}

#[derive(uniffi::Object)]
pub struct PluginRuntimeHandle {
    runtime: PluginRuntime,
}

#[uniffi::export]
impl PluginRuntimeHandle {
    pub fn load(&self, request: PluginLoadRequest) -> Result<(), PluginRuntimeException> {
        self.runtime
            .load(
                request.operation_id,
                request.script,
                request.filename,
                request.timeout_ms,
            )
            .map_err(Into::into)
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
        self.runtime.cancel_operation(operation_id);
    }

    pub fn shutdown(&self) {
        self.runtime.close();
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
        max_http_request_bytes: options.max_http_request_bytes as usize,
        max_http_response_bytes: options.max_http_response_bytes as usize,
        max_cache_bytes: options.max_plugin_cache_bytes as usize,
        max_inflate_bytes: options.max_inflate_bytes as usize,
        ..Default::default()
    });
    let runtime = PluginRuntime::new(
        RustOptions {
            memory_limit_bytes: options.memory_limit_bytes,
            stack_limit_bytes: options.stack_limit_bytes,
            default_timeout_ms: options.default_timeout_ms,
            load_timeout_ms: options.load_timeout_ms,
        },
        Box::new(host),
    )
    .map_err(PluginRuntimeException::from)?;
    Ok(Arc::new(PluginRuntimeHandle { runtime }))
}

#[uniffi::export]
pub fn clear_plugin_cache(
    cache_directory: String,
    plugin_id: String,
) -> Result<(), PluginRuntimeException> {
    let hash = format!("{:x}", Sha256::digest(plugin_id.as_bytes()));
    let directory = PathBuf::from(cache_directory).join("plugins").join(hash);
    if directory.exists() {
        fs::remove_dir_all(directory)
            .map_err(|error| PluginRuntimeException::Internal(error.to_string()))?;
    }
    Ok(())
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
    fs::create_dir_all(destination_dir).map_err(|error| error.to_string())?;
    let destination_dir = destination_dir
        .canonicalize()
        .map_err(|error| error.to_string())?;
    let file = fs::File::open(zip_path).map_err(|error| error.to_string())?;
    let mut archive = zip::ZipArchive::new(file).map_err(|error| error.to_string())?;
    if archive.len() > max_files {
        return Err("archive contains too many files".into());
    }

    let mut total = 0_u64;
    let mut buffer = [0_u8; 8192];
    for index in 0..archive.len() {
        let mut entry = archive.by_index(index).map_err(|error| error.to_string())?;
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
            fs::create_dir_all(&output).map_err(|error| error.to_string())?;
            continue;
        }
        if let Some(parent) = output.parent() {
            fs::create_dir_all(parent).map_err(|error| error.to_string())?;
        }
        let mut out = fs::File::create(&output).map_err(|error| error.to_string())?;
        loop {
            let read = entry.read(&mut buffer).map_err(|error| error.to_string())?;
            if read == 0 {
                break;
            }
            total = total.saturating_add(read as u64);
            if total > max_total_uncompressed_bytes {
                return Err("archive is too large after extraction".into());
            }
            out.write_all(&buffer[..read])
                .map_err(|error| error.to_string())?;
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

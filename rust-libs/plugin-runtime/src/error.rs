#[derive(Debug, thiserror::Error, Clone, PartialEq, Eq)]
pub enum PluginRuntimeError {
    #[error("runtime is closed")]
    Closed,
    #[error("initialization failed: {0}")]
    Initialization(String),
    #[error("script failed: {0}")]
    Script(String),
    #[error("function not found: {0}")]
    FunctionNotFound(String),
    #[error("invalid request: {0}")]
    InvalidRequest(String),
    #[error("operation timed out")]
    Timeout,
    #[error("operation cancelled")]
    Cancelled,
    #[error("out of memory")]
    OutOfMemory,
    #[error("host API failed: {0}")]
    HostApi(String),
    #[error("runtime is poisoned")]
    Poisoned,
    #[error("internal error: {0}")]
    Internal(String),
}

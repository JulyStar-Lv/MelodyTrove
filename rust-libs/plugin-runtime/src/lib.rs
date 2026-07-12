mod control;
mod engine;
mod error;
mod host;
mod worker;
pub use control::OperationControl;
pub use error::PluginRuntimeError;
pub use host::{HostApi, HostApiOptions, SUPPORTED_HOST_APIS};
pub use worker::{PluginRuntime, PluginRuntimeOptions};
pub trait HostApiDispatcher: Send + 'static {
    fn call(
        &mut self,
        name: &str,
        payload_json: &str,
        control: &OperationControl,
    ) -> Result<String, PluginRuntimeError>;
}
pub struct NoopHostApi;
impl HostApiDispatcher for NoopHostApi {
    fn call(
        &mut self,
        name: &str,
        _: &str,
        _: &OperationControl,
    ) -> Result<String, PluginRuntimeError> {
        Err(PluginRuntimeError::HostApi(format!(
            "unsupported API: {name}"
        )))
    }
}

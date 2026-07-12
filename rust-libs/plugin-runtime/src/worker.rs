use crate::{engine::QuickJsEngine, HostApiDispatcher, OperationControl, PluginRuntimeError};
use flume::{Receiver, Sender};
use std::{
    sync::{Arc, Mutex},
    thread::{self, JoinHandle},
};
#[derive(Clone)]
pub struct PluginRuntimeOptions {
    pub memory_limit_bytes: u64,
    pub stack_limit_bytes: u64,
    pub default_timeout_ms: u64,
}
impl Default for PluginRuntimeOptions {
    fn default() -> Self {
        Self {
            memory_limit_bytes: 4 * 1024 * 1024,
            stack_limit_bytes: 2 * 1024 * 1024,
            default_timeout_ms: 5000,
        }
    }
}
enum RuntimeCommand {
    Load {
        script: String,
        filename: String,
        response: Sender<Result<(), PluginRuntimeError>>,
    },
    Call {
        operation_id: u64,
        function_name: String,
        request_json: String,
        timeout_ms: u64,
        response: Sender<Result<String, PluginRuntimeError>>,
    },
    Close {
        response: Sender<()>,
    },
}
pub struct PluginRuntime {
    sender: Sender<RuntimeCommand>,
    control: Arc<OperationControl>,
    join_handle: Mutex<Option<JoinHandle<()>>>,
}
impl PluginRuntime {
    pub fn new(
        options: PluginRuntimeOptions,
        host: Box<dyn HostApiDispatcher>,
    ) -> Result<Self, PluginRuntimeError> {
        let (sender, receiver) = flume::bounded(2);
        let control = Arc::new(OperationControl::default());
        let worker_control = control.clone();
        let join = thread::Builder::new()
            .name("tidetunes-plugin-runtime".into())
            .spawn(move || run(receiver, worker_control, options, host))
            .map_err(|e| PluginRuntimeError::Initialization(e.to_string()))?;
        Ok(Self {
            sender,
            control,
            join_handle: Mutex::new(Some(join)),
        })
    }
    pub fn load(&self, script: String, filename: String) -> Result<(), PluginRuntimeError> {
        let (tx, rx) = flume::bounded(1);
        self.sender
            .send(RuntimeCommand::Load {
                script,
                filename,
                response: tx,
            })
            .map_err(|_| PluginRuntimeError::Closed)?;
        rx.recv().unwrap_or(Err(PluginRuntimeError::Closed))
    }
    pub fn call_json(
        &self,
        operation_id: u64,
        function_name: String,
        request_json: String,
        timeout_ms: u64,
    ) -> Result<String, PluginRuntimeError> {
        let (tx, rx) = flume::bounded(1);
        self.sender
            .try_send(RuntimeCommand::Call {
                operation_id,
                function_name,
                request_json,
                timeout_ms,
                response: tx,
            })
            .map_err(|e| match e {
                flume::TrySendError::Full(_) => {
                    PluginRuntimeError::Internal("runtime queue is full".into())
                }
                flume::TrySendError::Disconnected(_) => PluginRuntimeError::Closed,
            })?;
        rx.recv().unwrap_or(Err(PluginRuntimeError::Closed))
    }
    pub fn cancel_operation(&self, id: u64) {
        self.control.cancel(id)
    }
    pub fn close(&self) {
        self.control.close();
        if let Ok(mut h) = self.join_handle.lock() {
            if let Some(j) = h.take() {
                let (tx, rx) = flume::bounded(1);
                let _ = self.sender.send(RuntimeCommand::Close { response: tx });
                let _ = rx.recv();
                let _ = j.join();
            }
        }
    }
}
fn run(
    rx: Receiver<RuntimeCommand>,
    control: Arc<OperationControl>,
    options: PluginRuntimeOptions,
    host: Box<dyn HostApiDispatcher>,
) {
    let mut engine = match QuickJsEngine::new(
        options.memory_limit_bytes,
        options.stack_limit_bytes,
        control.clone(),
        host,
    ) {
        Ok(x) => x,
        Err(_) => return,
    };
    while let Ok(cmd) = rx.recv() {
        match cmd {
            RuntimeCommand::Load {
                script,
                filename,
                response,
            } => {
                let _ = response.send(engine.eval(&script, &filename));
            }
            RuntimeCommand::Call {
                operation_id,
                function_name,
                request_json,
                timeout_ms,
                response,
            } => {
                control.begin(
                    operation_id,
                    if timeout_ms == 0 {
                        options.default_timeout_ms
                    } else {
                        timeout_ms
                    },
                );
                let result = engine.call(&function_name, &request_json);
                let should_destroy = matches!(
                    result,
                    Err(
                        PluginRuntimeError::Timeout
                            | PluginRuntimeError::OutOfMemory
                            | PluginRuntimeError::Poisoned
                    )
                ) || control.poisoned.load(std::sync::atomic::Ordering::Acquire);
                if should_destroy {
                    control
                        .poisoned
                        .store(true, std::sync::atomic::Ordering::Release)
                }
                control.finish();
                let _ = response.send(result);
                if should_destroy {
                    break;
                }
            }
            RuntimeCommand::Close { response } => {
                let _ = response.send(());
                break;
            }
        }
    }
}
impl Drop for PluginRuntime {
    fn drop(&mut self) {
        self.close()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::NoopHostApi;
    fn runtime() -> PluginRuntime {
        PluginRuntime::new(Default::default(), Box::new(NoopHostApi)).unwrap()
    }
    #[test]
    fn eval_and_call_json() {
        let r = runtime();
        r.load(
            "function echo(x){return x} function nope(){return undefined}".into(),
            "test.js".into(),
        )
        .unwrap();
        assert_eq!(
            r.call_json(1, "echo".into(), r#"{"a":1}"#.into(), 1000)
                .unwrap(),
            r#"{"a":1}"#
        );
        assert_eq!(
            r.call_json(2, "nope".into(), "{}".into(), 1000).unwrap(),
            "null"
        );
    }
    #[test]
    fn errors_and_timeout() {
        let r = runtime();
        r.load("function loop(){while(true){}}".into(), "test.js".into())
            .unwrap();
        assert!(matches!(
            r.call_json(1, "missing".into(), "{}".into(), 1000),
            Err(PluginRuntimeError::FunctionNotFound(_))
        ));
        assert!(matches!(
            r.call_json(2, "loop".into(), "{}".into(), 10),
            Err(PluginRuntimeError::Timeout)
        ));
    }
}

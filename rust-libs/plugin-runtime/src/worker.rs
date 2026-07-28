use crate::{engine::QuickJsEngine, HostApiDispatcher, OperationControl, PluginRuntimeError};
use flume::{Receiver, Sender};
use std::{
    sync::{atomic::Ordering, Arc, Mutex},
    thread::{self, JoinHandle},
    time::{Duration, Instant},
};

const CLOSE_JOIN_TIMEOUT: Duration = Duration::from_secs(2);
const CLOSE_JOIN_POLL_INTERVAL: Duration = Duration::from_millis(10);

#[derive(Clone)]
pub struct PluginRuntimeOptions {
    pub memory_limit_bytes: u64,
    pub stack_limit_bytes: u64,
    pub default_timeout_ms: u64,
    pub load_timeout_ms: u64,
}

impl Default for PluginRuntimeOptions {
    fn default() -> Self {
        Self {
            memory_limit_bytes: 32 * 1024 * 1024,
            stack_limit_bytes: 2 * 1024 * 1024,
            default_timeout_ms: 15_000,
            load_timeout_ms: 10_000,
        }
    }
}

enum RuntimeCommand {
    Load {
        operation_id: u64,
        script: String,
        filename: String,
        timeout_ms: u64,
        response: Sender<Result<(), PluginRuntimeError>>,
    },
    Call {
        operation_id: u64,
        function_name: String,
        request_json: String,
        timeout_ms: u64,
        response: Sender<Result<String, PluginRuntimeError>>,
    },
    Close,
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
            .name("plugin-runtime".into())
            .spawn(move || run(receiver, worker_control, options, host))
            .map_err(|error| PluginRuntimeError::Initialization(error.to_string()))?;
        Ok(Self {
            sender,
            control,
            join_handle: Mutex::new(Some(join)),
        })
    }

    pub fn load(
        &self,
        operation_id: u64,
        script: String,
        filename: String,
        timeout_ms: u64,
    ) -> Result<(), PluginRuntimeError> {
        let (response, receiver) = flume::bounded(1);
        self.try_send(RuntimeCommand::Load {
            operation_id,
            script,
            filename,
            timeout_ms,
            response,
        })?;
        receiver.recv().unwrap_or(Err(PluginRuntimeError::Closed))
    }

    pub fn call_json(
        &self,
        operation_id: u64,
        function_name: String,
        request_json: String,
        timeout_ms: u64,
    ) -> Result<String, PluginRuntimeError> {
        let (response, receiver) = flume::bounded(1);
        self.try_send(RuntimeCommand::Call {
            operation_id,
            function_name,
            request_json,
            timeout_ms,
            response,
        })?;
        receiver.recv().unwrap_or(Err(PluginRuntimeError::Closed))
    }

    pub fn cancel_operation(&self, operation_id: u64) {
        self.control.cancel(operation_id);
    }

    pub fn close(&self) {
        self.control.close();
        let join = self
            .join_handle
            .lock()
            .ok()
            .and_then(|mut handle| handle.take());
        let Some(join) = join else {
            return;
        };

        let _ = self.sender.try_send(RuntimeCommand::Close);
        let deadline = Instant::now() + CLOSE_JOIN_TIMEOUT;
        while !join.is_finished() && Instant::now() < deadline {
            thread::sleep(CLOSE_JOIN_POLL_INTERVAL);
        }
        if join.is_finished() {
            let _ = join.join();
        }
    }

    fn try_send(&self, command: RuntimeCommand) -> Result<(), PluginRuntimeError> {
        if self.control.is_unavailable() {
            return Err(PluginRuntimeError::Closed);
        }
        self.sender.try_send(command).map_err(|error| match error {
            flume::TrySendError::Full(_) => {
                PluginRuntimeError::Internal("runtime queue is full".into())
            }
            flume::TrySendError::Disconnected(_) => PluginRuntimeError::Closed,
        })
    }
}

fn run(
    receiver: Receiver<RuntimeCommand>,
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
        Ok(engine) => engine,
        Err(_) => return,
    };

    while let Ok(command) = receiver.recv() {
        match command {
            RuntimeCommand::Load {
                operation_id,
                script,
                filename,
                timeout_ms,
                response,
            } => {
                control.begin(
                    operation_id,
                    effective_timeout(timeout_ms, options.load_timeout_ms),
                );
                let result = engine.eval(&script, &filename);
                let should_destroy =
                    should_destroy_after_load(&result) || control.poisoned.load(Ordering::Acquire);
                if should_destroy {
                    control.poisoned.store(true, Ordering::Release);
                }
                control.finish(operation_id);
                let _ = response.send(result);
                if should_destroy {
                    break;
                }
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
                    effective_timeout(timeout_ms, options.default_timeout_ms),
                );
                let result = engine.call(&function_name, &request_json);
                let should_destroy =
                    should_destroy_after_call(&result) || control.poisoned.load(Ordering::Acquire);
                if should_destroy {
                    control.poisoned.store(true, Ordering::Release);
                }
                control.finish(operation_id);
                let _ = response.send(result);
                if should_destroy {
                    break;
                }
            }
            RuntimeCommand::Close => break,
        }
    }
}

fn effective_timeout(requested: u64, fallback: u64) -> u64 {
    if requested == 0 {
        fallback.max(1)
    } else {
        requested
    }
}

fn should_destroy_after_load<T>(result: &Result<T, PluginRuntimeError>) -> bool {
    matches!(
        result,
        Err(PluginRuntimeError::Timeout
            | PluginRuntimeError::Cancelled
            | PluginRuntimeError::Closed
            | PluginRuntimeError::OutOfMemory
            | PluginRuntimeError::Poisoned
            | PluginRuntimeError::Internal(_))
    )
}

fn should_destroy_after_call<T>(result: &Result<T, PluginRuntimeError>) -> bool {
    matches!(
        result,
        Err(PluginRuntimeError::Timeout
            | PluginRuntimeError::Cancelled
            | PluginRuntimeError::Closed
            | PluginRuntimeError::OutOfMemory
            | PluginRuntimeError::Poisoned
            | PluginRuntimeError::Internal(_))
    )
}

impl Drop for PluginRuntime {
    fn drop(&mut self) {
        self.close();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::NoopHostApi;

    fn runtime() -> PluginRuntime {
        PluginRuntime::new(Default::default(), Box::new(NoopHostApi)).unwrap()
    }

    fn load(runtime: &PluginRuntime, script: &str) {
        runtime
            .load(1, script.into(), "test.js".into(), 1_000)
            .unwrap();
    }

    #[test]
    fn eval_and_call_json() {
        let runtime = runtime();
        load(
            &runtime,
            "function echo(x){return x} function nope(){return undefined}",
        );
        assert_eq!(
            runtime
                .call_json(2, "echo".into(), r#"{"a":1}"#.into(), 1_000)
                .unwrap(),
            r#"{"a":1}"#
        );
        assert_eq!(
            runtime
                .call_json(3, "nope".into(), "{}".into(), 1_000)
                .unwrap(),
            "null"
        );
    }

    #[test]
    fn return_values_are_normalized_without_double_encoding() {
        let runtime = runtime();
        load(
            &runtime,
            r#"
                function jsonString(){return JSON.stringify([{id:"1",title:"Song"}])}
                function objectValue(){return {id:"1",title:"Song"}}
                function arrayValue(){return [{id:"1"}]}
                function nullValue(){return null}
                function undefinedValue(){return undefined}
                function numberValue(){return 7}
                function booleanValue(){return true}
            "#,
        );

        assert_eq!(
            runtime
                .call_json(2, "jsonString".into(), "{}".into(), 1_000)
                .unwrap(),
            r#"[{"id":"1","title":"Song"}]"#
        );
        assert_eq!(
            runtime
                .call_json(3, "objectValue".into(), "{}".into(), 1_000)
                .unwrap(),
            r#"{"id":"1","title":"Song"}"#
        );
        assert_eq!(
            runtime
                .call_json(4, "arrayValue".into(), "{}".into(), 1_000)
                .unwrap(),
            r#"[{"id":"1"}]"#
        );
        assert_eq!(
            runtime
                .call_json(5, "nullValue".into(), "{}".into(), 1_000)
                .unwrap(),
            "null"
        );
        assert_eq!(
            runtime
                .call_json(6, "undefinedValue".into(), "{}".into(), 1_000)
                .unwrap(),
            "null"
        );
        assert_eq!(
            runtime
                .call_json(7, "numberValue".into(), "{}".into(), 1_000)
                .unwrap(),
            "7"
        );
        assert_eq!(
            runtime
                .call_json(8, "booleanValue".into(), "{}".into(), 1_000)
                .unwrap(),
            "true"
        );
    }

    #[test]
    fn errors_and_call_timeout() {
        let runtime = runtime();
        load(&runtime, "function loop(){while(true){}}");
        assert!(matches!(
            runtime.call_json(2, "missing".into(), "{}".into(), 1_000),
            Err(PluginRuntimeError::FunctionNotFound(_))
        ));
        assert!(matches!(
            runtime.call_json(3, "loop".into(), "{}".into(), 10),
            Err(PluginRuntimeError::Timeout)
        ));
        assert!(matches!(
            runtime.call_json(4, "loop".into(), "{}".into(), 10),
            Err(PluginRuntimeError::Closed)
        ));
    }

    #[test]
    fn top_level_infinite_loop_times_out_and_destroys_worker() {
        let runtime = runtime();
        assert!(matches!(
            runtime.load(1, "while(true){}".into(), "loop.js".into(), 10),
            Err(PluginRuntimeError::Timeout)
        ));
        assert!(matches!(
            runtime.call_json(2, "anything".into(), "{}".into(), 10),
            Err(PluginRuntimeError::Closed)
        ));
    }

    #[test]
    fn cancellation_before_worker_begin_is_honoured() {
        let runtime = Arc::new(runtime());
        load(&runtime, "function loop(){while(true){}}");
        let worker_runtime = runtime.clone();
        let call =
            thread::spawn(move || worker_runtime.call_json(99, "loop".into(), "{}".into(), 5_000));
        runtime.cancel_operation(99);

        assert!(matches!(
            call.join().unwrap(),
            Err(PluginRuntimeError::Cancelled)
        ));
    }

    #[test]
    fn close_is_idempotent() {
        let runtime = runtime();
        runtime.close();
        runtime.close();
    }
}

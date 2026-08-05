use once_cell::sync::Lazy;
use tokio::runtime::{self, Runtime};

static RUNTIME: Lazy<Runtime> = Lazy::new(|| {
    runtime::Builder::new_multi_thread()
        .worker_threads(4)
        .enable_all()
        .build()
        .expect("Failed to create Tokio runtime")
});

pub fn tokio_runtime() -> &'static Runtime {
    &RUNTIME
}

#[cfg(test)]
mod tests {
    use super::tokio_runtime;
    use std::{sync::mpsc, time::Duration};

    #[test]
    fn spawned_task_progresses_without_explicitly_driving_the_runtime() {
        let (sender, receiver) = mpsc::channel();

        std::mem::drop(tokio_runtime().spawn(async move {
            sender.send(()).unwrap();
        }));

        receiver
            .recv_timeout(Duration::from_secs(1))
            .expect("spawned Tokio task did not make progress");
    }
}

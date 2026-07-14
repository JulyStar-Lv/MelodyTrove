use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Default)]
pub struct OperationControl {
    pub active_operation_id: AtomicU64,
    pub deadline_millis: AtomicU64,
    cancelled_operation_id: AtomicU64,
    pub poisoned: AtomicBool,
    closed: AtomicBool,
}

impl OperationControl {
    pub fn begin(&self, id: u64, timeout_ms: u64) {
        self.deadline_millis
            .store(now().saturating_add(timeout_ms), Ordering::Release);
        self.active_operation_id.store(id, Ordering::Release);
    }

    pub fn finish(&self, id: u64) {
        if self
            .active_operation_id
            .compare_exchange(id, 0, Ordering::AcqRel, Ordering::Acquire)
            .is_ok()
        {
            self.deadline_millis.store(0, Ordering::Release);
            let _ = self.cancelled_operation_id.compare_exchange(
                id,
                0,
                Ordering::AcqRel,
                Ordering::Acquire,
            );
        }
    }

    pub fn cancel(&self, id: u64) {
        if id != 0 {
            self.cancelled_operation_id.store(id, Ordering::Release);
        }
    }

    pub fn close(&self) {
        self.closed.store(true, Ordering::Release);
    }

    pub fn is_unavailable(&self) -> bool {
        self.closed.load(Ordering::Acquire) || self.poisoned.load(Ordering::Acquire)
    }

    pub fn should_interrupt(&self) -> bool {
        let active_id = self.active_operation_id.load(Ordering::Acquire);
        self.closed.load(Ordering::Acquire)
            || self.poisoned.load(Ordering::Acquire)
            || (active_id != 0 && self.cancelled_operation_id.load(Ordering::Acquire) == active_id)
            || {
                let deadline = self.deadline_millis.load(Ordering::Acquire);
                deadline != 0 && now() >= deadline
            }
    }

    pub fn interrupted_error(&self) -> crate::PluginRuntimeError {
        if self.closed.load(Ordering::Acquire) {
            crate::PluginRuntimeError::Closed
        } else {
            let active_id = self.active_operation_id.load(Ordering::Acquire);
            if active_id != 0 && self.cancelled_operation_id.load(Ordering::Acquire) == active_id {
                crate::PluginRuntimeError::Cancelled
            } else if self.poisoned.load(Ordering::Acquire) {
                crate::PluginRuntimeError::Poisoned
            } else {
                crate::PluginRuntimeError::Timeout
            }
        }
    }
}

fn now() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cancellation_before_begin_is_not_lost() {
        let control = OperationControl::default();
        control.cancel(42);
        control.begin(42, 1_000);

        assert!(control.should_interrupt());
        assert_eq!(
            control.interrupted_error(),
            crate::PluginRuntimeError::Cancelled
        );
    }

    #[test]
    fn stale_cancellation_does_not_cancel_new_operation() {
        let control = OperationControl::default();
        control.cancel(41);
        control.begin(42, 1_000);

        assert!(!control.should_interrupt());
        control.finish(42);
    }
}

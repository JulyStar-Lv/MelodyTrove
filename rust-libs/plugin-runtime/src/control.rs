use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};
#[derive(Default)]
pub struct OperationControl {
    pub active_operation_id: AtomicU64,
    pub deadline_millis: AtomicU64,
    pub cancelled: AtomicBool,
    pub poisoned: AtomicBool,
    closed: AtomicBool,
}
impl OperationControl {
    pub fn begin(&self, id: u64, timeout: u64) {
        self.active_operation_id.store(id, Ordering::Release);
        self.cancelled.store(false, Ordering::Release);
        self.deadline_millis
            .store(now().saturating_add(timeout), Ordering::Release)
    }
    pub fn finish(&self) {
        self.active_operation_id.store(0, Ordering::Release);
        self.deadline_millis.store(0, Ordering::Release);
        self.cancelled.store(false, Ordering::Release)
    }
    pub fn cancel(&self, id: u64) {
        if self.active_operation_id.load(Ordering::Acquire) == id {
            self.cancelled.store(true, Ordering::Release)
        }
    }
    pub fn close(&self) {
        self.closed.store(true, Ordering::Release)
    }
    pub fn should_interrupt(&self) -> bool {
        self.closed.load(Ordering::Acquire)
            || self.poisoned.load(Ordering::Acquire)
            || self.cancelled.load(Ordering::Acquire)
            || {
                let d = self.deadline_millis.load(Ordering::Acquire);
                d != 0 && now() > d
            }
    }
    pub fn interrupted_error(&self) -> crate::PluginRuntimeError {
        if self.cancelled.load(Ordering::Acquire) {
            crate::PluginRuntimeError::Cancelled
        } else if self.poisoned.load(Ordering::Acquire) {
            crate::PluginRuntimeError::Poisoned
        } else {
            crate::PluginRuntimeError::Timeout
        }
    }
}
fn now() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}

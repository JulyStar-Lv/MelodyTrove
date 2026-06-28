use std::{
    fmt::Debug,
    sync::{Arc, RwLock, Weak},
    time::Duration,
};

struct BackendContextInternal {
    storage_path: RwLock<String>,
    app_cache_dir: RwLock<String>,
}

impl Drop for BackendContextInternal {
    fn drop(&mut self) {
        tracing::info!("drop BackendContextInternal")
    }
}

pub struct BackendContext {
    internal: Arc<BackendContextInternal>,
}

#[derive(Clone)]
pub struct WeakBackendContext {
    internal: Weak<BackendContextInternal>,
}

impl Debug for BackendContext {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("BackendContext")
            .field("storage_path", &self.internal.storage_path)
            .field("app_cache_dir", &self.internal.app_cache_dir)
            .finish()
    }
}

impl WeakBackendContext {
    pub fn upgrade(&self) -> Option<BackendContext> {
        self.internal
            .upgrade()
            .map(|internal| BackendContext { internal })
    }
}

impl Default for BackendContext {
    fn default() -> Self {
        Self::new()
    }
}

impl BackendContext {
    pub fn new() -> Self {
        Self {
            internal: Arc::new(BackendContextInternal {
                storage_path: RwLock::new(String::new()),
                app_cache_dir: RwLock::new(String::new()),
            }),
        }
    }

    pub fn weak(&self) -> WeakBackendContext {
        WeakBackendContext {
            internal: Arc::downgrade(&self.internal),
        }
    }

    pub fn current_time(&self) -> Duration {
        std::time::UNIX_EPOCH.elapsed().unwrap()
    }

    pub fn set_storage_path(&self, p: &str) {
        let mut w = self.internal.storage_path.write().unwrap();
        *w = p.to_string();
    }

    pub fn get_storage_path(&self) -> String {
        self.internal.storage_path.read().unwrap().clone()
    }

    pub fn set_app_cache_dir(&self, p: &str) {
        let mut w = self.internal.app_cache_dir.write().unwrap();
        *w = p.to_string();
    }

    pub fn get_app_cache_dir(&self) -> String {
        self.internal.app_cache_dir.read().unwrap().clone()
    }
}

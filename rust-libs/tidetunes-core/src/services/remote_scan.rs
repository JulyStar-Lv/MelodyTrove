use std::{
    collections::VecDeque,
    sync::{
        atomic::{AtomicBool, Ordering},
        Arc,
    },
};

use tidetunes_remote_storage::{Entry, StorageBackend};
use tokio::sync::{Mutex, Notify};

use crate::{
    error::{BError, BResult},
    objects::{RemoteMusicScanBatch, StorageEntry},
    schema::StorageId,
};

const MAX_SCAN_BATCH_FILES: u32 = 1_000;
const MAX_SCAN_ENTRIES: u64 = 100_000;

struct RemoteMusicScanState {
    directories: VecDeque<String>,
    pending_entries: VecDeque<Entry>,
    listed_directory_count: u64,
    visited_entry_count: u64,
    done: bool,
}

#[derive(uniffi::Object)]
pub struct RemoteMusicScanSession {
    storage_id: StorageId,
    backend: Arc<dyn StorageBackend + Send + Sync>,
    state: Mutex<RemoteMusicScanState>,
    cancelled: AtomicBool,
    cancel_notify: Notify,
}

#[uniffi::export]
impl RemoteMusicScanSession {
    pub async fn next_batch(&self, max_files: u32) -> BResult<RemoteMusicScanBatch> {
        if !(1..=MAX_SCAN_BATCH_FILES).contains(&max_files) {
            return Err(BError::CustomError {
                message: format!(
                    "remote scan batch size must be between 1 and {MAX_SCAN_BATCH_FILES}"
                ),
            });
        }

        let mut state = self.state.lock().await;
        if state.done || self.cancelled.load(Ordering::Acquire) {
            state.done = true;
            state.directories.clear();
            state.pending_entries.clear();
            return Ok(self.batch(&state, Vec::new()));
        }

        let mut files = Vec::with_capacity(max_files as usize);
        loop {
            if self.cancelled.load(Ordering::Acquire) {
                state.done = true;
                state.directories.clear();
                state.pending_entries.clear();
                break;
            }

            while files.len() < max_files as usize {
                let Some(entry) = state.pending_entries.pop_front() else {
                    break;
                };
                state.visited_entry_count += 1;
                if state.visited_entry_count > MAX_SCAN_ENTRIES {
                    state.done = true;
                    state.directories.clear();
                    state.pending_entries.clear();
                    return Err(BError::CustomError {
                        message: format!(
                            "remote scan exceeded the {MAX_SCAN_ENTRIES} entry safety limit"
                        ),
                    });
                }

                if entry.is_dir {
                    state.directories.push_back(entry.path);
                } else if is_supported_music_path(&entry.path) {
                    files.push(storage_entry(self.storage_id, entry));
                }
            }

            if files.len() == max_files as usize {
                break;
            }

            let Some(directory) = state.directories.pop_front() else {
                state.done = state.pending_entries.is_empty();
                break;
            };
            let list_result = tokio::select! {
                result = self.backend.list(directory.clone()) => Some(result),
                _ = self.cancel_notify.notified() => None,
            };
            let Some(list_result) = list_result else {
                state.done = true;
                state.directories.clear();
                state.pending_entries.clear();
                break;
            };
            match list_result {
                Ok(entries) => {
                    state.listed_directory_count += 1;
                    state.pending_entries.extend(entries);
                }
                Err(error) => {
                    state.directories.push_front(directory);
                    return Err(error.into());
                }
            }
        }

        Ok(self.batch(&state, files))
    }

    pub fn cancel(&self) {
        self.cancelled.store(true, Ordering::Release);
        self.cancel_notify.notify_one();
    }

    pub fn is_cancelled(&self) -> bool {
        self.cancelled.load(Ordering::Acquire)
    }
}

impl Drop for RemoteMusicScanSession {
    fn drop(&mut self) {
        self.cancelled.store(true, Ordering::Release);
        self.cancel_notify.notify_waiters();
    }
}

impl RemoteMusicScanSession {
    pub fn new(
        storage_id: StorageId,
        backend: Arc<dyn StorageBackend + Send + Sync>,
        root: String,
    ) -> Arc<Self> {
        Arc::new(Self {
            storage_id,
            backend,
            state: Mutex::new(RemoteMusicScanState {
                directories: VecDeque::from([root]),
                pending_entries: VecDeque::new(),
                listed_directory_count: 0,
                visited_entry_count: 0,
                done: false,
            }),
            cancelled: AtomicBool::new(false),
            cancel_notify: Notify::new(),
        })
    }

    fn batch(
        &self,
        state: &RemoteMusicScanState,
        entries: Vec<StorageEntry>,
    ) -> RemoteMusicScanBatch {
        RemoteMusicScanBatch {
            entries,
            done: state.done,
            cancelled: self.cancelled.load(Ordering::Acquire),
            listed_directory_count: state.listed_directory_count,
            visited_entry_count: state.visited_entry_count,
        }
    }
}

pub(crate) fn storage_entry(storage_id: StorageId, entry: Entry) -> StorageEntry {
    StorageEntry {
        storage_id,
        name: entry.name,
        path: entry.path,
        size: entry.size.map(|size| size as u64),
        is_dir: entry.is_dir,
        remote_id: entry.remote_id,
        parent_remote_id: entry.parent_remote_id,
        mime_type: entry.mime_type,
        etag: entry.etag,
        ctag: entry.ctag,
        created_at: entry.created_at,
        modified_at: entry.modified_at,
    }
}

pub(crate) fn is_supported_music_path(path: &str) -> bool {
    let lower_path = path.to_ascii_lowercase();
    [
        ".mp3", ".flac", ".m4a", ".mp4", ".aac", ".ogg", ".oga", ".opus", ".wav", ".aif", ".aiff",
    ]
    .iter()
    .any(|suffix| lower_path.ends_with(suffix))
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;

    use bytes::Bytes;
    use futures_util::future::BoxFuture;
    use tidetunes_remote_storage::{
        ByteRange, RangeResponse, StorageBackendError, StorageBackendResult, StreamFile,
    };

    use super::*;

    struct MemoryStorage {
        directories: HashMap<String, Vec<Entry>>,
    }

    impl StorageBackend for MemoryStorage {
        fn list(&self, dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>> {
            Box::pin(async move {
                self.directories
                    .get(&dir)
                    .cloned()
                    .ok_or_else(|| StorageBackendError::UrlParseError(dir))
            })
        }

        fn get(
            &self,
            _path: String,
            _byte_offset: u64,
        ) -> BoxFuture<'_, StorageBackendResult<StreamFile>> {
            Box::pin(async {
                Err(StorageBackendError::UrlParseError(
                    "not implemented for scan test".to_string(),
                ))
            })
        }

        fn get_range_response(
            &self,
            _path: String,
            _range: ByteRange,
        ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>> {
            Box::pin(async {
                Ok(RangeResponse {
                    bytes: Bytes::new(),
                    total_size: 1,
                    content_type: None,
                })
            })
        }
    }

    #[tokio::test]
    async fn scans_music_in_bounded_batches_and_can_cancel() {
        let storage = Arc::new(MemoryStorage {
            directories: HashMap::from([
                (
                    "/".to_string(),
                    vec![
                        entry("/Album", true),
                        entry("/root.mp3", false),
                        entry("/readme.txt", false),
                    ],
                ),
                (
                    "/Album".to_string(),
                    vec![
                        entry("/Album/song.flac", false),
                        entry("/Album/cover.jpg", false),
                    ],
                ),
            ]),
        });
        let session = RemoteMusicScanSession::new(StorageId::wrap(7), storage, "/".to_string());

        let first = session.next_batch(1).await.unwrap();
        assert_eq!(first.entries.len(), 1);
        assert_eq!(first.entries[0].path, "/root.mp3");
        assert!(!first.done);

        let second = session.next_batch(1).await.unwrap();
        assert_eq!(second.entries.len(), 1);
        assert_eq!(second.entries[0].path, "/Album/song.flac");
        assert!(!second.done);

        session.cancel();
        let cancelled = session.next_batch(1).await.unwrap();
        assert!(cancelled.entries.is_empty());
        assert!(cancelled.done);
        assert!(cancelled.cancelled);
    }

    #[tokio::test]
    async fn cancellation_interrupts_an_in_flight_directory_request() {
        let session =
            RemoteMusicScanSession::new(StorageId::wrap(7), Arc::new(SlowStorage), "/".to_string());
        let scanning = {
            let session = session.clone();
            tokio::spawn(async move { session.next_batch(10).await.unwrap() })
        };
        tokio::task::yield_now().await;
        session.cancel();

        let cancelled = tokio::time::timeout(std::time::Duration::from_secs(1), scanning)
            .await
            .expect("scan cancellation should not wait for the remote timeout")
            .unwrap();
        assert!(cancelled.done);
        assert!(cancelled.cancelled);
        assert!(cancelled.entries.is_empty());
    }

    struct SlowStorage;

    impl StorageBackend for SlowStorage {
        fn list(&self, _dir: String) -> BoxFuture<'_, StorageBackendResult<Vec<Entry>>> {
            Box::pin(async {
                std::future::pending::<()>().await;
                Ok(Vec::new())
            })
        }

        fn get(
            &self,
            _path: String,
            _byte_offset: u64,
        ) -> BoxFuture<'_, StorageBackendResult<StreamFile>> {
            Box::pin(async {
                Err(StorageBackendError::UrlParseError(
                    "not implemented for scan test".to_string(),
                ))
            })
        }

        fn get_range_response(
            &self,
            _path: String,
            _range: ByteRange,
        ) -> BoxFuture<'_, StorageBackendResult<RangeResponse>> {
            Box::pin(async {
                Ok(RangeResponse {
                    bytes: Bytes::new(),
                    total_size: 1,
                    content_type: None,
                })
            })
        }
    }

    fn entry(path: &str, is_dir: bool) -> Entry {
        Entry {
            name: path.rsplit('/').next().unwrap_or_default().to_string(),
            path: path.to_string(),
            size: (!is_dir).then_some(100),
            is_dir,
            remote_id: None,
            parent_remote_id: None,
            mime_type: None,
            etag: None,
            ctag: None,
            created_at: None,
            modified_at: None,
        }
    }
}

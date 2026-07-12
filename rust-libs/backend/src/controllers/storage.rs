use std::sync::Arc;

use crate::schema::StorageEntryLoc;
use tidetunes_storage_backend::{BuildOneDriveArg, OneDriveBackend};

use crate::{
    error::{BError, BResult},
    objects::{
        create_onedrive_oauth_session, ListStorageEntryChildrenResp, OneDriveDeltaItem,
        OneDriveDeltaPage, OneDriveDeltaPageResult, OneDriveDeltaRequest, OneDriveDrive,
        OneDriveDriveList, OneDriveOAuthSession, Storage, StorageConnectionTestResult,
    },
    services::{
        build_storage_backend, build_storage_backend_by_arg, storage_entry, RemoteMusicScanSession,
    },
    ArgUpsertStorage, Backend,
};

fn normalize_arg_upsert_storage(mut arg: ArgUpsertStorage) -> ArgUpsertStorage {
    if arg.is_anonymous {
        arg.username = Default::default();
        arg.password = Default::default();
    }
    arg
}

#[uniffi::export]
pub async fn ct_exchange_onedrive_code(
    _cx: Arc<Backend>,
    code: String,
    code_verifier: String,
) -> BResult<String> {
    let refresh_token = OneDriveBackend::request_refresh_token(code, code_verifier).await?;
    Ok(refresh_token)
}

#[uniffi::export]
pub async fn ct_list_onedrive_drives(refresh_token: String) -> BResult<OneDriveDriveList> {
    let backend = OneDriveBackend::new(BuildOneDriveArg {
        code: refresh_token,
        drive_id: None,
    });
    let drives = backend
        .list_drives()
        .await?
        .into_iter()
        .map(|drive| OneDriveDrive {
            id: drive.id,
            name: drive.name,
            drive_type: drive.drive_type,
            owner_name: drive.owner_name,
        })
        .collect();
    Ok(OneDriveDriveList {
        drives,
        refresh_token: backend.current_refresh_token().await,
    })
}

#[uniffi::export]
pub async fn ct_get_onedrive_delta_page(
    cx: Arc<Backend>,
    storage: Storage,
    request: OneDriveDeltaRequest,
) -> BResult<OneDriveDeltaPageResult> {
    let backend = build_storage_backend(cx.get_context(), storage)?;
    let page = backend
        .delta(request.root_remote_id, request.cursor, request.latest_only)
        .await;
    let page = match page {
        Ok(page) => page,
        Err(error) if error.is_delta_resync_required() => {
            return Ok(OneDriveDeltaPageResult::ResyncRequired);
        }
        Err(error) => return Err(error.into()),
    };
    Ok(OneDriveDeltaPageResult::Page(OneDriveDeltaPage {
        items: page
            .items
            .into_iter()
            .map(|item| OneDriveDeltaItem {
                remote_id: item.remote_id,
                parent_remote_id: item.parent_remote_id,
                name: item.name,
                path: item.path,
                size: item.size.map(|size| size as u64),
                is_dir: item.is_dir,
                deleted: item.deleted,
                mime_type: item.mime_type,
                etag: item.etag,
                ctag: item.ctag,
                created_at: item.created_at,
                modified_at: item.modified_at,
            })
            .collect(),
        next_link: page.next_link,
        delta_link: page.delta_link,
        refresh_token: backend.current_refresh_token().await?,
    }))
}

#[uniffi::export]
pub async fn ct_test_storage(
    cx: Arc<Backend>,
    arg: ArgUpsertStorage,
) -> BResult<StorageConnectionTestResult> {
    let arg = normalize_arg_upsert_storage(arg);
    let cx = cx.get_context();
    let backend = build_storage_backend_by_arg(cx, arg)?;
    let res = backend.list("/".to_string()).await;

    match res {
        Ok(_) => Ok(StorageConnectionTestResult::Success),
        Err(e) => {
            tracing::warn!("ct_test_storage, {e:?}");
            if e.is_unauthorized() {
                Ok(StorageConnectionTestResult::Unauthorized)
            } else if e.is_timeout() {
                Ok(StorageConnectionTestResult::Timeout)
            } else {
                Ok(StorageConnectionTestResult::OtherError)
            }
        }
    }
}

#[uniffi::export]
pub async fn ct_list_storage_entry_children(
    cx: Arc<Backend>,
    storage: Storage,
    arg: StorageEntryLoc,
) -> BResult<ListStorageEntryChildrenResp> {
    let backend = build_storage_backend(cx.get_context(), storage)?;
    let storage_id = arg.storage_id;
    let res = backend.list(arg.path).await;

    match res {
        Ok(entries) => {
            let entries = entries
                .into_iter()
                .map(|entry| storage_entry(storage_id, entry))
                .collect();
            Ok(ListStorageEntryChildrenResp::Ok(entries))
        }
        Err(e) => {
            tracing::warn!("ct_list_storage_entry_children, {e:?}");
            if e.is_unauthorized() {
                Ok(ListStorageEntryChildrenResp::AuthenticationFailed)
            } else if e.is_timeout() {
                Ok(ListStorageEntryChildrenResp::Timeout)
            } else {
                Ok(ListStorageEntryChildrenResp::Unknown)
            }
        }
    }
}

#[uniffi::export]
pub async fn ct_scan_storage_music_folder(
    cx: Arc<Backend>,
    storage: Storage,
    arg: StorageEntryLoc,
) -> BResult<ListStorageEntryChildrenResp> {
    let backend = build_storage_backend(cx.get_context(), storage)?;
    let mut files = Vec::new();
    let session = RemoteMusicScanSession::new(arg.storage_id, backend, arg.path);
    loop {
        let batch = match session.next_batch(1_000).await {
            Ok(batch) => batch,
            Err(BError::RemoteStorageError(error)) => {
                tracing::warn!("ct_scan_storage_music_folder, {error:?}");
                return if error.is_unauthorized() {
                    Ok(ListStorageEntryChildrenResp::AuthenticationFailed)
                } else if error.is_timeout() {
                    Ok(ListStorageEntryChildrenResp::Timeout)
                } else {
                    Ok(ListStorageEntryChildrenResp::Unknown)
                };
            }
            Err(error) => {
                tracing::warn!("ct_scan_storage_music_folder, {error:?}");
                return Ok(ListStorageEntryChildrenResp::Unknown);
            }
        };
        files.extend(batch.entries);
        if batch.done {
            break;
        }
    }

    Ok(ListStorageEntryChildrenResp::Ok(files))
}

#[uniffi::export]
pub fn ct_start_storage_music_scan(
    cx: Arc<Backend>,
    storage: Storage,
    arg: StorageEntryLoc,
) -> BResult<Arc<RemoteMusicScanSession>> {
    let backend = build_storage_backend(cx.get_context(), storage)?;
    Ok(RemoteMusicScanSession::new(
        arg.storage_id,
        backend,
        arg.path,
    ))
}

#[uniffi::export]
pub fn ct_start_onedrive_oauth() -> OneDriveOAuthSession {
    create_onedrive_oauth_session()
}

#[cfg(test)]
mod tests {
    use crate::services::is_supported_music_path;

    #[test]
    fn detects_supported_music_extensions_case_insensitively() {
        assert!(is_supported_music_path("/Music/Track.FLAC"));
        assert!(is_supported_music_path("/Music/Track.opus"));
        assert!(!is_supported_music_path("/Music/cover.jpg"));
    }
}

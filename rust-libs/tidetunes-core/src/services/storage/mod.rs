use std::{sync::Arc, time::Duration};

use crate::{
    ctx::BackendContext,
    error::BResult,
    objects::{ArgUpsertStorage, Storage},
    schema::{StorageEntryLoc, StorageType},
};
use tidetunes_remote_storage::{
    BuildOneDriveArg, BuildWebdavArg, LocalBackend, OneDriveBackend, StorageBackend, StreamFile,
    Webdav,
};

pub fn build_storage_backend_by_arg(
    _cx: &BackendContext,
    arg: ArgUpsertStorage,
) -> BResult<Arc<dyn StorageBackend + Send + Sync>> {
    let connect_timeout = Duration::from_secs(5);

    let ret: Arc<dyn StorageBackend + Send + Sync + 'static> = match arg.typ {
        StorageType::Local => Arc::new(LocalBackend::new()),
        StorageType::Webdav => {
            let arg = BuildWebdavArg {
                addr: arg.addr,
                username: arg.username,
                password: arg.password,
                is_anonymous: arg.is_anonymous,
                connect_timeout,
            };
            Arc::new(Webdav::new(arg))
        }
        StorageType::OneDrive => {
            let arg = BuildOneDriveArg {
                code: arg.password,
                drive_id: (!arg.addr.is_empty()).then_some(arg.addr),
            };
            Arc::new(OneDriveBackend::new(arg))
        }
    };
    Ok(ret)
}

pub fn build_storage_backend(
    cx: &BackendContext,
    storage: Storage,
) -> BResult<Arc<dyn StorageBackend + Send + Sync>> {
    build_storage_backend_by_arg(
        cx,
        ArgUpsertStorage {
            id: Some(storage.id),
            addr: storage.addr,
            alias: storage.alias,
            username: storage.username,
            password: storage.password,
            is_anonymous: storage.is_anonymous,
            typ: storage.typ,
        },
    )
}

async fn get_asset_file_by_loc(
    cx: &BackendContext,
    storage: Storage,
    entry: StorageEntryLoc,
    byte_offset: u64,
) -> BResult<Option<StreamFile>> {
    let storage_backend = build_storage_backend(cx, storage)?;

    let file = storage_backend.get(entry.path, byte_offset).await;
    if let Err(e) = &file {
        if e.is_not_found() {
            return Ok(None);
        }
    }
    let file = file?;
    Ok(Some(file))
}

pub(crate) async fn get_asset_file(
    cx: &BackendContext,
    storage: Storage,
    entry: StorageEntryLoc,
    byte_offset: u64,
) -> BResult<Option<StreamFile>> {
    get_asset_file_by_loc(cx, storage, entry, byte_offset).await
}

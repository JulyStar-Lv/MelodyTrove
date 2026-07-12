use std::sync::Arc;

use crate::{
    error::BResult,
    objects::Storage,
    schema::StorageEntryLoc,
    services::{build_storage_backend, start_playback_gateway, PlaybackSession},
    Backend,
};

#[uniffi::export]
pub async fn ct_create_playback_session(
    backend: Arc<Backend>,
    storage: Storage,
    loc: StorageEntryLoc,
) -> BResult<Arc<PlaybackSession>> {
    let context = backend.get_context();
    let storage_backend = build_storage_backend(context, storage)?;
    start_playback_gateway(storage_backend, loc.path).await
}

use crate::schema::{MusicId, PlaylistId};
use tidetunes_order_key::OrderKeyError;

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum BError {
    #[error("remote storage error: {0:?}")]
    RemoteStorageError(#[from] tidetunes_storage_backend::StorageBackendError),
    #[error("metadata error: {0}")]
    MetadataError(#[from] tidetunes_audio_metadata::MetadataError),
    #[error("failed to load asset: {0:?}")]
    AssetLoadFail(String),
    #[error("asset not found")]
    AssetNotFound,
    #[error("playlist not found")]
    PlaylistNotFound(PlaylistId),
    #[error("music not found")]
    MusicNotFound(MusicId),
    #[error("io error: {0:?}")]
    IoError(#[from] std::io::Error),
    #[error(transparent)]
    OrderKeyError(#[from] OrderKeyError),
    #[error("custom: {message}")]
    CustomError { message: String },
    #[error(transparent)]
    AnyHowError(#[from] anyhow::Error),
}

pub type BResult<T> = Result<T, BError>;

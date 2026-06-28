mod v2;

pub use v2::{
    BlobId, MusicDuration, MusicId, PlayMode, PlaylistId, StorageEntryLoc, StorageId, StorageType,
};

#[derive(Debug, Clone, Hash, PartialEq, Eq, uniffi::Enum)]
pub enum DataSourceKey {
    Music { id: MusicId },
    Cover { id: MusicId },
    AnyEntry { entry: StorageEntryLoc },
}

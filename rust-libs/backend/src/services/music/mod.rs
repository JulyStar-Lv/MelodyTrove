use std::time::Duration;

use crate::{
    objects::StorageEntry,
    schema::{MusicId, PlaylistId, StorageEntryLoc},
};

#[derive(Debug, uniffi::Record)]
pub struct ArgUpdatePlaylist {
    pub id: PlaylistId,
    pub title: String,
    pub cover: Option<StorageEntryLoc>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct ToAddMusicEntry {
    pub entry: StorageEntry,
    pub name: String,
}

#[derive(Debug, uniffi::Record)]
pub struct ArgCreatePlaylist {
    pub title: String,
    pub cover: Option<StorageEntryLoc>,
    pub entries: Vec<ToAddMusicEntry>,
}

#[derive(Debug, uniffi::Record)]
pub struct ArgAddMusicsToPlaylist {
    pub id: PlaylistId,
    pub entries: Vec<ToAddMusicEntry>,
}

#[derive(Debug, uniffi::Record)]
pub struct ArgRemoveMusicFromPlaylist {
    pub playlist_id: PlaylistId,
    pub music_id: MusicId,
}

#[derive(Debug, uniffi::Record)]
pub struct ArgUpdateMusicLyric {
    pub id: MusicId,
    pub lyric_loc: Option<StorageEntryLoc>,
}

#[derive(uniffi::Record)]
pub struct ArgUpdateMusicDuration {
    pub id: MusicId,
    pub duration: Duration,
}

#[derive(uniffi::Record)]
pub struct ArgUpdateMusicCover {
    pub id: MusicId,
    pub cover: Vec<u8>,
}

use crate::schema::{MusicId, PlaylistId, StorageEntryLoc, StorageId, StorageType};
use base64::{engine::general_purpose::URL_SAFE_NO_PAD, Engine};
use rand::RngCore;
use serde::Serialize;
use sha2::{Digest, Sha256};

use crate::objects::MUSICAPP_ONEDRIVE_ID;

const ONEDRIVE_AUTHORIZE_URL: &str =
    "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
const ONEDRIVE_REDIRECT_URI: &str = "melodytrove://oauth2redirect/";

#[derive(Debug, Clone, uniffi::Record)]
pub struct StorageEntry {
    pub storage_id: StorageId,
    pub name: String,
    pub path: String,
    pub size: Option<u64>,
    pub is_dir: bool,
    pub remote_id: Option<String>,
    pub parent_remote_id: Option<String>,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub ctag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct RemoteMusicScanBatch {
    pub entries: Vec<StorageEntry>,
    pub done: bool,
    pub cancelled: bool,
    pub directory_request_count: u64,
    pub listed_directory_count: u64,
    pub visited_entry_count: u64,
    pub directory_concurrency: u32,
}

#[derive(Debug, Clone, PartialEq, Eq, Default, uniffi::Record)]
pub struct ArgUpsertStorage {
    pub id: Option<StorageId>,
    pub addr: String,
    pub alias: String,
    pub username: String,
    pub password: String,
    pub is_anonymous: bool,
    pub typ: StorageType,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct StorageCredential {
    pub username: String,
    pub secret: String,
    pub is_anonymous: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct OneDriveOAuthSession {
    pub authorization_url: String,
    pub code_verifier: String,
    pub state: String,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct OneDriveDrive {
    pub id: String,
    pub name: String,
    pub drive_type: Option<String>,
    pub owner_name: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct OneDriveDriveList {
    pub drives: Vec<OneDriveDrive>,
    pub refresh_token: String,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct OneDriveDeltaRequest {
    pub storage_id: StorageId,
    pub root_remote_id: String,
    pub cursor: Option<String>,
    pub latest_only: bool,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct OneDriveDeltaItem {
    pub remote_id: String,
    pub parent_remote_id: Option<String>,
    pub name: Option<String>,
    pub path: Option<String>,
    pub size: Option<u64>,
    pub is_dir: bool,
    pub deleted: bool,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub ctag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct OneDriveDeltaPage {
    pub items: Vec<OneDriveDeltaItem>,
    pub next_link: Option<String>,
    pub delta_link: Option<String>,
    pub refresh_token: Option<String>,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum OneDriveDeltaPageResult {
    Page(OneDriveDeltaPage),
    ResyncRequired,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct WebDavSyncRequest {
    pub storage_id: StorageId,
    pub root_path: String,
    pub sync_token: Option<String>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct WebDavSyncItem {
    pub path: String,
    pub name: Option<String>,
    pub size: Option<u64>,
    pub is_dir: bool,
    pub deleted: bool,
    pub mime_type: Option<String>,
    pub etag: Option<String>,
    pub created_at: Option<i64>,
    pub modified_at: Option<i64>,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct WebDavSyncPage {
    pub items: Vec<WebDavSyncItem>,
    pub sync_token: String,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum WebDavSyncPageResult {
    Page(WebDavSyncPage),
    Unsupported,
    ResyncRequired,
}

#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct LegacyStorageCredential {
    pub id: StorageId,
    pub credential: StorageCredential,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, uniffi::Enum)]
pub enum StorageConnectionTestResult {
    #[default]
    None,
    Testing,
    Success,
    Unauthorized,
    Timeout,
    PermissionDenied,
    NotFound,
    InvalidAddress,
    Unavailable,
    Unsupported,
    OtherError,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq, uniffi::Enum)]
pub enum StorageEntryType {
    Folder,
    Music,
    Image,
    Lyric,
    Other,
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct Storage {
    pub id: StorageId,
    pub addr: String,
    pub alias: String,
    pub username: String,
    pub password: String,
    pub is_anonymous: bool,
    pub typ: StorageType,
    pub music_count: u64,
}

#[derive(Debug, Default, Clone, Copy, Serialize, PartialEq, Eq, uniffi::Enum)]
pub enum CurrentStorageImportType {
    #[default]
    None,
    ImportMusics {
        id: PlaylistId,
    },
    EditPlaylistCover,
    CreatePlaylistEntries,
    CreatePlaylistCover,
    CurrentMusicLyrics {
        id: MusicId,
    },
}

#[derive(Debug, Default, Clone, PartialEq, Eq, Serialize, uniffi::Enum)]
pub enum CurrentStorageStateType {
    #[default]
    Loading,
    OK,
    NeedPermission,
    AuthenticationFailed,
    Timeout,
    UnknownError,
}

impl StorageEntry {
    pub fn loc(&self) -> StorageEntryLoc {
        StorageEntryLoc {
            path: self.path.clone(),
            storage_id: self.storage_id,
        }
    }
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum ListStorageEntryChildrenResp {
    Ok(Vec<StorageEntry>),
    AuthenticationFailed,
    Timeout,
    PermissionDenied,
    NotFound,
    InvalidAddress,
    Unavailable,
    Unsupported,
    Unknown,
}

impl ListStorageEntryChildrenResp {
    pub fn is_error(&self) -> bool {
        match self {
            ListStorageEntryChildrenResp::Ok(_) => false,
            ListStorageEntryChildrenResp::AuthenticationFailed
            | ListStorageEntryChildrenResp::Timeout
            | ListStorageEntryChildrenResp::PermissionDenied
            | ListStorageEntryChildrenResp::NotFound
            | ListStorageEntryChildrenResp::InvalidAddress
            | ListStorageEntryChildrenResp::Unavailable
            | ListStorageEntryChildrenResp::Unsupported
            | ListStorageEntryChildrenResp::Unknown => true,
        }
    }
}

pub fn create_onedrive_oauth_session() -> OneDriveOAuthSession {
    let mut verifier_bytes = [0_u8; 32];
    let mut state_bytes = [0_u8; 32];
    rand::thread_rng().fill_bytes(&mut verifier_bytes);
    rand::thread_rng().fill_bytes(&mut state_bytes);

    build_onedrive_oauth_session(
        URL_SAFE_NO_PAD.encode(verifier_bytes),
        URL_SAFE_NO_PAD.encode(state_bytes),
    )
}

fn build_onedrive_oauth_session(code_verifier: String, state: String) -> OneDriveOAuthSession {
    let code_challenge = URL_SAFE_NO_PAD.encode(Sha256::digest(code_verifier.as_bytes()));
    let client_id: &str = MUSICAPP_ONEDRIVE_ID;
    let scope = urlencoding::encode("Files.Read offline_access").to_string();
    let redirect_uri = urlencoding::encode(ONEDRIVE_REDIRECT_URI);

    OneDriveOAuthSession {
        authorization_url: format!(
            "{ONEDRIVE_AUTHORIZE_URL}?client_id={client_id}&response_type=code&redirect_uri={redirect_uri}&scope={scope}&state={state}&code_challenge={code_challenge}&code_challenge_method=S256"
        ),
        code_verifier,
        state,
    }
}

#[cfg(test)]
mod tests {
    use super::{build_onedrive_oauth_session, create_onedrive_oauth_session};

    #[test]
    fn creates_random_pkce_session() {
        let first = create_onedrive_oauth_session();
        let second = create_onedrive_oauth_session();

        assert_eq!(first.code_verifier.len(), 43);
        assert_eq!(first.state.len(), 43);
        assert_ne!(first.code_verifier, second.code_verifier);
        assert_ne!(first.state, second.state);
        assert!(first
            .authorization_url
            .contains("code_challenge_method=S256"));
        assert!(first
            .authorization_url
            .contains(&format!("state={}", first.state)));
    }

    #[test]
    fn builds_expected_s256_challenge() {
        let session = build_onedrive_oauth_session(
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk".to_string(),
            "state-value".to_string(),
        );

        assert!(session
            .authorization_url
            .contains("code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"));
        assert!(session.authorization_url.contains("state=state-value"));
        assert!(session
            .authorization_url
            .contains("redirect_uri=melodytrove%3A%2F%2Foauth2redirect%2F"));
    }
}

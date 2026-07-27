use std::{env, fs, path::PathBuf, sync::Arc, time::Duration};

use tidetunes_storage_backend::{
    BuildSmbArg, ByteRange, SmbBackend, StorageBackend, StorageBackendError,
};

const AUTH_URL: &str = "TIDETUNES_SMB_TEST_AUTH_URL";
const GUEST_URL: &str = "TIDETUNES_SMB_TEST_GUEST_URL";
const USERNAME: &str = "TIDETUNES_SMB_TEST_USERNAME";
const PASSWORD: &str = "TIDETUNES_SMB_TEST_PASSWORD";
const FIXTURE_DIR: &str = "TIDETUNES_SMB_TEST_FIXTURE_DIR";

fn required_env(name: &str) -> String {
    env::var(name)
        .unwrap_or_else(|_| panic!("{name} is required for ignored SMB integration tests"))
}

fn backend(address: String, is_guest: bool) -> SmbBackend {
    let (username, password) = if is_guest {
        (String::new(), String::new())
    } else {
        (required_env(USERNAME), required_env(PASSWORD))
    };
    SmbBackend::new(
        BuildSmbArg::from_url(
            &address,
            username,
            password,
            is_guest,
            Duration::from_secs(5),
        )
        .unwrap(),
    )
    .unwrap()
}

#[tokio::test]
#[ignore = "requires the Samba fixture described in docs/music-sources/smb.md"]
async fn authenticated_share_supports_listing_unicode_streams_and_ranges() {
    let backend = Arc::new(backend(required_env(AUTH_URL), false));
    let root = backend.list("/".to_string()).await.unwrap();
    assert!(root.iter().any(|entry| entry.name == "range.bin"));
    assert!(root
        .iter()
        .any(|entry| entry.name == "音乐" && entry.is_dir));

    let unicode = backend.list("/音乐".to_string()).await.unwrap();
    assert!(unicode.iter().any(|entry| entry.name == "大海.flac"));

    let large = backend
        .get("/large.flac".to_string(), 0)
        .await
        .unwrap()
        .bytes()
        .await
        .unwrap();
    assert_eq!(large.len(), 4 * 1024 * 1024);

    let full = backend
        .get("/range.bin".to_string(), 0)
        .await
        .unwrap()
        .bytes()
        .await
        .unwrap();
    assert_eq!(full.as_ref(), b"0123456789");

    let resumed = backend
        .get("/range.bin".to_string(), 4)
        .await
        .unwrap()
        .bytes()
        .await
        .unwrap();
    assert_eq!(resumed.as_ref(), b"456789");

    let range = backend
        .get_range("/range.bin".to_string(), ByteRange::new(2, 5).unwrap())
        .await
        .unwrap();
    assert_eq!(range.as_ref(), b"2345");

    let concurrent = (0..8).map(|index| {
        let backend = backend.clone();
        tokio::spawn(async move {
            backend
                .get_range(
                    "/range.bin".to_string(),
                    ByteRange::new(index, index).unwrap(),
                )
                .await
                .unwrap()[0]
        })
    });
    let bytes = futures_util::future::join_all(concurrent)
        .await
        .into_iter()
        .map(Result::unwrap)
        .collect::<Vec<_>>();
    assert_eq!(bytes, b"01234567");

    let missing = backend
        .get_range("/missing.flac".to_string(), ByteRange::new(0, 0).unwrap())
        .await;
    assert!(matches!(missing, Err(StorageBackendError::NotFound)));

    backend.release("/range.bin".to_string()).await.unwrap();
}

#[tokio::test]
#[ignore = "requires the Samba fixture described in docs/music-sources/smb.md"]
async fn guest_share_can_be_browsed_without_credentials() {
    let backend = backend(required_env(GUEST_URL), true);
    let entries = backend.list("/".to_string()).await.unwrap();
    assert!(entries.iter().any(|entry| entry.name == "range.bin"));
}

#[tokio::test]
#[ignore = "requires the Samba fixture described in docs/music-sources/smb.md"]
async fn permission_denied_is_reported_without_dropping_the_directory_result() {
    let backend = backend(required_env(AUTH_URL), false);
    let result = backend.list("/restricted".to_string()).await;
    assert!(matches!(result, Err(StorageBackendError::PermissionDenied)));
}

#[tokio::test]
#[ignore = "requires the writable local Samba fixture described in docs/music-sources/smb.md"]
async fn changed_file_size_invalidates_the_cached_reader() {
    let fixture_file = PathBuf::from(required_env(FIXTURE_DIR)).join("mutable.bin");
    fs::write(&fixture_file, b"0123456789").unwrap();
    let backend = backend(required_env(AUTH_URL), false);

    let initial = backend
        .get_range("/mutable.bin".to_string(), ByteRange::new(0, 0).unwrap())
        .await
        .unwrap();
    assert_eq!(initial.as_ref(), b"0");

    fs::write(&fixture_file, b"0123456789a").unwrap();
    let grown = backend
        .get_range("/mutable.bin".to_string(), ByteRange::new(10, 10).unwrap())
        .await
        .unwrap();
    assert_eq!(grown.as_ref(), b"a");

    fs::write(&fixture_file, b"01234").unwrap();
    let shrunk = backend
        .get_range("/mutable.bin".to_string(), ByteRange::new(4, 4).unwrap())
        .await
        .unwrap();
    assert_eq!(shrunk.as_ref(), b"4");
}

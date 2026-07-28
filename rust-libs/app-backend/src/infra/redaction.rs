use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    sync::RwLock,
};

use once_cell::sync::Lazy;
use regex::{Captures, Regex};
use serde::{Deserialize, Serialize};

use super::file_ops::{atomic_write_json, read_json};

const REDACTION_VERSION: &str = "1";
const REDACTED: &str = "***";
static MUSIC_ROOTS: Lazy<RwLock<Vec<PathBuf>>> = Lazy::new(|| RwLock::new(Vec::new()));

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
struct PersistedMusicRoots {
    schema_version: i32,
    roots: Vec<String>,
}

static URL_CREDENTIALS: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?i)\b(https?://)[^/@\s:]+:[^/@\s]+@").expect("valid credential regex")
});
static SENSITIVE_ASSIGNMENT: Lazy<Regex> = Lazy::new(|| {
    Regex::new(
        r"(?i)\b(token|access_token|refresh_token|password|passwd|secret|api_key|apikey|code)\s*=\s*([^&\s]+)",
    )
    .expect("valid assignment regex")
});
static AUTH_HEADER: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?i)\b(authorization\s*:?\s*(?:bearer|basic)?|bearer|basic)\s+[^,\s]+")
        .expect("valid auth regex")
});
static COOKIE_HEADER: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?i)\b(set-cookie|cookie)\s*:\s*[^\r\n]+").expect("valid cookie regex")
});
static URL_QUERY: Lazy<Regex> =
    Lazy::new(|| Regex::new(r"(?i)(https?://[^\s?#]+)\?[^\s#]+").expect("valid URL regex"));
static UUID: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b")
        .expect("valid UUID regex")
});
static ADDRESS: Lazy<Regex> =
    Lazy::new(|| Regex::new(r"(?i)\b0x[0-9a-f]+\b").expect("valid address regex"));
static LINE_NUMBER: Lazy<Regex> =
    Lazy::new(|| Regex::new(r"(?i)(:\d+|\bline\s+\d+\b)").expect("valid line regex"));

pub(crate) fn redaction_version() -> &'static str {
    REDACTION_VERSION
}

pub(crate) fn set_music_roots(roots: Vec<String>) {
    let normalized = roots
        .into_iter()
        .filter(|root| root.trim().len() > 3)
        .map(PathBuf::from)
        .collect();
    if let Ok(mut current) = MUSIC_ROOTS.write() {
        *current = normalized;
    }
}

pub(crate) fn persist_music_roots(path: &Path, roots: Vec<String>) -> std::io::Result<()> {
    let normalized = roots
        .into_iter()
        .filter(|root| root.trim().len() > 3)
        .collect::<Vec<_>>();
    atomic_write_json(
        path,
        &PersistedMusicRoots {
            schema_version: 1,
            roots: normalized.clone(),
        },
    )?;
    set_music_roots(normalized);
    Ok(())
}

pub(crate) fn load_music_roots(path: &Path) -> std::io::Result<()> {
    let state: PersistedMusicRoots = read_json(path)?;
    set_music_roots(state.roots);
    Ok(())
}

pub(crate) fn redact_text(
    value: &str,
    app_document_dir: Option<&Path>,
    app_cache_dir: Option<&Path>,
) -> String {
    let mut redacted = URL_CREDENTIALS
        .replace_all(value, "$1***:***@")
        .into_owned();
    redacted = SENSITIVE_ASSIGNMENT
        .replace_all(&redacted, |captures: &Captures<'_>| {
            format!("{}={REDACTED}", &captures[1])
        })
        .into_owned();
    redacted = AUTH_HEADER
        .replace_all(&redacted, |captures: &Captures<'_>| {
            format!("{} {REDACTED}", &captures[1])
        })
        .into_owned();
    redacted = COOKIE_HEADER
        .replace_all(&redacted, |captures: &Captures<'_>| {
            format!("{}: {REDACTED}", &captures[1])
        })
        .into_owned();
    redacted = URL_QUERY
        .replace_all(&redacted, "$1?<REDACTED_QUERY>")
        .into_owned();

    if let Some(path) = app_document_dir.and_then(Path::to_str) {
        if !path.is_empty() {
            redacted = redacted.replace(path, "<APP_DOCUMENT_DIR>");
        }
    }
    if let Some(path) = app_cache_dir.and_then(Path::to_str) {
        if !path.is_empty() {
            redacted = redacted.replace(path, "<APP_CACHE_DIR>");
        }
    }
    if let Some(home) = user_home() {
        redacted = redacted.replace(&home, "<HOME>");
    }
    if let Ok(roots) = MUSIC_ROOTS.read() {
        for (index, root) in roots.iter().enumerate() {
            if let Some(path) = root.to_str().filter(|path| path.len() > 3) {
                redacted = redacted.replace(path, &format!("<MUSIC_ROOT_{}>", index + 1));
            }
        }
    }
    redacted
}

pub(crate) fn sanitize_fields(
    fields: &HashMap<String, String>,
    app_document_dir: Option<&Path>,
    app_cache_dir: Option<&Path>,
) -> HashMap<String, String> {
    fields
        .iter()
        .filter(|(key, _)| is_allowed_field_name(key))
        .map(|(key, value)| {
            (
                key.clone(),
                redact_text(value, app_document_dir, app_cache_dir),
            )
        })
        .collect()
}

pub(crate) fn stable_fingerprint_material(value: &str) -> String {
    let redacted = redact_text(value, None, None);
    let redacted = UUID.replace_all(&redacted, "<UUID>");
    let redacted = ADDRESS.replace_all(&redacted, "<ADDRESS>");
    LINE_NUMBER.replace_all(&redacted, "<LINE>").into_owned()
}

fn is_allowed_field_name(name: &str) -> bool {
    let normalized = name
        .chars()
        .filter(|character| character.is_ascii_alphanumeric())
        .flat_map(char::to_lowercase)
        .collect::<String>();
    matches!(
        normalized.as_str(),
        "trackid"
            | "playlistid"
            | "queuesize"
            | "sourcetype"
            | "decoder"
            | "backend"
            | "outputdevice"
            | "dspenabled"
            | "playbackcorrelationid"
            | "scanid"
            | "accountid"
            | "rootidentifier"
            | "scanned"
            | "imported"
            | "failed"
            | "elapsedms"
            | "metadatafetchedbytes"
            | "pluginid"
            | "pluginversion"
            | "pluginfunction"
            | "runtimestage"
            | "timeout"
            | "component"
            | "status"
            | "reason"
            | "pid"
            | "importance"
            | "psskb"
            | "rsskb"
            | "truncated"
            | "bytes"
            | "count"
            | "durationms"
            | "attemptid"
            | "stage"
    )
}

fn user_home() -> Option<String> {
    std::env::var("HOME")
        .ok()
        .or_else(|| std::env::var("USERPROFILE").ok())
        .filter(|value| !value.is_empty())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn redacts_credentials_headers_queries_and_paths() {
        let input = "https://user:pass@example.com/a?access_token=abc Authorization: Bearer xyz Cookie: sid=1\n/Users/test/music";
        let output = redact_text(input, Some(Path::new("/Users/test")), None);
        assert!(!output.contains("pass"));
        assert!(!output.contains("abc"));
        assert!(!output.contains("xyz"));
        assert!(!output.contains("sid=1"));
        assert!(!output.contains("/Users/test"));
        assert!(output.contains("<APP_DOCUMENT_DIR>"));
    }

    #[test]
    fn structured_fields_use_a_denylist_before_writing() {
        let fields = HashMap::from([
            ("trackId".to_string(), "42".to_string()),
            ("authorizationToken".to_string(), "secret".to_string()),
            ("unreviewedField".to_string(), "value".to_string()),
        ]);
        let sanitized = sanitize_fields(&fields, None, None);
        assert_eq!(sanitized.get("trackId").map(String::as_str), Some("42"));
        assert!(!sanitized.contains_key("authorizationToken"));
        assert!(!sanitized.contains_key("unreviewedField"));
    }

    #[test]
    fn fingerprint_removes_unstable_values() {
        let first = stable_fingerprint_material(
            "panic 0x1234 at /tmp/a.rs:42 123e4567-e89b-12d3-a456-426614174000",
        );
        let second = stable_fingerprint_material(
            "panic 0xabcd at /tmp/a.rs:99 550e8400-e29b-41d4-a716-446655440000",
        );
        assert_eq!(first, second);
    }

    #[test]
    fn replaces_known_music_roots() {
        set_music_roots(vec!["/mnt/private-music".to_string()]);
        assert_eq!(
            redact_text("/mnt/private-music/album/song.flac", None, None),
            "<MUSIC_ROOT_1>/album/song.flac",
        );
        set_music_roots(Vec::new());
    }

    #[test]
    fn persisted_music_roots_are_available_before_full_application_startup() {
        let root = super::super::file_ops::temporary_test_directory("redaction-roots");
        let path = root.join("music-roots.json");
        persist_music_roots(&path, vec!["/Volumes/private-music".to_string()]).unwrap();
        set_music_roots(Vec::new());
        load_music_roots(&path).unwrap();

        assert_eq!(
            redact_text("/Volumes/private-music/artist/song.flac", None, None),
            "<MUSIC_ROOT_1>/artist/song.flac",
        );
        std::fs::remove_dir_all(root).unwrap();
        set_music_roots(Vec::new());
    }
}

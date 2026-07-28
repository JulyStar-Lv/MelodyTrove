use std::{
    fs::{self, File, OpenOptions},
    io::{self, Read, Write},
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use rand::{distributions::Alphanumeric, thread_rng, Rng};
use serde::{de::DeserializeOwned, Serialize};

pub(crate) fn now_epoch_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .min(i64::MAX as u128) as i64
}

pub(crate) fn new_id(prefix: &str) -> String {
    let suffix: String = thread_rng()
        .sample_iter(&Alphanumeric)
        .take(12)
        .map(char::from)
        .collect();
    format!("{prefix}-{}-{suffix}", now_epoch_ms())
}

pub(crate) fn create_diagnostics_directories(root: &Path) -> io::Result<()> {
    for relative in [
        "logs/sessions",
        "incidents",
        "startup/history",
        "state",
        "state/corrupt",
        "exports",
    ] {
        fs::create_dir_all(root.join(relative))?;
    }
    Ok(())
}

pub(crate) fn read_json<T: DeserializeOwned>(path: &Path) -> io::Result<T> {
    let bytes = fs::read(path)?;
    serde_json::from_slice(&bytes).map_err(io::Error::other)
}

pub(crate) fn atomic_write_json<T: Serialize>(path: &Path, value: &T) -> io::Result<()> {
    let bytes = serde_json::to_vec_pretty(value).map_err(io::Error::other)?;
    atomic_write(path, &bytes)
}

pub(crate) fn atomic_write(path: &Path, bytes: &[u8]) -> io::Result<()> {
    let parent = path
        .parent()
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, "path has no parent"))?;
    fs::create_dir_all(parent)?;
    let temp_path = temporary_sibling(path);
    let result = (|| {
        let mut file = OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(&temp_path)?;
        file.write_all(bytes)?;
        file.flush()?;
        file.sync_all()?;
        drop(file);
        fs::rename(&temp_path, path)?;
        sync_directory(parent)
    })();
    if result.is_err() {
        let _ = fs::remove_file(&temp_path);
    }
    result
}

pub(crate) fn sync_directory(path: &Path) -> io::Result<()> {
    #[cfg(unix)]
    {
        File::open(path)?.sync_all()
    }
    #[cfg(not(unix))]
    {
        let _ = path;
        Ok(())
    }
}

pub(crate) fn quarantine_corrupt_file(path: &Path, corrupt_dir: &Path) -> io::Result<PathBuf> {
    fs::create_dir_all(corrupt_dir)?;
    let file_name = path
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("diagnostics-state.json");
    let destination = corrupt_dir.join(format!("{file_name}.corrupt-{}", now_epoch_ms()));
    fs::rename(path, &destination)?;
    Ok(destination)
}

pub(crate) fn directory_size(path: &Path) -> i64 {
    if path.is_file() {
        return fs::metadata(path)
            .map(|metadata| metadata.len().min(i64::MAX as u64) as i64)
            .unwrap_or(0);
    }
    let Ok(entries) = fs::read_dir(path) else {
        return 0;
    };
    entries
        .flatten()
        .map(|entry| directory_size(&entry.path()))
        .fold(0_i64, i64::saturating_add)
}

pub(crate) fn copy_with_limit<R: io::Read, W: io::Write>(
    mut reader: R,
    mut writer: W,
    limit: u64,
) -> io::Result<(u64, bool)> {
    let mut buffer = Vec::new();
    (&mut reader)
        .take(limit.saturating_add(1))
        .read_to_end(&mut buffer)?;
    let copied = buffer.len().min(limit as usize);
    writer.write_all(&buffer[..copied])?;
    Ok((copied as u64, buffer.len() as u64 > limit))
}

fn temporary_sibling(path: &Path) -> PathBuf {
    let suffix: String = thread_rng()
        .sample_iter(&Alphanumeric)
        .take(8)
        .map(char::from)
        .collect();
    let name = path
        .file_name()
        .and_then(|name| name.to_str())
        .unwrap_or("diagnostics");
    path.with_file_name(format!(".{name}.{suffix}.tmp"))
}

#[cfg(test)]
pub(crate) fn temporary_test_directory(name: &str) -> PathBuf {
    let directory = std::env::temp_dir().join(new_id(&format!("tidetunes-{name}")));
    fs::create_dir_all(&directory).expect("create test directory");
    directory
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn atomic_json_write_never_leaves_a_temp_file() {
        let directory = temporary_test_directory("atomic-write");
        let path = directory.join("state.json");
        atomic_write_json(&path, &serde_json::json!({"ok": true})).unwrap();

        assert_eq!(
            read_json::<serde_json::Value>(&path).unwrap()["ok"],
            serde_json::Value::Bool(true)
        );
        assert_eq!(fs::read_dir(&directory).unwrap().count(), 1);
        fs::remove_dir_all(directory).unwrap();
    }

    #[test]
    fn bounded_copy_reports_truncation() {
        let mut output = Vec::new();
        let (copied, truncated) = copy_with_limit(&b"abcdef"[..], &mut output, 4).unwrap();
        assert_eq!(copied, 4);
        assert!(truncated);
        assert_eq!(&output[..4], b"abcd");
    }
}

# TideTune Room KMP schema

Date: 2026-06-26

The shared Room database is `tidetune.db`. Android, iOS, and Desktop use
platform-specific builders with bundled SQLite. Schema versions 1, 2, and 3 are
exported under
`shared/schemas/com.github.tidetune.database.TideTuneDatabase/`.

## Ownership

- Room is the UI-facing source of truth.
- Rust owns remote access, bounded range reads, metadata parsing, and scanning.
- KMP repositories translate Rust results into Room transactions.
- Credentials are never stored in Room. `StorageEntity.credentialRef` points to
  Android Keystore, iOS Keychain, or the Desktop operating-system credential
  store.

## Tables

| Table | Purpose | Important constraints |
| --- | --- | --- |
| `storage` | Local, WebDAV, and OneDrive configuration | Indexed by type; unique credential reference |
| `selected_folder` | User-selected import roots and sync state | Unique storage/remote ID and storage/path pairs |
| `remote_file` | Stable remote file inventory | Unique storage/remote ID and storage/path pairs; indexed deletion and scan markers |
| `track` | Normalized audio metadata and properties | One track per remote file; album/title indexes |
| `album`, `artist`, `genre` | Normalized library dimensions | Unique normalized names |
| `track_artist`, `album_artist`, `track_genre` | Ordered many-to-many metadata | Foreign-key cascades |
| `artwork` | Paths and hashes for extracted artwork | Artwork bytes stay outside Room |
| `lyrics` | Embedded or sidecar lyrics | One current lyric row per track |
| `raw_metadata` | Unmapped source tags | Indexed by track and tag key |
| `import_job` | Resumable import progress and errors | Indexed active status and folder |
| `sync_cursor` | OneDrive delta/WebDAV scan checkpoints | One cursor per selected folder |
| `playlist`, `playlist_track` | User playlists and stable ordering | Foreign-key cascades and ordered indexes |

## Room-only persistence

Room is now the only app database for relational library data. Storage
definitions, selected folders, remote-file inventory, tracks, playlists,
playlist order, and lyrics are read and written through Room DAOs.

Lightweight UI preferences that do not need relational queries, such as
`playMode`, live in KMP Preferences DataStore instead of Room.

Rust no longer opens or owns an app database. KMP code passes a Room-derived
`Storage` DTO to Rust when it needs remote directory listing, metadata reads,
asset streaming, OneDrive delta pages, or range playback. Tracks created from
direct playlist imports keep `sourceStorageId` and `sourcePath` so playback and
asset reads can resolve the remote location without a legacy Rust row.

## Remote import coordinator

`RemoteLibraryImportCoordinator` is the KMP write boundary for a complete remote
folder snapshot. Its `scanAndImportFolder` path starts Rust
`RemoteMusicScanSession` and consumes bounded batches while the recursive
`Depth: 1` traversal is still running. It:

- ensures the selected folder exists in `selected_folder`;
- creates the `RUNNING` `import_job` before remote enumeration;
- compares incoming `StorageEntry` values with existing `remote_file` rows using
  size plus ETag, falling back to Last-Modified when the server has no ETag;
- resolves stable `remoteId` matches after path lookup so ordinary OneDrive
  rename/move operations retain the `remote_file` and `track` identities;
- skips unchanged files without re-reading metadata;
- reads metadata for changed files through Rust `MetadataRepository.readBatch`;
- persists normalized album, track/album artist, and genre relationships;
- stores embedded lyrics in `lyrics` and bounded text tags in `raw_metadata`;
- defaults to 100-file batches, bounding SQLite `IN` queries, FFI DTOs, memory,
  and transaction duration;
- commits changed files, tracks, and updated `import_job` counters/checkpoint
  after each batch;
- marks rows missing from the complete snapshot as deleted and advances
  `sync_cursor` only after Rust reports the scan complete;
- leaves existing rows undeleted when a scan is cancelled or interrupted.

`SyncDao.observeRecentJobs()` feeds `ImportStatusRepository`,
`ImportStatusVM`, and the Dashboard. The UI therefore observes persisted import
counts, failures, checkpoint names, completion state, and cancellation state
from Room rather than from an in-memory scanner callback.

`TrackDao.observeAll()` and paged reads hide tracks whose `remote_file` row is
marked deleted, while direct playlist-import tracks with `remoteFileId = null`
remain visible through their `sourceStorageId` and `sourcePath`.

Desktop bundled-SQLite integration tests exercise generated DAO code rather
than mocks. They cover upsert, stable-ID path moves, deletion visibility,
restoration, writer-transaction rollback, and a 50,000-file/50,000-track
batched import followed by paged reads.

## Migration 1 to 2

`MIGRATION_1_2` adds nullable standardized metadata columns to `track` without
destructive migration:

- primary artist, lyricist, conductor, copyright, and publisher;
- original release date, BPM, musical key, and ISRC;
- MusicBrainz recording, track, release, release-group, artist,
  release-artist, and work IDs;
- ReplayGain track/album gain and peak values.

The migration is registered in the shared database builder and is directly
tested against bundled SQLite.

## Migration 2 to 3

`MIGRATION_2_3` adds the Room-only playback fields:

- `track.sourceStorageId`
- `track.sourcePath`
- `playlist.coverStorageId`
- `playlist.coverPath`

The fields are nullable so imported library tracks can continue resolving
through `remote_file`, while direct playlist-import tracks can resolve their
remote location without any Rust database row.

`LibraryRepository` maps `TrackDao.observeAll()` into UI-facing
`LibraryTrackItem` values. `LibraryVM` and `LibrarySubpage` observe that Room
flow, so the library tab does not depend on remote scanner output or generated
FFI calls.

The current UI entry point is the storage edit screen. For an existing storage,
the Library action opens the shared import picker in current-directory mode.
Confirming the current directory calls `scanAndImportFolder`, so the directory
selection flow now reaches Room persistence instead of returning a transient
file list only.

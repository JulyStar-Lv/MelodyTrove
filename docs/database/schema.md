# TideTunes Room KMP Schema

Date: 2026-07-01

The shared Room database is `tidetunes.db`. Android, iOS, and Desktop use
platform-specific builders with bundled SQLite. Schema versions 1 through 7 are
exported under
`shared/schemas/com.github.tidetunes.database.TideTunesDatabase/`.

## Ownership

- Room is the UI-facing source of truth for library, playlist, sync, and
  download state.
- Canonical library tables are source-agnostic. Tracks, albums, artists, genres,
  lyrics, artwork, raw metadata, playlists, and downloads are not owned by any
  provider.
- `RemoteLibraryImportCoordinator` is the write boundary from source scan data
  into canonical Room tables.
- Source adapters authenticate, browse, scan, and resolve playback resources;
  they do not write canonical DAOs directly.
- Rust owns remote access, bounded range reads, metadata parsing, and scanning,
  but it does not open or own an app database.
- Credentials, access tokens, cookies, signed URLs, playback headers, and
  temporary loopback URLs are never persisted in Room.

## Current Tables

| Table | Purpose | Important constraints |
| --- | --- | --- |
| `source_account` | Local, WebDAV, OneDrive, and future provider account metadata | Provider type, display name, endpoint/account hints, credential reference only |
| `library_root` | User-selected import roots and root-level sync state | Unique source-account/provider-root and source-account/path identities |
| `source_item` | Provider inventory item identity and file facts | Unique source-account/provider-item and source-account/path identities; deletion and scan markers |
| `source_item_property` | Extensible provider-specific item attributes | Key/value rows scoped to one source item |
| `track_source_ref` | Relationship between canonical tracks and playable source items | One source item maps to one canonical track; availability/download/preference flags |
| `source_sync_cursor` | Delta or scan checkpoint state | One cursor per source account, library root, and cursor type |
| `source_error` | Persisted source/import errors | Scoped to account, root, and optionally source item |
| `track` | Canonical normalized audio metadata | No provider ownership fields; indexed title, ISRC, MusicBrainz IDs |
| `album`, `artist`, `genre` | Canonical library dimensions | Unique normalized names |
| `track_artist`, `album_artist`, `track_genre` | Ordered many-to-many metadata | Foreign-key cascades |
| `artwork` | Extracted artwork cache metadata | Artwork bytes stay outside Room |
| `lyrics` | Embedded or sidecar lyrics | One current lyric row per track |
| `raw_metadata` | Unmapped source tags | Indexed by track and tag key |
| `import_job` | Resumable import progress and errors | References `library_root` |
| `download_task` | Offline download task state and progress | Unique source/media/remote ID; indexed status and update time |
| `playlist`, `playlist_track` | User playlists and stable ordering | Foreign-key cascades and ordered indexes |

The live schema no longer contains `storage`, `selected_folder`, `remote_file`,
or `sync_cursor`. Those tables are read only by historical migration code.
`TrackEntity` also no longer has `remoteFileId`, `sourceStorageId`, or
`sourcePath`.

## Import Coordinator

`RemoteLibraryImportCoordinator.scanAndImportFolder` consumes Rust
`RemoteMusicScanSession` batches and writes Room transactions:

1. Ensure a `source_account` and `library_root`.
2. Create or update the `import_job`.
3. Compare incoming `StorageEntry` values with `source_item` rows by canonical
   path and stable provider item ID.
4. Skip unchanged source items using size plus ETag, falling back to modified
   time when the source has no ETag.
5. Read metadata only for changed items through Rust metadata APIs.
6. Upsert `source_item`, canonical `track`, normalized album/artist/genre
   relationships, lyrics, artwork metadata, raw tags, and `track_source_ref`.
7. Mark missing source items and their refs unavailable only after a complete
   snapshot finishes.
8. Advance `source_sync_cursor` and final `import_job` state in the same
   bounded persistence path.

Canonical track matching prefers MusicBrainz recording ID, then ISRC plus
duration, then strict title/artist/album/duration metadata. A track can have
multiple source refs across accounts/providers, while each source item points
to one canonical track.

## Playback Resolution

Playback resolves through persisted source references, not provider fields on
`track`:

```text
TrackEntity
  -> TrackSourceRefEntity
  -> SourceItemEntity
  -> MusicSource.resolvePlayback(...)
  -> transient PlaybackResource
```

`PlaybackResourceResolver` orders available refs by downloaded/local/preferred
and audio quality hints. The returned URI, headers, cookies, and expiration
metadata remain transient and are released through the playback resolver.

## Visibility And Deletion

Ordinary library/search queries require an available `track_source_ref`.
When a source item disappears or an account is unavailable, the app marks the
source item/ref unavailable. It does not delete canonical tracks, metadata, or
user playlist data as part of source disappearance.

## Migrations

- `MIGRATION_1_2` adds standardized metadata columns to `track`.
- `MIGRATION_2_3` adds old nullable playback columns used by the previous Room
  schema line.
- `MIGRATION_3_4` adds `download_task`.
- `MIGRATION_4_5` adds playlist cover location columns.
- `MIGRATION_5_6` adds Room FTS4 support for local search.
- `MIGRATION_6_7` creates `source_account`, `library_root`, `source_item`,
  `source_item_property`, `track_source_ref`, `source_sync_cursor`, and
  `source_error`; rebuilds `track` and `import_job`; migrates old storage,
  selected-folder, remote-file, and sync-cursor data; then drops the old tables.

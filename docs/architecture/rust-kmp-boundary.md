# Rust and KMP boundary

Date: 2026-06-25

## Responsibilities

Rust owns:

- WebDAV and OneDrive networking and authentication protocol handling;
- controlled `Depth: 1` traversal and Graph pagination;
- bounded HTTP ranges and response validation;
- range-block caching and metadata read budgets;
- Lofty metadata parsing;
- Desktop decoding/playback when the player crate is implemented.

KMP owns:

- secure credential references and platform credential stores;
- import coordination and Room transactions;
- repositories, `Flow`/`StateFlow`, ViewModels, and Compose UI;
- platform player adapters for Android and iOS.

Compose UI must not import generated UniFFI functions. Generated types are
currently confined to repositories and legacy adapters while domain models are
introduced incrementally.

## Metadata call path

```text
KMP MetadataRepository
  -> ctReadRemoteMetadata
  -> StorageRangeSource
  -> StorageBackend.get_range(start..=end)
  -> RemoteRangeReader (Read + Seek, block cache, budgets)
  -> Lofty Probe
  -> RemoteMetadata
       - normalized people, release, identifiers, ReplayGain, and audio properties
       - embedded lyrics descriptor
       - bounded generic text-tag list
```

Remote range reads use finite `bytes=start-end` requests. A successful remote
response must be `206 Partial Content` with a matching `Content-Range`.
Successful `200 OK` responses are rejected as unsupported range behavior rather
than silently downloading the entire object.

Default metadata limits are 256 KiB blocks, 64 requests, and 4 MiB fetched per
file. A 50-file live WebDAV scan showed that 64 KiB blocks caused request-bound
timeouts on FLAC files with large metadata regions; 256 KiB reduced requests by
63.9% and scan time by 70.1%, while keeping average transfer near 0.50 MiB per
file. These are scanner safeguards, not playback limits.

The initial pass still disables artwork decoding. Text metadata is bounded to
2,048 entries, 256 KiB per value, and 1 MiB total per file. Oversized input
returns an explicit metadata budget error instead of crossing FFI or being
written as an unbounded Room value. Parsed fields now include multi-artist
credits, composer, lyricist, conductor, grouping, comments, copyright,
publisher/label, original release date, BPM, key, ISRC, MusicBrainz IDs,
ReplayGain values, embedded lyrics, channel layout, codec/container, and
lossless classification.

## Import persistence call path

```text
EditStorageVM.prepareImportLibraryFolder
  -> ImportRepository current-directory mode
  -> ImportVM directory picker
  -> RemoteScannerRepository.listDirectory
  -> ctListStorageEntryChildren
  -> user confirms the current directory
  -> RemoteLibraryImportCoordinator.scanAndImportFolder
  -> ctStartStorageMusicScan
  -> RemoteMusicScanSession.nextBatch(default 100 files)
  -> StorageBackend.list with controlled Depth: 1 recursion
  -> for each bounded batch:
       - match by canonical path, then stable remoteId when available
       - compare size + ETag or Last-Modified fingerprints
       - MetadataRepository.readBatch(concurrency = 2..4 typical, 4 default)
       - Room transaction
            - upsert changed source_item rows
            - mark unchanged source items as seen
            - upsert album, artist, genre, and relationship rows
            - upsert normalized track metadata
            - upsert track_source_ref rows
            - replace embedded lyrics and bounded raw text tags
            - persist import_job counters and checkpoint
  -> after the Rust session reports done:
       - mark source items not seen by this scan as deleted/unavailable
       - persist source_sync_cursor and final import_job status
  -> SyncDao.observeRecentJobs
  -> ImportStatusRepository / ImportStatusVM
  -> Dashboard import status and cancellation action
  -> TrackDao.observeAll
  -> LibraryRepository
  -> LibraryVM
  -> LibrarySubpage
```

The scanner uses repeated single-level remote listings rather than unbounded
Depth: infinity WebDAV requests. Rust retains only its directory queue, the
current directory response, and one bounded file batch. KMP defaults to
100-file batches, keeping SQLite `IN` arguments, the metadata FFI request,
metadata results, and each Room transaction bounded for 1,000- to 100,000-file
libraries.

`import_job` is created before remote enumeration begins and is updated after
every committed batch. Cancelling the Rust scan session preserves already
committed rows, records `CANCELLED`, and deliberately does not run the
missing-file deletion step. The Rust session races each in-flight directory
request against a cancellation signal, so cancellation drops the active
listing future instead of waiting for the remote timeout. Missing remote rows
are marked deleted only after the scanner reports a complete snapshot.

For OneDrive and other backends with stable item IDs, a rename or move reuses
the existing `source_item` primary key. If size and ETag/Last-Modified are
unchanged, the coordinator updates only the source inventory and skips metadata
Range reads. If the item revision also changed, metadata is refreshed while the
existing canonical `track` primary key and creation timestamp are retained.

The coordinator also accepts an already-built complete snapshot for tests and
future delta implementations; that path uses the same bounded batch writer. The
directory picker browses one level at a time through `RemoteScannerRepository`;
the Compose page and `ImportVM` do not import generated UniFFI functions. The
picker is not the source of truth for the imported library: Room rows written by
the coordinator are. The Home pager includes a Library tab backed by
`TrackDao.observeAll()`, so imported songs remain visible after the remote scan
objects have been released.

## iOS playback call path

```text
IosPlayerController
  -> ctCreatePlaybackSession
  -> Rust loopback gateway on 127.0.0.1
  -> AVPlayer HTTP Range request
  -> 256 KiB in-memory LRU block cache
  -> StorageBackend.get_range_response(start..=end)
  -> finite WebDAV or OneDrive Range request
```

The loopback URL contains a random per-session token and a media extension for
AVFoundation format detection. The gateway supports one HTTP byte range per
request, returns `206 Partial Content` or `416 Range Not Satisfiable`, and never
falls back to a whole-file disk cache. The KMP controller owns the Rust
`PlaybackSession`; replacing or stopping playback shuts down the gateway.

Gateway setup and serving run inside TideTunes's Rust Tokio runtime. This is
required because UniFFI async functions may be polled from platform coroutine
threads that do not have a Tokio reactor.

## Credential call path

Room persists only `credentialRef` and non-secret storage display fields.
`StorageRepository` loads the platform credential and registers it in the
in-memory Rust backend state. Rust clears migrated legacy secrets and never
returns passwords or refresh tokens in storage-list responses.

Platform stores:

- Android: AES-GCM key in Android Keystore; encrypted payload in private
  preferences.
- iOS: generic-password item in Keychain.
- Desktop: macOS Keychain or Linux Secret Service command adapter. Windows
  Credential Manager remains to be implemented.

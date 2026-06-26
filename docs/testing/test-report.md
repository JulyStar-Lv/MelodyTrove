# TideTune test report

Last updated: 2026-06-26

This report tracks verified migration gates. Secrets used for live WebDAV
checks were provided at runtime and are not stored in this repository.

## Current verified commands

| Area | Command | Result |
| --- | --- | --- |
| Room/DataStore persistence | `./gradlew :shared:desktopTest` | Passed; 29 tests including Room v2->v3 migration, DataStore play-mode persistence, RoomLibraryStore playlist/location writes, lyric removal, duration update, generated DAO integration, and 50,000-track paging |
| Android shared compile | `./gradlew :shared:compileDebugKotlinAndroid` | Passed |
| iOS shared compile | `./gradlew :shared:compileKotlinIosSimulatorArm64` | Passed |
| Rust core tests | `cargo test -p tidetune-core` | Passed; 7 tests |
| Rust core compile | `cargo check -p tidetune-core` | Passed |
| redb code scan | `rg -n "redb|DatabaseServer|database_server|tidetune-legacy|LegacyLibraryMirror|ctUpsertStorage|ctRemoveStorage|ctListStorage\\(" shared/src rust-libs/tidetune-core rust-libs/Cargo.toml rust-libs/Cargo.lock -g '!**/build/**' -g '!**/target/**'` | No matches |
| Rust formatting | `cargo fmt --manifest-path rust-libs/Cargo.toml --all -- --check` | Passed |
| Rust clippy | `cargo clippy --manifest-path rust-libs/Cargo.toml --workspace --all-targets -- -D warnings` | Passed |
| Rust tests | `cargo test --manifest-path rust-libs/Cargo.toml --workspace` | Passed; 49 tests |
| KMP Desktop/shared | `./gradlew :shared:desktopTest` | Passed; 29 tests including DataStore play-mode persistence, Room v1->v2/v2->v3 migrations, lyrics/raw-tag replacement, generated DAO integration, 50,000-track performance, batching, rename/move, delta deletion/cursor persistence, rollback, import status, and library mapping |
| Android app | `./gradlew :androidApp:assembleDebug` | Passed with streamed import, generated Room DAO changes, and cancellation UI |
| iOS shared compile | `./gradlew :shared:compileKotlinIosSimulatorArm64` | Passed with streamed import, generated Room DAO changes, and cancellation UI |
| iOS Simulator app | XcodeBuildMCP `build_run_sim` for the `TideTune` scheme on iPhone 13 Pro / iOS 17.2 | Passed; app installed and launched with bundle ID `com.github.tidetune` |
| iOS OAuth redirect | `simctl openurl` with an invalid test state, followed by the system Open confirmation | Passed; TideTune resumed without accepting credentials or crashing |
| Diff hygiene | `git diff --check` | Passed |
| Secret scan | Repository-wide ripgrep for the live WebDAV password and credential-bearing URL patterns | No matches |

## Live WebDAV evidence

See [webdav-live-test-2026-06-25.md](webdav-live-test-2026-06-25.md) and
[webdav-metadata-scan-50-2026-06-25.md](webdav-metadata-scan-50-2026-06-25.md).

Current optimized metadata scan:

| Metric | Result |
| --- | ---: |
| Files selected | 50 |
| Full metadata success | 50 |
| Partial/failure/timeout | 0 |
| Metadata concurrency | 4 |
| Metadata scan time | 20.183 s |
| Range requests | 75 |
| Range bytes | 19,660,800 |

Incremental scan using size plus ETag/Last-Modified fingerprints:

| Metric | Result |
| --- | ---: |
| Files selected | 50 |
| Files skipped unchanged | 50 |
| Metadata files parsed | 0 |
| Range requests | 0 |
| Range bytes | 0 |
| Total elapsed | 0.824 s |

## Room large-library evidence

`RoomLibraryIntegrationTest` uses Room's generated Desktop DAO
implementations with bundled SQLite and an in-memory database.

| Metric | Result |
| --- | ---: |
| `remote_file` rows inserted | 50,000 |
| `track` rows inserted | 50,000 |
| Transaction batch size | 500 |
| Insert elapsed | 496 ms |
| Final page size | 200 |
| DAO/transaction failures | 0 |

The same test class verifies transaction rollback and the lifecycle
`upsert -> stable-ID move -> mark deleted -> restore`, including the
`TrackDao` visibility rules. It also verifies the v1-to-v2 metadata migration
and v2-to-v3 playback-field migration, transactional replacement of embedded
lyrics and raw tags, plus normalized album, artist, genre, and relationship
queries. `AppPreferencesRepositoryTest` verifies that `playMode` defaults and
persists through KMP Preferences DataStore rather than Room.

## Notes

- The legacy Rust database path has been removed from `tidetune-core`; Rust now
  receives Room-derived `Storage` values for remote operations.
- `RoomLibraryIntegrationTest` verifies the new Room v2-to-v3 migration,
  RoomLibraryStore playlist creation, source location persistence, lyric removal,
  and duration updates.
- `RemoteLibraryImportCoordinatorTest` verifies unchanged-file skipping by
  remote fingerprint, stable positive track IDs, and mapping of Rust metadata
  DTOs into Room `TrackEntity` fields, including `sourceStorageId` and
  `sourcePath`. It also verifies that a 1,007-file snapshot is normalized into
  bounded 100-file import batches, and that a stable-remote-ID move skips
  metadata while preserving track identity.
- `services::remote_scan::tests::scans_music_in_bounded_batches_and_can_cancel`
  verifies the Rust `RemoteMusicScanSession` directory queue, bounded batch
  delivery, music filtering, and explicit cancellation.
- `services::remote_scan::tests::cancellation_interrupts_an_in_flight_directory_request`
  verifies that cancellation drops a blocked listing future without waiting
  for the remote request timeout.
- `ImportStatusRepositoryTest` verifies Room import-job mapping, checkpoints,
  error state, and active-job state used by the Dashboard.
- `tidetune-metadata` tests verify extended normalized tags, synchronized
  lyrics, bounded raw text metadata, codec/lossless properties, and rejection
  of an oversized text tag.
- OneDrive OAuth tests verify random PKCE verifier/state generation and the
  RFC 7636 S256 challenge. Android and iOS callbacks validate the returned
  state, and the temporary verifier is kept in the platform credential store.
  A live Microsoft account authorization test is still pending.
- OneDrive Graph tests verify paginated Drive response parsing, delta
  file/folder/deletion parsing, parent DriveItem IDs, trusted cursor validation,
  `token=latest`, and explicit Drive URL construction. KMP tests verify delta
  entry mapping and full-scan fallback rules; generated Room DAO tests verify
  stable-ID deletion and transactional delta cursor persistence. Live Graph
  delta verification is still pending.
- `ImportRepositoryTest` verifies the current-directory import mode used by the
  library-folder picker and that normal entry import resets the picker mode.
- `LibraryRepositoryTest` verifies mapping from Room `TrackEntity` rows to the
  UI-facing library track model without requiring remote scan state.
- `controllers::storage::tests::detects_supported_music_extensions_case_insensitively`
  verifies the Rust recursive scanner's music-file filter.
- Recursive imports now create the Room job before enumeration, stream
  100-file batches from Rust into bounded metadata reads and Room transactions,
  and defer deletion reconciliation until the scan is complete. The Dashboard
  observes these persisted counters and can cancel the active Rust session.
- The Rust workspace now satisfies the intended CI gate for formatting,
  clippy, and tests.
- Generated UniFFI Kotlin currently emits warnings, but they do not fail the
  verified Gradle commands.

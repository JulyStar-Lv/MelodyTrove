# Room-only persistence verification

Date: 2026-06-26

## Scope

This verification covers removal of the Rust app database path and the switch to
Room as the only local relational app database. Lightweight non-relational
preferences now live in KMP Preferences DataStore. Rust remains responsible for
remote storage, metadata reads, asset streaming, OneDrive delta pages, and
playback range serving.

## Test cases added

| Test | Coverage |
| --- | --- |
| `migrationTwoToThreeAddsRoomOnlyPlaybackColumns` | Verifies `track.sourceStorageId`, `track.sourcePath`, `playlist.coverStorageId`, and `playlist.coverPath` are added by Room migration 2->3 |
| `roomLibraryStoreCreatesPlaylistTracksAndRemoteLocWithoutLegacyDatabase` | Verifies playlist creation, track title preservation, source location persistence, and playback location resolution from Room |
| `roomLibraryStoreUpdatesDurationAndRemovesLyricsInRoom` | Verifies duration refresh and lyric removal update Room only |
| `RemoteLibraryImportCoordinatorTest.mapsRemoteMetadataToTrackEntity` | Verifies imported tracks keep `sourceStorageId` and normalized `sourcePath` |
| `AppPreferencesRepositoryTest.persistsPlayModeInDataStore` | Verifies play mode defaults to `SINGLE` and persists through KMP Preferences DataStore |

## Verification commands

| Command | Result |
| --- | --- |
| `cargo check -p tidetune-core` | Passed |
| `cargo test -p tidetune-core` | Passed; 7 tests |
| `./gradlew :shared:desktopTest` | Passed; 29 tests including DataStore play-mode persistence |
| `./gradlew :shared:compileDebugKotlinAndroid` | Passed |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | Passed |
| `git diff --check` | Passed |
| `rg -n "redb|DatabaseServer|database_server|tidetune-legacy|LegacyLibraryMirror|ctUpsertStorage|ctRemoveStorage|ctListStorage\\(" shared/src rust-libs/tidetune-core rust-libs/Cargo.toml rust-libs/Cargo.lock -g '!**/build/**' -g '!**/target/**'` | No matches |

## Notes

- Gradle builds still emit existing warnings from generated UniFFI Kotlin and
  unrelated preview/deprecation warnings; they do not fail compilation or tests.
- Historical migration documents may still mention the former Rust database as
  background. Runtime code and current active architecture/schema docs now treat
  Room as the only local relational app database.
- Room owns relational library state. KMP Preferences DataStore owns lightweight
  non-relational settings such as `playMode`.

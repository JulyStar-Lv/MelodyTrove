# TideTune migration plan

Date: 2026-06-24

## Assumptions and decisions

1. The upstream `kmp` branch is the only source baseline. Reusable functionality
   is migrated rather than rewritten.
2. `Design/` is an accepted TideTune design input and remains in the repository.
3. Room KMP becomes the UI-facing source of truth. Rust does not write Room
   tables directly; it returns typed batches to a KMP import coordinator.
4. Rust remains authoritative for remote storage, bounded range reads, metadata
   extraction, and Desktop decoding.
5. Credentials are referenced by opaque IDs in Room. Platform secure stores own
   the secret values.
6. Gobley Cargo + UniFFI Gradle plugins are the preferred bridge because the
   official plugin supports Kotlin Multiplatform binding generation and Cargo
   packaging. A minimal spike must pass before deleting the current bindings.
7. Room 2.x KMP is the initial stable database line. Room 3.0 is currently an
   alpha line and is not required for this migration.
8. Existing features stay usable at each migration checkpoint. Temporary
   adapters are allowed only when fully implemented and covered by tests.

References:

- Gobley UniFFI plugin: <https://gobley.dev/docs/gradle-plugins/uniffi/>
- Gobley Android/iOS tutorial: <https://gobley.dev/docs/tutorial/>
- Gobley Cargo plugin: <https://gobley.dev/docs/next/gradle-plugins/cargo/>
- Room KMP setup: <https://developer.android.com/kotlin/multiplatform/room>
- Compose Multiplatform setup:
  <https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html>

## Phase 0: reproducible baseline

Changes:

- retain the upstream commit and `Design/` input;
- fix the macOS license-path conflict without changing license terms;
- document required Java, Rust, Android, and Xcode toolchains;
- add a Gradle Java toolchain declaration and `rust-toolchain.toml`;
- record dependency-network failures separately from source failures.

Verify:

```text
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
cargo test --workspace
```

Success means each command reaches source compilation with provisioned
toolchains; network or machine prerequisites are explicit.

## Phase 1: identity migration

Changes:

- rename the root project, application IDs, namespaces, source directories,
  packages, app labels, manifest components, OAuth scheme, Desktop main class,
  package name, data directories, and database filename;
- rename Rust crates and Rust module imports incrementally;
- rename UniFFI namespace and native library;
- update scripts, CI, README, tests, and resources;
- preserve required historical/upstream attribution.

Verify:

```text
rg -n "com\\.kutedev\\.easemusicplayer|EaseMusicPlayer|Ease Music Player|easemusicplayer|ease-client-" \
  --glob '!docs/migration/package-rename-report.md' \
  --glob '!docs/architecture/current-state.md'
./gradlew :androidApp:assembleDebug :desktopApp:compileKotlinDesktop
cargo test --workspace
```

## Phase 2: platform-capable shared module

Changes:

- add `iosArm64()` and `iosSimulatorArm64()` targets;
- create `iosMain`, `iosTest`, and missing common/platform test source sets;
- create an `iosApp` Xcode project with bundle ID `com.github.tidetune`;
- move JNA/JVM APIs to Desktop-only code;
- replace Java time/URL APIs in common code with Kotlin/common equivalents;
- isolate generated Rust bindings behind `RustMusicCore`;
- create explicit close/release ownership for native handles and application
  coroutine scopes.

Verify:

```text
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -configuration Debug build
```

## Phase 3: Gobley/UniFFI build spike

Changes:

- create `tidetune-core` and `tidetune-ffi` crate identities;
- configure Gobley Cargo and UniFFI plugins;
- export one asynchronous typed function and one typed error;
- build and call it from Android, Desktop, iOS Simulator, and iOS device
  compilation targets;
- remove checked-in generated JVM bindings only after the spike passes.

Verify:

```text
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:packageDistributionForCurrentOS
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Fallback: retain UniFFI 0.28 temporarily with generated JVM and Kotlin/Native
bindings if a documented Gobley/Kotlin incompatibility remains after a bounded
spike. Handwritten JNI is not an acceptable fallback.

## Phase 4: Room KMP library

Changes:

- add bundled SQLite, KSP schema generation, platform database builders, and
  `tidetune.db`;
- implement storage, selected folder, remote file, track, album, artist, genre,
  artwork, lyrics, raw metadata, import job, sync cursor, playlist, and
  cross-reference entities;
- add indexed DAOs with Flow, paging, upsert, transactions, and migrations;
- retire the Rust database path so KMP repositories write Room directly;
- remove plaintext credentials from persisted models.

Verify:

```text
./gradlew :shared:allTests
./gradlew :shared:verifyRoomSchemas
```

Tests cover DAO CRUD/upsert, transaction rollback, migration, rename/move/delete,
and a 50,000-track query/import benchmark.

## Phase 5: remote model, range reader, and metadata

Changes:

- split Rust into `tidetune-core`, `tidetune-ffi`,
  `tidetune-remote-storage`, `tidetune-metadata`,
  `tidetune-player-desktop`, and `tidetune-test-support`;
- implement typed `RemoteNode` with stable IDs and remote metadata;
- add bounded `RemoteRangeReader`, block cache, validation, request/byte budgets,
  cancellation, retry, rate limits, and explicit no-range fallback;
- add `lofty` metadata normalization, raw tags, lyrics, audio properties, and
  size-limited artwork extraction;
- implement Graph drive selection, DriveItem IDs, PKCE, token refresh, and delta;
- keep WebDAV at controlled `Depth: 1` traversal.

Verify:

```text
cargo fmt --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
```

## Phase 6: import and synchronization

Changes:

- implement a KMP `ImportCoordinator` that consumes Rust batches and writes Room
  transactions;
- persist import jobs, checkpoints, errors, statistics, and sync cursors;
- implement initial import, cancellation, resume, manual/automatic sync, retry,
  additions, modifications, deletion, rename, move, duplicates, multiple
  accounts, and multiple selected folders;
- expose repository Flow and ViewModel StateFlow from Room only.

Verify:

- integration tests for WebDAV recursive import and OneDrive pagination/delta;
- interrupted import resume and offline Room display tests;
- no UI or ViewModel import of generated FFI packages;
- no transient scan list used as the formal library.

## Phase 7: players

Changes:

- replace `PlayerController` with the complete shared `MusicPlayer` contract;
- adapt Android Media3 and MediaSession;
- implement iOS AVPlayer/AVQueuePlayer, audio session, now playing, and remote
  commands;
- implement Desktop Symphonia + CPAL with bounded buffers, seek, volume,
  ReplayGain, output changes, and cleanup;
- reuse the Rust remote reader/cache for all remote playback.

Verify:

- shared player-state tests;
- Android service tests;
- iOS Simulator player lifecycle test;
- Desktop current-host playback smoke test.

## Phase 8: TideTune UI

Changes:

- implement `TideTuneTheme`, colors, typography, shapes, spacing, motion, and
  elevation from `Design/design-system/tokens.json`;
- migrate existing screens surgically and add the missing onboarding, library,
  folder selection, progress, album, artist, search, queue, lyrics, sync, and
  error-detail screens;
- implement compact/medium/expanded navigation and a real Desktop three-column
  layout;
- keep UI network-free and FFI-free.

Verify:

- Compose UI tests for navigation and state;
- screenshots at compact, medium, and expanded widths in light/dark themes;
- current-host interactive Desktop run and Android emulator smoke test.

## Phase 9: security, CI, and final documentation

Changes:

- implement Android Keystore/Credential Manager, iOS Keychain, and Desktop OS
  credential-store adapters;
- redact URLs, credentials, tokens, and FFI errors;
- constrain loopback listeners to `127.0.0.1` with short-lived random tokens;
- add path traversal, oversized metadata/artwork, corrupt audio, and panic
  boundary tests;
- add Android, iOS, Desktop matrix, Rust, Kotlin lint, and Room schema CI;
- produce all required architecture, database, remote-storage, FFI, rename, and
  test reports.

Final verification:

```text
./gradlew clean check
cargo fmt --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
xcodebuild ... test
```

The migration is complete only when the acceptance checklist in the development
goal is backed by commands and test artifacts, not by placeholder interfaces or
TODO comments.

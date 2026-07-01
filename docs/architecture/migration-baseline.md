# TideTunes Migration Baseline (Historical Archive)

Date: 2026-06-24 (original) / archived 2026-06-30

This document combines the original pre-migration state assessment and migration plan
into a single historical archive. The current project architecture is documented in
`final-architecture.md`; active task tracking is in `komi-cmp-task.md`.

---

# TideTunes migration: current state

Date: 2026-06-24  
Upstream: `https://github.com/hpp2334/ease-music-player.git`  
Branch: `kmp`  
Reviewed commit: `897ce0747dce191070fcc91711b5369e04df903c`

## Scope and baseline

The workspace was populated from the upstream `kmp` branch. The pre-existing
`Design/` directory is not part of the upstream commit and is retained as the
TideTunes UI specification.

The upstream checkout has a case-insensitive-filesystem issue before any source
change: the tracked `LICENSE` symlink conflicts with the tracked `license/`
directory on macOS, so Git reports `D LICENSE`. The GPL-3.0 text remains present
at `license/LICENSE-GPL`.

Baseline commands:

| Area | Command | Result |
| --- | --- | --- |
| Desktop | `./gradlew :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain --stacktrace` | Passed in 29s after retrying an initial Maven Central TLS handshake failure. Kotlin emitted the same two existing `FlowPreview` warnings. |
| Android | `./gradlew :androidApp:assembleDebug --no-daemon --console plain --stacktrace` | Passed in 1m 50s. Kotlin emitted two existing `FlowPreview` opt-in warnings. |
| Rust | `cargo test --workspace` in `rust-libs/` | Failed before compilation because local Cargo 1.65 does not support crates using Rust Edition 2024. |

Local toolchains observed during the baseline:

- macOS 26.4 arm64
- Xcode 26.4
- Java 17.0.10; no Java 21 JDK is installed
- Gradle 8.13
- Kotlin Gradle plugin 2.1.21
- Rust/Cargo 1.65.0

The Android baseline currently compiles on the installed Java 17 runtime despite
the Android modules declaring Java 21 source/target compatibility (there are no
Java sources in those modules). A reproducible build should still provision the
declared Java toolchain explicitly. The Rust crates using Edition 2024 require a
current Rust toolchain.

## Gradle modules and dependency direction

The root build contains:

```text
:
├── :shared
├── :androidApp
└── :desktopApp
```

- `shared` is a Kotlin Multiplatform module with `androidTarget()` and
  `jvm("desktop")`.
- `androidApp` is the Android application and depends on `shared`.
- `desktopApp` is the Compose Desktop application and depends on `shared`.
- There is no iOS target, iOS source set, framework binary, CocoaPods
  integration, or Xcode app.
- There is a separate legacy `android/` Gradle build containing duplicated
  Android application sources. It is not included by the root settings file.

Important dependency problems:

- `commonMain` directly depends on JNA.
- generated UniFFI/JNA bindings are checked into
  `shared/src/commonMain/kotlin/uniffi`.
- common code imports JVM-only `java.net.URLDecoder`, `java.time.Duration`, and
  other Java APIs.
- `desktopApp` points `jna.library.path` at `rust-libs/target/debug`, so packaged
  applications depend on a developer-local Cargo output.

These constraints prevent the current shared module from compiling for iOS.

## Kotlin packages and source layout

The Kotlin root package is `com.kutedev.easemusicplayer`. Android uses the same
namespace and application ID; `shared` uses
`com.kutedev.easemusicplayer.shared`. The Desktop main class is
`com.kutedev.easemusicplayer.MainKt`.

Current shared source sets:

```text
shared/src/
├── commonMain
├── androidMain
└── desktopMain
```

There are no shared, Android, Desktop, or iOS test source sets in active use.

## Rust workspace and FFI

The Rust workspace currently contains:

```text
rust-libs/
├── ease-client-backend
├── ease-client-android-ffi-builder
├── ease-client-schema
├── ease-client-tokio
├── ease-order-key
└── ease-remote-storage
```

`ease-client-backend` is a `cdylib`/`rlib` and uses UniFFI 0.28.3 proc-macro
exports. Generated Kotlin bindings are committed to the shared common source
set and use JNA. Cargo build and native-library packaging are driven by custom
TypeScript scripts rather than Gradle. There is no iOS binding or native
packaging path.

The FFI surface is fine-grained and function-oriented. UI-facing repositories
and ViewModels import generated UniFFI records and functions directly. Native
object cleanup exists for the top-level `Backend`, but lifecycle ownership is
not consistently represented by a closeable KMP abstraction.

## Remote storage

`ease-remote-storage` provides:

- local filesystem access;
- WebDAV `PROPFIND Depth: 1`, Basic/Digest authentication, directory listing,
  and streamed GET;
- OneDrive OAuth token redemption/refresh, children pagination, path-based
  listing, and streamed GET.

Current gaps:

- `Entry` exposes only name, path, optional size, and directory status. It has no
  stable remote ID, parent ID, MIME type, ETag, cTag, or timestamps.
- OneDrive addresses files by path rather than DriveItem ID and has no drive
  selection or delta synchronization.
- OAuth uses the custom `easem://oauth2redirect/` scheme and does not implement
  PKCE.
- WebDAV and OneDrive request `Range: bytes=start-`, an unbounded range.
- `200 OK` fallback and `Content-Range` validation are not safely bounded.
- there is no block cache, adjacent-range coalescing, metadata read budget,
  cancellation model, or request concurrency policy.
- no `lofty` metadata extraction exists.

## Import flow and persisted data

The current music import path is:

```text
Compose screen
  -> KMP ViewModel/repository
  -> Room DAO transaction
  -> Room Flow refresh

Remote directory listing, metadata reads, asset streaming, OneDrive delta, and
range playback call Rust services with a Room-derived Storage DTO.
```

Directory entries are still displayed from an FFI directory-list response, but
selected files are persisted into Room. Metadata import writes normalized track,
album, artist, genre, lyrics, and raw-tag rows. Duration can also be refreshed
from the Android player back into Room.

There is no recursive selected-folder library import, import job, progress,
resume, retry, incremental sync, rename/move/delete reconciliation, or
database-backed library screen independent of playlists.

## Database

The local app database is Room KMP, created as `tidetunes.db`. Binary artwork and
credentials stay outside Room.

Persisted models include storage, selected folders, remote files, tracks,
albums, artists, genres, artwork references, lyrics, raw metadata, import jobs,
sync cursors, playlists, and cross-references. Lightweight UI preferences such
as `playMode` are stored in KMP Preferences DataStore. Storage credentials are
stored through platform credential stores and are attached to Rust calls only at
the repository boundary.

Room DAO integration tests cover migrations, generated DAO behavior,
transaction rollback, large-library paging, and Room-only playlist/playback
location writes.

## Dependency injection and state

Koin is used across shared code. `appModule` creates a process-level coroutine
scope, the FFI `Bridge`, repositories, and ViewModels. Platform modules bind
Android or Desktop player and permission implementations.

Repositories expose in-memory `MutableStateFlow` values and explicitly reload
from Rust. They do not observe a persistent Kotlin database. Several ViewModels
and repositories call generated FFI functions directly, so the Rust boundary
is not isolated behind a domain interface.

## Players

Android:

- Media3 ExoPlayer, `MediaSession`, and `MediaSessionService`;
- a custom Media3 `DataSource` streams bytes through Rust;
- lifecycle cleanup releases player/session and cancels the service scope.

Desktop:

- `DesktopPlayerController` is an empty implementation returning zero/default
  values;
- no Symphonia, CPAL, queue, seek, buffering, or system media integration is
  implemented.

iOS:

- no player implementation exists.

The shared `PlayerController` does not expose the complete queue/state contract
required by TideTunes.

## UI and navigation

The shared Compose UI contains:

- a three-page horizontal home pager;
- playlists, dashboard, settings, storage editor, remote entry picker, playlist
  details, player, logs, and debug screens;
- a mini player and Android/Desktop platform adapters.

Navigation is shared Navigation Compose. The current theme is a small Material 3
color override named `EaseMusicPlayerTheme`; it has no TideTunes spacing, shape,
motion, elevation, or responsive-window system. Desktop reuses the phone layout.

The pre-existing `Design/` package defines TideTunes colors, typography, spacing,
responsive behavior, and target screens. It can be reused without replacing
the functional Compose screens wholesale.

## Code that can be retained

- Compose Multiplatform screen and navigation foundations.
- Koin module structure, after isolating platform and FFI dependencies.
- Android Media3 service/data-source concepts.
- WebDAV `Depth: 1` traversal and Digest authentication logic.
- OneDrive Graph request/pagination foundations.
- Rust async runtime, streaming response, error mapping, and UniFFI foundations.
- playlist and lyric behavior that can be adapted to Room-backed domain models.
- the GPL-3.0 license and existing copyright history.

## Code that must be refactored

- all product/package/crate identifiers;
- common-source JVM dependencies and checked-in JNA bindings;
- Gradle/Cargo native build integration;
- credential persistence;
- remaining historical migration docs and assumptions from the former Rust DB
  implementation;
- direct UI/ViewModel FFI calls;
- path-only remote models and unbounded range reads;
- metadata extraction and artwork persistence;
- import/sync coordinator;
- Desktop player and all iOS platform implementations;
- responsive TideTunes UI system and missing library screens.

---

# TideTunes migration plan

Date: 2026-06-24

## Assumptions and decisions

1. The upstream `kmp` branch is the only source baseline. Reusable functionality
   is migrated rather than rewritten.
2. `Design/` is an accepted TideTunes design input and remains in the repository.
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
- create an `iosApp` Xcode project with bundle ID `com.github.tidetunes`;
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

- create `tidetunes-core` and `tidetunes-ffi` crate identities;
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
  `tidetunes.db`;
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

- split Rust into `tidetunes-core`, `tidetunes-ffi`,
  `tidetunes-remote-storage`, `tidetunes-metadata`,
  `tidetunes-player-desktop`, and `tidetunes-test-support`;
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

## Phase 8: TideTunes UI

Changes:

- implement `TideTunesTheme`, colors, typography, shapes, spacing, motion, and
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

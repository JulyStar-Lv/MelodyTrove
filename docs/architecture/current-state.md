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

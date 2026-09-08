# Stage A Runtime Extraction Plan

Review basis: `HEAD` at `45bba3c5` and the accepted Stage A contract in
`ARCHITECTURE_DECISION.md`. Phase 0 remains explicitly skipped. This plan is
limited to isolating `:core:runtime`; it does not restate the Car UI plan.

## Decision

Do **not** move the currently proposed runtime roots in one batch. The listed
roots contain at least four forbidden edges, and the platform `expect`/`actual`
files mix runtime and Compose APIs. The safe order is: scaffold an empty module,
remove the known dependency inversions, move independently compilable runtime
clusters, then move DI/bootstrap and switch the existing entry points.

A real runtime bootstrap cannot be the first implementation slice. Today
`appModule` closes over Mobile feature modules, `playbackModule` includes
`:service:playback:presentation`, and the runtime providers instantiate classes
that still live in `:shared`. An early `RuntimeModules.kt` could only be an empty
or caller-supplied wrapper; it would not prove that Car can construct the Tide
runtime. Add the real `runtimeModules` only after Slices 2-4 compile without a
`:shared`, `:feature:*`, or presentation dependency.

The accepted architecture contract needs these corrections:

1. `core/data/**` is not a valid move root by itself. Keep
   `LegacyEditPlaylistGateway.kt` and `media/RepositoryArtworkImageLoader.kt` in
   `:shared`; split bitmap caching out of `AssetRepository.kt` before moving it.
2. `ManualMetadataService.kt` stays in `:shared` with its dialog and Mobile Koin
   binding. It accepts the presentation DTO `NowPlayingTrackItem` and is not
   needed to construct playback or the first Car shell.
3. Do not move `AppLocaleEnvironment*`, `BackHandler.kt`, `RemoteImageBytes*`,
   or the existing mixed `PlatformExpect/Actual` files verbatim. Split neutral
   runtime platform functions into their own `expect`/`actual` files first.
4. `HomeStatisticsRepository` and the listening-statistics models used by
   `PlayerRepository` must move to a neutral contract package. Do not move all
   of `HomePersistenceModels.kt`: it also contains Mobile pinned/history models
   and imports `core:presentation.QualityBadgeType`.
5. Search contracts must leave `:feature:search` before its Room implementations
   can enter runtime. Playlist editing does not need a new neutral contract in
   Stage A because its adapter remains Mobile-owned.

## Evidence

- `shared/build.gradle.kts` combines Room, UniFFI, all source/service modules,
  Compose, `:core:presentation`, `:service:playback:presentation`, and nearly all
  `:feature:*` modules. Therefore `carApp -> shared` cannot satisfy the boundary.
- `shared/.../di/AppModule.kt` includes runtime and Mobile feature modules in a
  single Koin module; `Koin.kt` always loads that module.
- `shared/.../di/PlaybackModule.kt` directly includes
  `playbackPresentationModule`.
- `shared/.../di/CoreDataModule.kt` imports Home data and domain types from
  `:feature:home`. `PlayerRepository.kt` and
  `PlayerPlaybackStatisticsTracker.kt` also import those Home contracts.
- `shared/.../di/SearchFeatureModule.kt` imports both Search presentation DI and
  Search data/domain types from `:feature:search`.
- `HomePersistenceModels.kt` imports
  `core.presentation.components.QualityBadgeType`; moving the whole file would
  create a forbidden runtime-to-presentation edge.
- `ManualMetadataService.kt` imports
  `service.playback.presentation.nowplaying.NowPlayingTrackItem`.
- `AssetRepository.kt` stores Compose `ImageBitmap` values and calls
  `byteArrayToImageBitmap`; `PlatformExpect.kt` and all three
  `PlatformActual.kt` files declare/implement that Compose conversion beside
  neutral filesystem, clock, locale, and capability functions.
- `Bridge.kt` imports three Compose runtime symbols, although none is used. The
  imports must be removed rather than adding Compose to runtime.
- `PlatformModule.kt` cannot be moved alone. Its Android implementation needs
  the Android controller and scheduler implementations; Desktop and iOS need
  their respective player/controller/scheduler implementations. Moving only the
  three `PlatformModule.kt` files leaves unresolved symbols.
- `AppDatabase.kt` declares `APP_DATABASE_VERSION = 25`. No Stage A slice needs
  an entity or migration change.
- `PlaybackService.kt` reflects `io.github.julystar.musicapp.MainActivity`.
  Runtime ownership requires package-local launcher resolution before Car can
  reuse the service implementation.
- iOS playback uses `shared/src/nativeInterop/cinterop/AudioProcessingTap.def`
  and headers in `iosApp`. Moving iOS playback without moving the cinterop
  configuration breaks the Apple target even if Android compiles.

## Affected Modules

| Module | Stage A role |
| --- | --- |
| `:core:runtime` | New KMP Android/Desktop/iOS implementation module; Room and UniFFI owner after extraction. |
| `:core:domain` | Receives only neutral Home listening-statistics and Search contracts/models. |
| `:shared` | Remains Mobile/Desktop/iOS presentation shell and consumes `:core:runtime`. |
| `:feature:home` | Consumes moved neutral statistics contracts; retains pinned/history UI models and presentation. |
| `:feature:search` | Consumes moved neutral Search contracts; retains ViewModels, screens, navigation, and presentation DI. |
| `:service:playback:presentation` | Remains unchanged except import updates caused by neutral contract moves; never becomes a runtime dependency. |
| `:androidApp`, `:desktopApp`, iOS framework | Existing entry points switch composition only in the final slice and remain regression gates. |
| `:source:*`, `:service:download:*`, `:service:librarysync:*`, `:service:playback:domain` | Existing UI-neutral dependencies of runtime; no Car duplicates. |

## Affected Files

Paths below name the real current files or directories. A directory move means
the same relative package path under `core/runtime/src/<sourceSet>/kotlin`.
Move the corresponding focused tests with each cluster; leave UI tests in
`:shared`.

### Slice 0 - empty module and one-way edge

Create only:

- `core/runtime/build.gradle.kts`
- `settings.gradle.kts` (`include(":core:runtime")`)
- `shared/build.gradle.kts` (`implementation(project(":core:runtime"))`)

Move no source files. The runtime build must use the KMP library, Room,
Cargo/UniFFI, atomicfu, and Android library plugins needed by the eventual code,
  but no Compose/CMP or feature convention plugin. Cargo/UniFFI ownership remains
  exclusively in `:shared` while this module is empty: applying the plugin in both
  modules makes both tasks claim `rust-libs/target/.../libapp_backend.so`, which
  Gradle 8.14 rejects as an undeclared cross-task output dependency. Transfer the
  Cargo/UniFFI plugin and its configuration to `:core:runtime` atomically with the
  first Rust binding consumer; never configure two owners. Declare only neutral project
dependencies: `:core:domain`, `:core:data`, `:core:lyrics-core`, `:source:*`,
`:service:playback:domain`, `:service:download:*`, and
`:service:librarysync:*`. Do not add `:shared`, `:core:presentation`,
`:service:playback:presentation`, or `:feature:*` even temporarily.

Required dependencies: Gradle convention build and the neutral modules above.

Presentation reverse dependencies: none, because no runtime source exists yet.

Acceptance:

```powershell
$env:JAVA_HOME='C:/Software/Android Studio/jbr'
./gradlew.bat :core:runtime:compileCommonMainKotlinMetadata `
  :core:runtime:compileKotlinDesktop `
  :core:runtime:compileDebugKotlinAndroid `
  :shared:compileKotlinDesktop `
  :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --console plain
```

Rollback: remove the single dependency, include, and empty module. No runtime
behavior changes.

### Slice 1 - remove boundary blockers before any bulk move

Move/split:

- Split neutral listening-statistics types out of
  `feature/home/.../domain/HomePersistenceModels.kt`, and move
  `feature/home/.../domain/HomeStatisticsRepository.kt` plus those neutral types
  to `core/domain/src/commonMain/...`. Update
  `shared/.../feature/home/data/RoomHomeStatisticsRepository.kt`,
  `shared/.../service/playback/data/PlayerRepository.kt`, and
  `PlayerPlaybackStatisticsTracker.kt` imports. Keep
  `HomeHistoryRepository.kt`, `HomePinnedRepository.kt`, `PinnedHomeItem`, and
  `HistoryPlayItem` in `:feature:home`.
- Move `feature/search/.../domain/SearchModels.kt` to a neutral Search package in
  `:core:domain`; update Search feature and the two shared Search data files.
- Refactor `shared/.../core/data/media/AssetRepository.kt` to bytes/stream only.
  Move its Compose bitmap cache/adapter beside
  `RepositoryArtworkImageLoader.kt` in `:shared`.
- Split `shared/.../platform/PlatformExpect.kt` and Android/Desktop/iOS
  `PlatformActual.kt`: neutral directory, cache, database path, platform/process
  name, settings capabilities, language application, clock, and app-context
  functions are runtime candidates; `byteArrayToImageBitmap` and `BackHandler`
  remain in `:shared`.
- Remove the unused Compose imports from `shared/.../singleton/Bridge.kt`.
- Split Koin declarations in place, without moving them yet:
  `PlaybackModule.kt` must stop including `playbackPresentationModule` and must
  stop binding `ManualMetadataService`; `LibraryFeatureModule.kt` must separate
  repository bindings from image-loader, edit-playlist, and feature DI includes;
  `SearchFeatureModule.kt` must separate data bindings from `searchFeatureDiModule`;
  `CoreDataModule.kt` must move the neutral `ToastRepositoryImpl` binding out of
  `SettingsFeatureModule.kt` and keep Home history bindings Mobile-owned.

Required dependencies: only existing module dependencies. `:core:domain` must
not depend on `:source:api`; this is why `EditPlaylistGateway` is deliberately
not moved.

Presentation reverse dependencies to eliminate from the intended runtime
closure: `QualityBadgeType`, `NowPlayingTrackItem`, `ArtworkImageLoader`,
`playbackPresentationModule`, all `*FeatureDiModule` values, Compose bitmap and
Composable APIs.

Acceptance:

```powershell
rg -n "^(import (androidx\.compose|org\.jetbrains\.compose)|.*@Composable|.*ImageBitmap)" `
  shared/src/commonMain/kotlin/io/github/julystar/musicapp/core/data/media/AssetRepository.kt `
  shared/src/commonMain/kotlin/io/github/julystar/musicapp/singleton/Bridge.kt
rg -n "io\.github\.julystar\.musicapp\.(feature\.|core\.presentation|service\.playback\.presentation)" `
  shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/CoreDataModule.kt `
  shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/PlaybackModule.kt
./gradlew.bat :core:domain:allTests :feature:home:allTests :feature:search:allTests `
  :shared:allTests --no-daemon --no-configuration-cache --console plain
```

The two `rg` commands must return no match for the listed runtime candidates.

**First mandatory STOP:** stop before Slice 2 if the intended runtime files still
import Compose, `:core:presentation`, `:service:playback:presentation`, or any
`:feature:*` package; if neutralizing them requires a Room schema change, a
Repository/`PlaybackController` API change, a `:core:domain -> :source:api`
cycle, or moving a screen/ViewModel. This is the first point where the accepted
contract is known to be insufficient; continuing would merely hide the problem
behind a large move.

### Slice 2 - persistence, diagnostics, bridge, and platform primitives

#### Slice 2 dependency-closure amendment

The original Slice 2 root list contains a real source-level cycle with the
planned Slice 3 and Slice 4 roots. `LibraryDao` directly uses the database-bound
identity/lyrics algorithms, while `RoomLibraryStore` directly uses remote-path
identity helpers and the entity-to-playback-audio mapper. Moving persistence
without those files would require the forbidden `:core:runtime -> :shared`
edge. Therefore Slice 2 also moves the following minimal non-UI closure while
preserving every package name and public API:

- `metadata/LyricsQualitySelector.kt`, `metadata/TrackIdentityMatcher.kt`, and
  `metadata/TrackVersionTokens.kt`
- a new `domain/importing/RemotePathIdentity.kt` extracted from
  `RemoteLibraryImportCoordinator.kt`, containing `RemotePathSemantics`,
  `normalizeRemotePath`, `toLegacyAndroidPrimaryStoragePath`, `stableTrackId`,
  and `DURATION_MATCH_TOLERANCE_MS`
- `service/playback/data/AudioTechnicalInfoEntityMappers.kt`
- `core/data/PersistedLyrics.kt`, plus the playlist mutation request models and
  the two selection-to-legacy-entry adapters extracted into runtime-owned files

These files are implementation prerequisites rather than an early start of the
source, metadata-service, or playback-controller migrations. All remaining
`domain/importing/**`, `metadata/**`, and `service/playback/data/**` files stay
in `:shared` until their planned slices and consume these declarations through
the existing `:shared -> :core:runtime` edge. Declarations that were module
`internal` become public only where the existing shell still has to consume
them across that edge; their names and behavior remain unchanged.

Move these coherent roots/files:

- `shared/src/commonMain/.../database/**` and Android/Desktop/iOS
  `database/DatabaseBuilder.kt`
- `shared/src/commonMain/.../migration/**` and Android/Desktop/iOS migration
  actuals
- `shared/src/commonMain/.../core/data/datastore/**` and all platform actuals
- `shared/src/commonMain/.../core/data/security/**` and all platform actuals
- `shared/src/commonMain/.../diagnostics/**`, excluding `SafeModeScreen.kt`, plus
  `PlatformFaultInjection.*` and the platform diagnostic export presenters
- the neutral platform files created in Slice 1, `platform/AppVersion.kt` and all
  its actuals, and `plugin/PlatformTime.kt` plus all actuals
- `shared/src/commonMain/.../singleton/Bridge.kt` and `RoomLibraryStore.kt`
- `shared/src/commonMain/.../core/DataSourceKeyH.kt`
- `shared/schemas/**` to `core/runtime/schemas/**`
- `shared/src/commonTest/...` and `shared/src/desktopTest/...` tests whose system
  under test is wholly in the roots above

Move the generated build-info task and Room/UniFFI/KSP configuration from
`shared/build.gradle.kts` to `core/runtime/build.gradle.kts`. Keep the original
Kotlin package names so existing callers need import changes only where Slice 1
intentionally created neutral packages.

Required dependencies: `:core:domain`, `:core:data`, Room/sqlite, DataStore,
Koin core, coroutines, serialization, datetime, okio, UniFFI, atomicfu; Android
core-ktx for diagnostic sharing.

Presentation reverse dependencies: `SafeModeScreen.kt`, bitmap conversion,
locale CompositionLocal environment, and BackHandler remain in `:shared`.

Acceptance:

```powershell
./gradlew.bat :core:runtime:allTests :core:runtime:compileKotlinDesktop `
  :core:runtime:compileDebugKotlinAndroid :shared:compileKotlinDesktop `
  :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --console plain
rg -n "APP_DATABASE_VERSION\s*=\s*25" core/runtime/src/commonMain
git ls-tree -r --name-only HEAD shared/schemas | ForEach-Object {
  $runtimeSchema = $_ -replace '^shared/schemas', 'core/runtime/schemas'
  if ((git rev-parse "HEAD:$_") -ne (git hash-object $runtimeSchema)) {
    throw "Schema content changed: $runtimeSchema"
  }
}
```

The schema command requires every moved JSON blob to be byte-identical. On macOS also run
`:core:runtime:compileKotlinIosSimulatorArm64` and `:shared:linkDebugFrameworkIosSimulatorArm64`.

### Slice 3 - sources, metadata, library, and Search data

Move:

- `shared/src/commonMain/.../source/storage/**`
- `shared/src/commonMain/.../source/server/**`
- `shared/src/commonMain/.../domain/importing/**`
- `shared/src/commonMain/.../metadata/**`
- `shared/src/commonMain/.../plugin/runtime/**`, `plugin/install/**`, and the
  non-UI management files `MetadataLookupUseCase.kt`, `PlaybackLyricsEnricher.kt`,
  `PluginManager.kt`, `PluginMetaSourceRegistry.kt`, `PluginRepository.kt`
- `shared/src/commonMain/.../core/audio/**`
- `shared/src/commonMain/.../core/data/**`, excluding
  `LegacyEditPlaylistGateway.kt`, `media/RepositoryArtworkImageLoader.kt`, and
  the bitmap adapter created in Slice 1
- `shared/src/commonMain/.../feature/home/data/RoomHomeStatisticsRepository.kt`
  only; keep `RoomHomeHistoryRepository.kt` Mobile-owned
- `shared/src/commonMain/.../feature/search/data/**`
- corresponding non-UI common/desktop tests

Required dependencies: Slice 2, all `:source:*`, `:core:lyrics-core`, and the
neutral Home/Search contracts from Slice 1.

Presentation reverse dependencies: playlist editing adapter, artwork image
loader, Home recent-history adapter, manual metadata service/dialog, plugin
dialogs/settings roots stay in `:shared`.

Acceptance:

```powershell
./gradlew.bat :core:runtime:allTests :core:runtime:compileKotlinDesktop `
  :core:runtime:compileDebugKotlinAndroid :shared:allTests `
  --no-daemon --no-configuration-cache --console plain
rg -n "^import (io\.github\.julystar\.musicapp\.(feature\.[^.]+\.presentation|core\.presentation|service\.playback\.presentation)|androidx\.compose|org\.jetbrains\.compose)" core/runtime/src
```

The static search must return no match. Runtime may preserve historical package
names such as `feature.search.data`; package declarations do not create a Gradle
or source dependency and are not treated as feature-presentation edges.

### Slice 4 - download, sync, playback, and complete platform actuals

Move:

- `shared/src/commonMain/.../service/download/data/**`
- `shared/src/commonMain/.../service/librarysync/data/**`
- `shared/src/commonMain/.../service/playback/data/**`
- Android/Desktop/iOS `service/download/data/scheduler/**`
- Android/Desktop/iOS `service/playback/data/**`
- Android `singleton/PermissionRepository.kt`,
  `singleton/PlayerControllerRepository.kt`, and the complete
  `core/Android*`, `core/MusicPlayer*`, `core/PlaybackService.kt`,
  `core/TidePlayerSessionPlayer.kt`, and `core/audio/**` cluster
- Desktop `singleton/DesktopPermissionChecker.kt`,
  `DesktopPlaybackEngine.kt`, and `DesktopPlayerController.kt`
- iOS `singleton/IosPermissionChecker.kt`, `IosPlaybackEngine.kt`, and
  `IosPlayerController.kt`
- all three `di/PlatformModule.kt` actuals together with their common `expect`
- Android playback resources:
  `notification_small_icon.xml`, `icon_mode_list.xml`, `icon_lyrics.xml`, and
  the runtime strings they reference
- `shared/src/nativeInterop/cinterop/AudioProcessingTap.def` and its cinterop
  configuration, adjusting the header include path to `iosApp`
- corresponding common, Android unit, Desktop, and iOS tests

Before moving `PlaybackService.kt`, replace the reflected Mobile Activity with a
package-local launcher intent lookup and a safe no-launcher fallback. Preserve
the class FQN, queue/session behavior, and the single `ExoPlayer`/
`MediaLibrarySession` owner.

Required dependencies: Slice 3, `:service:playback:domain`,
`:service:download:*`, `:service:librarysync:*`; Android Media3, WorkManager,
core-ktx and existing lyric provider APIs. Desktop/iOS keep their current native
and platform dependencies.

Presentation reverse dependencies: `ManualMetadataService` remains Mobile; the
runtime playback Koin module does not include playback presentation. Desktop
floating lyrics uses AWT/Swing and domain/runtime state; it is not a Compose or
feature dependency.

Acceptance:

```powershell
./gradlew.bat :core:runtime:allTests :core:runtime:testDebugUnitTest `
  :core:runtime:desktopTest :core:runtime:compileDebugKotlinAndroid `
  :shared:allTests :androidApp:assembleDebug :desktopApp:compileKotlinDesktop `
  --no-daemon --no-configuration-cache --console plain
rg -n "Class\.forName\(\"io\.github\.julystar\.musicapp\.MainActivity\"" core/runtime/src
rg -n "ExoPlayer\.Builder|MediaLibrarySession\.Builder" core/runtime/src/androidMain
```

The reflection search must be empty. The builder search must identify only the
existing runtime owner. On macOS, compile both iOS architectures and link the
existing SharedKit framework before continuing.

### Slice 5 - real bootstrap and existing-shell cutover

Move/create:

- Move neutral providers from `CoreDataModule.kt`, `SourceDataModule.kt`,
  `DownloadModule.kt`, `LibrarySyncModule.kt`, the runtime half of
  `LibraryFeatureModule.kt`, the runtime half of `SearchFeatureModule.kt`, and
  `PlaybackModule.kt` into `core/runtime/src/commonMain/.../di/**`.
- Move `AppInitializer.kt` to runtime without changing its public FQN or startup
  ordering.
- Create `core/runtime/.../runtime/RuntimeModules.kt` and make it expose the
  complete common runtime plus the platform runtime module.
- Move `Koin.kt` to runtime as a primitive that accepts explicit additional
  modules; it must not know Mobile modules.
- Keep `shared/.../di/AppModule.kt` as Mobile presentation composition. It adds
  `:core:presentation`, `:service:playback:presentation`, all feature DI modules,
  `RepositoryArtworkImageLoader`, `LegacyEditPlaylistGateway`, Home history, and
  `ManualMetadataService` to `runtimeModules`.
- Update `androidApp/AppApplication.kt`, `androidApp/MainActivity.kt`,
  `desktopApp/Main.kt`, and `shared/src/iosMain/.../MainViewController.kt` to use
  runtime plus Mobile presentation modules. Keep their current startup order and
  one Koin graph per process.

Required dependencies: all prior slices. `:shared -> :core:runtime` is the only
new shell edge; runtime must have no edge back.

Presentation reverse dependencies: all are composed by `:shared` after the
runtime module list, never included inside runtime declarations.

Acceptance:

```powershell
./gradlew.bat :core:runtime:dependencies :shared:allTests `
  :androidApp:assembleDebug :desktopApp:compileKotlinDesktop `
  --no-daemon --no-configuration-cache --console plain
rg -n "project\(\":(shared|core:presentation|service:playback:presentation|feature:)" core/runtime/build.gradle.kts
rg -n "^import (io\.github\.julystar\.musicapp\.(feature\.[^.]+\.presentation|core\.presentation|service\.playback\.presentation)|androidx\.compose|org\.jetbrains\.compose)" core/runtime/src
```

Both static searches must be empty. On macOS, also compile/link the existing iOS
framework and run its moved controller tests. Only after this gate may Stage B
add `:car:presentation` and `:carApp`.

## Files Not To Modify

- `shared/src/commonMain/.../Root.kt`, `navigation/**`, and `core/Routes.kt`
- all feature screens, roots, navigation, widgets, and design resources except
  import/package updates required by the two neutral contract moves
- `core/presentation/**`, `core/lyrics-ui/**`, and
  `service/playback/presentation/**` behavior
- `shared/.../diagnostics/SafeModeScreen.kt`
- `shared/.../plugin/management/ManualMetadataSearchDialog.kt`,
  `PluginSettingsRoot.kt`, and `PluginSettingsText.kt`
- `shared/.../platform/AppLocaleEnvironment*`, `BackHandler.kt`, and
  `RemoteImageBytes*`
- Room entities, migrations, schema JSON content, and database version
- playback queue algorithms, `PlaybackController` contract, Media3 session
  semantics, and source/repository behavior
- `androidApp` application ID, Desktop identity, iOS bundle/framework identity,
  `iosApp/AudioProcessingTap.h`, and `iosApp/AudioProcessingTap.m`
- any `carApp` or Car presentation file; those belong to Stage B

## Implementation Contract

1. Each slice is one reviewable commit. Do not begin the next slice until its
   compile/static gates pass. A failed slice rolls back as a unit; never fix it
   by adding a forbidden dependency.
2. Preserve package names and public APIs except the explicitly listed neutral
   Home/Search package moves and the private bitmap/platform file splits.
3. `:core:runtime` may depend only on neutral domain/data/source/service modules
   and non-UI libraries. It must never depend on `:shared`, a feature module,
   `:core:presentation`, or playback presentation.
4. `runtimeModules` owns all common runtime providers. Platform runtime bindings
   are selected by KMP `expect`/`actual`. Mobile composition lives in `:shared`.
5. `AppInitializer` keeps the exact sequence: settings, database/migration,
   Bridge, plugins, recovery, download/playback restore, repository reload, scan
   scheduling.
6. Android retains one playback service, one `ExoPlayer`, and one
   `MediaLibrarySession` per application process. No controller/player state is
   copied.
7. Android, Desktop, and iOS implementations move together when their shared
   `expect` declaration moves. Windows results never substitute for the macOS
   iOS compile/link gate.
8. Schema 25 and schema JSON content are invariant. Any required schema/entity
   change is a STOP, not an extraction fix.

## Acceptance

Stage A is complete only when all slice gates pass and the final dependency
report/static searches prove the boundary. The minimum final evidence is:

- `:core:runtime` common, Android, and Desktop compilation/tests pass on JDK 21.
- `:shared:allTests`, `:androidApp:assembleDebug`, and
  `:desktopApp:compileKotlinDesktop` pass.
- macOS compiles both iOS targets, links the existing SharedKit framework, and
  runs the moved iOS playback tests.
- runtime dependency/source scans contain no Compose, feature, shared, core
  presentation, or playback presentation edge.
- database version remains 25 and schema JSON content is unchanged.
- the existing Android manifest still resolves exactly one `PlaybackService`;
  its notification/session intent resolves the launcher within the current
  package and contains no reflected Mobile Activity name.
- startup smoke checks confirm settings, database, Bridge, plugins, recovery,
  library reload, download recovery, and playback recovery still run in the
  established order for Android, Desktop, and iOS.

## Risks

- The largest risk is accepting an empty or parameter-only `RuntimeModules` as
  proof of isolation. It proves only Gradle wiring; the real gate is the final
  provider closure compiling with no forbidden edge.
- The accepted move-root list omitted download/library-sync implementations,
  platform schedulers/controllers, native iOS cinterop, and several UI leaks.
  Moving the listed 100-ish files before resolving these omissions produces a
  late, cross-platform failure that is hard to roll back selectively.
- Moving `HomePersistenceModels.kt` wholesale would pull
  `QualityBadgeType` and `:core:presentation` into the neutral layer. Split only
  the listening-statistics types actually used by playback.
- Moving `ManualMetadataService` would pull a playback presentation DTO into
  runtime. Keeping it Mobile-owned is the smallest Stage A solution.
- Moving `LegacyEditPlaylistGateway` to runtime by relocating its contract into
  `:core:domain` would create a `core:domain <-> source:api` cycle through
  `SourceNodeSelection`. Keep both binding and adapter Mobile-owned.
- A runtime build that compiles only Android can still break Desktop and iOS.
  The platform modules must move with complete implementations, and the iOS
  cinterop must be validated on macOS.
- Separate future APKs still have separate package-private databases/settings
  and one playback process each. Runtime extraction shares implementation, not
  physical storage or a cross-package singleton.

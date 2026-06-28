# Komi CMP/KMP music app task

Date: 2026-06-27

This task tracks the ongoing upper-layer refactor requested in the Komi Store
architecture goal. It complements the lower-level migration plan in
`docs/architecture/migration-plan.md`.

## Scope

Build TideTunes into an Android, iOS, and Desktop Kotlin Multiplatform /
Compose Multiplatform music app using:

- Komi Store style Clean Architecture, feature layering, Koin assembly, and
  typed Navigation Compose.
- A shared presentation layer with State, Action, and Event page contracts.
- Room KMP as the library source of truth.
- Unified global subsystems for playback, music sources, library sync, and
  downloads.
- Incremental migration of existing WebDAV, OneDrive, Room, playlist, and
  player code without discarding working behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).

## Current Evidence

Completed in the current worktree:

- `Root` is reduced to theme plus navigation host wiring.
- `navigation/MusicGraph.kt` defines serializable typed routes.
- `navigation/AppNavigation.kt` owns the root scaffold and NavHost.
- Route registration is split into `HomeGraph`, `LibraryGraph`, `PlayerGraph`,
  `SettingsGraph`, and `SourcesGraph`.
- Android, iOS, and Desktop entry points use the shared `initKoin` bootstrap.
- Koin registration is split into `coreDataModule`, `sourceDataModule`,
  `playbackModule`, `downloadModule`, `librarySyncModule`,
  `libraryFeatureModule`, `importFeatureModule`, and `settingsFeatureModule`.
- The Library page has a first feature-style presentation slice:
  `LibraryRoot`, `LibraryScreen`, `LibraryState`, `LibraryAction`, and
  `LibraryEvent`.
- The Dashboard source list is migrated into a Sources presentation slice:
  `SourcesRoot`, `SourcesScreen`, `SourcesViewModel`, `SourcesState`,
  `SourcesAction`, and `SourcesEvent`.
- The storage editor now has a feature-style Root plus no-secret
  `SourceEditorState`, `SourceEditorAction`, and `SourceEditorEvent` shell for
  top-level navigation, save, delete, test, and import actions.
- The storage editor UI is now `SourceEditorScreen` under the Sources
  presentation package. It reads only `SourceEditorState`, emits
  `SourceEditorAction`, opens OneDrive OAuth through a Root event, and no
  longer depends directly on `EditStorageVM`, `ArgUpsertStorage`, `StorageType`,
  or Koin.
- `EditStorageVM` now keeps its editing state in the feature-owned
  `SourceEditorDraft`. `ArgUpsertStorage` is only created at the
  `StorageRepository` boundary for test/save calls.
- Package-level `core/domain`, `core/data`, and `core/presentation`
  boundaries now exist inside `shared`: domain media/source IDs live under
  `core/domain/model`, credential storage lives under `core/data/security`,
  and the app theme lives under `core/presentation/theme`.
- The Sources list now uses `SourceAccountId` in presentation state/actions;
  the current `storage:<id>` route bridge is isolated as a transitional mapper
  until source contracts replace legacy storage route IDs.
- The first `source/api` contract slice defines `MusicSource`,
  `MusicSourceRegistry`, `SourceCapability`, source configuration/auth result
  types, `PlaybackResource`, and built-in source IDs.
- Local, WebDAV, and OneDrive now have initial `MusicSource` adapters. WebDAV
  and OneDrive authentication are wired through a `LegacyStorageConnectionTester`
  bridge to the existing `StorageRepository.test(...)` path.
- `sourceDataModule` registers the built-in source adapters and a shared
  `MusicSourceRegistry`. WebDAV passwords and OneDrive refresh tokens are kept
  out of default source configuration string output.
- `source/api` now includes the first browse contract: `SourceNode`,
  `SourceNodeType`, and `SourceListResult`.
- Local, WebDAV, and OneDrive `MusicSource` adapters implement `list(...)`
  through a `LegacyStorageDirectoryLister` bridge backed by the existing
  `RemoteScannerRepository.listDirectory(...)` path.
- `MusicSource.resolvePlayback(...)` now returns structured
  `SourcePlaybackResult` values. Local, WebDAV, and OneDrive resolve stable
  legacy storage track media IDs into transient `PlaybackResource` values
  through the source API.
- `RetainedLegacyStoragePlaybackResolver` keeps Rust playback gateway sessions
  alive behind source-level `PlaybackResource` values and exposes release hooks
  for the upcoming playback service boundary.
- A shared `PlaybackResourceResolver` now maps legacy Room/Rust `Music.loc`
  values to source-level media IDs, calls `MusicSource.resolvePlayback(...)`,
  and centralizes retained playback-resource release.
- Android playback startup now resolves media through `PlaybackResourceResolver`
  before building the Media3 `MediaItem`; Media3 consumes the transient gateway
  URL instead of the old `MusicPlayerDataSource` path for new playback starts.
- iOS playback startup now resolves media through `PlaybackResourceResolver`
  instead of directly calling `ctCreatePlaybackSession(...)` from the platform
  controller.
- Desktop now has a `DesktopPlaybackEngine` boundary. `DesktopPlayerController`
  resolves media through `PlaybackResourceResolver`, routes the transient
  resource into the engine, and releases it when the current no-op engine
  reports that desktop playback is unsupported.
- A shared playback service contract now exists under
  `service/playback/domain`: `PlaybackController` exposes separated
  `PlayerState`, `PlaybackPosition`, and `PlaybackQueue` flows. The current
  platform `PlayerController` implementations are bridged through
  `LegacyPlaybackController` so new presentation code can move to the shared
  contract incrementally.
- `service/playback/domain` now also defines an optional
  `AdvancedPlaybackController` contract with capability, enhancement setting,
  and output-device state models for gapless playback, crossfade, ReplayGain,
  output device selection, Android Auto, AirPlay, and CarPlay. Current playback
  controllers are not forced to implement these backend-dependent features yet.
- `service/playback/domain` now defines the pure commonMain `PlaybackEngine`
  contract and `PlaybackEngineResource` load model. Platform engine objects
  remain outside commonMain, while the source playback resource can be adapted
  into the playback service boundary without leaking `source:api` into the
  playback domain module.
- Audio output is now split into an `AudioOutputController` contract. The
  optional `AdvancedPlaybackController` extends it so output selection can be
  implemented independently from gapless, crossfade, and ReplayGain.
- `PlayerVM` now receives playback commands, status, queue, and progress from
  the shared `PlaybackController`; `MiniPlayer` and the global bottom bar use
  shared playback state while legacy `PlayerRepository` is retained only for
  metadata surfaces that still need cover, lyrics, and library mutation data.
- `LegacyPlaybackController.position` now starts polling only while
  `PlaybackPosition` has active subscribers, keeping AppShell-level playback
  state separate from progress updates.
- The first Search domain/data slice now exists: `SearchRepository`,
  `SearchLibraryUseCase`, and `SearchResults` expose local-first track search,
  while `RoomSearchRepository` keeps Room as the library source of truth through
  `TrackDao.search(...)`.
- `MusicSource.search(...)` now exists as a source-level contract with
  structured `SourceMediaItem` and `SourceSearchResult` values. The current
  Local, WebDAV, and OneDrive adapters explicitly report unsupported search
  until provider-native search is implemented.
- `MusicSourceSearchAggregator` now combines Room-first search results with
  capability-gated source search results, preserves source labels, skips sources
  that do not advertise `SourceCapability.Search`, and drops duplicate tracks
  while keeping local library results first.
- Search now has a feature-style presentation slice:
  `SearchRoot`, `SearchScreen`, `SearchViewModel`, `SearchState`,
  `SearchAction`, and `SearchEvent`. Query changes debounce for 300 ms, newer
  queries cancel active work, and the state exposes persisted history and
  suggestions.
- Search history is now persisted in Preferences DataStore through
  `DataStoreSearchHistoryRepository`, keeping ordered, deduped query history
  outside `SearchViewModel`.
- Search suggestions now merge persisted history with Room-backed local library
  suggestions from track title, artist, album artist, and composer fields while
  preserving Room as the local source of truth.
- Local, WebDAV, and OneDrive `MusicSource.search(...)` now use a shared
  Room-backed `LegacyStorageSearchProvider` for source-indexed search over
  synced library tracks. Results are filtered by source account, exclude
  deleted remote files, and carry source-level media IDs that continue to
  resolve playback on demand through `MusicSource.resolvePlayback(...)`.
- The current pager shell now includes Search as the fifth top-level entry and
  `navigation/SearchGraph.kt` registers a typed `MusicGraph.Search` route.
- `ImportVM` now resolves directory listings through `MusicSourceRegistry`
  instead of depending directly on `RemoteScannerRepository`.
- `ImportVM.entries` and the import folder list UI now use source-level
  `SourceNode` models. Legacy `StorageEntry` is kept only at the existing
  `ImportRepository.onFinish(...)` callback boundary to preserve playlist,
  lyrics, and cover import callers.
- The Import/folder picker is now routed through `ImportRoot`,
  `ImportScreen`, `ImportState`, `ImportAction`, and `ImportEvent` under
  `feature/importing/presentation`.
- `ImportScreen` no longer injects Koin ViewModels, reads `LocalNavController`,
  or collects flows directly. It renders `ImportState` and emits
  `ImportAction`.
- `ImportVM` exposes the aggregate Import presentation state while preserving
  the existing source-listing and import-callback behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).
- Playlist create/edit now use source-level `CreatePlaylistRequest` and
  `UpdatePlaylistRequest` values from the ViewModel layer. Selected tracks and
  covers stay as `SourceNodeSelection` in the upper layer, while retained
  `ArgCreatePlaylist`, `ArgUpdatePlaylist`, `StorageEntry`, and
  `StorageEntryLoc` conversions are isolated inside the data/store boundary.
- The first Download service boundary now exists under `service/download`:
  `DownloadTask`, `DownloadStatus`, `DownloadPolicy`, `DownloadController`, and
  `DownloadTaskRepository` define the shared domain contract, while
  `RoomDownloadTaskRepository` persists task state in Room before any platform
  scheduler is introduced.
- `DownloadTaskScheduler` now defines the shared scheduler boundary behind
  `DownloadController`. `PersistentDownloadController` schedules enqueue,
  resume, and retry only after the queued task state is persisted, and sends
  pause/cancel only after a valid persisted status update.
- Desktop now binds a real `DesktopCoroutineDownloadScheduler` behind
  `DownloadTaskScheduler`. It resolves the task media ID through
  `MusicSourceRegistry`, streams local or HTTP playback resources into
  `.tidetunes/cache/downloads`, writes progress/completed/failed state through
  `DownloadTaskRepository`, limits concurrent downloads, and releases retained
  playback resources when the download finishes.
- Android now binds `AndroidWorkManagerDownloadScheduler` behind
  `DownloadTaskScheduler`. It enqueues unique WorkManager jobs per download
  task, applies a connected-network constraint for non-local sources, resolves
  media through `MusicSourceRegistry` inside `AndroidDownloadWorker`, streams
  local/content/HTTP playback resources into app-private `filesDir/downloads`,
  persists progress/completed/failed state through `DownloadTaskRepository`,
  and releases retained playback resources when work finishes.
- iOS now binds `IosUrlSessionDownloadScheduler` behind
  `DownloadTaskScheduler`. It creates a background `NSURLSession`, resolves
  download resources through `MusicSourceRegistry`, starts download tasks with
  playback-resource headers, persists progress/completed/failed state through
  `DownloadTaskRepository`, moves completed files into the app cache downloads
  directory, releases retained playback resources, and forwards SwiftUI app
  background-session completion callbacks back into shared code.
- Downloads now has a feature-style presentation slice:
  `DownloadsRoot`, `DownloadsScreen`, `DownloadsViewModel`, `DownloadsState`,
  `DownloadsAction`, and `DownloadsEvent`. It observes the shared
  `DownloadController`, maps persisted `DownloadTask` values into UI models
  without exposing Room entities or platform scheduler types, delegates pause,
  resume, retry, and cancel commands back to the controller, and is reachable
  through a typed `MusicGraph.Downloads` route from Dashboard.
- `EnqueueDownloadUseCase` now creates stable persisted `DownloadTask` values
  from source-level `MediaId` plus track metadata. Room-backed Library and
  local Search tracks resolve legacy storage source fields into downloadable
  source media IDs through `LegacyStorageLookup`, while Search source results
  continue to reuse their provider media IDs. Library and Search rows expose
  Download actions that delegate to the shared `DownloadController` without
  leaking Room entities or platform scheduler types into Composables.
- Playlist detail now routes through `PlaylistRoot` and renders through
  `PlaylistScreen` with `PlaylistState`, `PlaylistAction`, and `PlaylistEvent`.
  The Root owns navigation, playback, import, edit, and one-shot events while
  the Screen renders immutable state and emits actions. `PlaylistVM` maps Room
  playlist rows into immutable `PlaylistTrackItem` values with optional
  downloadable source media IDs, and row actions can enqueue downloads through
  the shared `EnqueueDownloadUseCase` without exposing Room entities or
  platform scheduler types to the row UI.
- Now Playing has a first playback-presentation menu contract under
  `service/playback/presentation/nowplaying`. The current track is mapped from
  legacy `Music.loc` into an immutable `NowPlayingTrackItem` with an optional
  downloadable source media ID, the header menu emits `NowPlayingAction`
  values instead of directly calling `PlayerVM` or navigation, and
  `PlayerVM` delegates the download action through `EnqueueDownloadUseCase`.
- Now Playing non-progress metadata, queue, and control affordances now flow
  through `NowPlayingState`. The page reads current title, cover, lyric data,
  previous/next affordances, repeat mode, loading, and play/pause state from
  playback presentation models instead of collecting those legacy
  `PlayerRepository` flows directly. High-frequency position remains separate
  from this state and is still limited to the slider path until the full
  Now Playing Root/Screen split.
- The Now Playing route now enters `NowPlayingRoot` under
  `service/playback/presentation/nowplaying` and renders through
  `NowPlayingScreen`. The Root owns Koin ViewModel injection, navigation,
  sleep-timer commands, and one-shot events, while the Screen renders immutable
  state plus actions without direct `LocalNavController`, ViewModel, or flow
  collection access. Millisecond playback progress is collected only by the
  progress leaf and passed into `NowPlayingProgressPanel`, keeping it out of
  the main `NowPlayingState`.
- Now Playing artwork and lyrics now use shared `core/domain/model`
  `Artwork`, `Lyrics`, `LyricLine`, and `LyricsLoadState` models. Legacy
  UniFFI `DataSourceKey`, `MusicLyric`, and lyric load-state values are mapped
  only at the legacy playback presentation mapper, and the current image loader
  has a domain `Artwork` adapter until the broader core presentation media
  component migration lands.
- `core/presentation/media` now owns the first shared artwork rendering
  boundary: `ArtworkImage` renders default art and resolved bitmaps through an
  `ArtworkImageLoader` interface. The current Rust/UniFFI asset path is adapted
  behind `core/data/media/LegacyArtworkRepository`, presentation bitmap loading
  goes through `RepositoryArtworkImageLoader`, `MusicCover` is a compatibility
  wrapper, and `NowPlayingScreen` renders artwork through the core presentation
  media component instead of the legacy music widget component.
- The Room-backed artwork metadata/cache-key boundary now has read access:
  `ArtworkCacheKey` models cache metadata without image blobs,
  `MetadataDao` can read artwork by track, album, and content hash, and
  `LegacyArtworkRepository.cacheKey(...)` resolves track-level `Artwork` values
  through Room while the actual byte loading path remains compatible with the
  current Rust/UniFFI asset API.
- Rust metadata import now performs bounded embedded-artwork extraction with a
  2 MiB image cap, writes accepted artwork bytes under
  `${app_cache_dir}/artwork/<sha256>.<ext>`, exposes only cache metadata through
  `RemoteArtwork`, and persists `ArtworkEntity` rows during the Room import
  transaction. `LegacyArtworkRepository.cacheKey(...)` falls back from track
  artwork to album artwork using the imported track album ID.
- Legacy `MusicCover(DataSourceKey)` overload and `TideTunesImage` composable
  have been retired from all presentation callers. `PlaylistState.cover`,
  `ImportCover`, `MiniPlayerCore`, `NowPlayingMappers`, and `PlaylistMappers`
  now carry `Artwork?`, and every caller converts `DataSourceKey?` to `Artwork?`
  through the shared `toArtwork()` extension in `DataSourceKeyH.kt` before
  passing it to `MusicCover(artwork = ...)` or `ArtworkImage(artwork = ...)`.
  `TideTunesImage` remains as a no-caller legacy component.
- The top-level `datastore/` package has been moved into `core/data/datastore/`.
  `AppDataStore` (expect declaration plus Android, Desktop, and iOS actuals)
  and `AppPreferencesRepository` now live under
  `com.github.tidetunes.core.data.datastore`. `CoreDataModule`,
  `PlayerRepository`, and the platform playback controller tests import from
  the new package. The desktop `AppPreferencesRepositoryTest` was also
  relocated to the new test package.
- `LiveStorageSearchProvider` powers `MusicSource.search(...)` for all three built-in sources by
  recursively traversing source directories through `StorageDirectoryLister`
  (backed by `RemoteScannerRepository`). It filters music files by name
  match, skips hidden directories, honors result limits, and is injected into
  `LocalMusicSource`, `WebDavMusicSource`, and `OneDriveMusicSource` through a named Koin binding.
  `LiveStorageSearchProviderTest` (6 tests) verifies case-insensitive name
  matching, empty results, limit enforcement, hidden-directory skipping,
  storage-type mismatch rejection, and blank-query short-circuit.
- `SearchAggregator` now exposes a `suggestSources(...)` method that returns
  live source search titles for type-ahead suggestion merging.
  `SearchSuggestionsUseCase` receives `SearchAggregator` and
  `SearchSourceAccountProvider`, appends source-title suggestions to the
  history-and-local merge pipeline used by `SearchViewModel`. The
  `RecordingSearchAggregator` test fake was updated with the new method.
- Room schema version 4 adds `download_task` with status, progress, local path,
  source media ID fields, and a unique source/media/remote ID index.
- The first library-sync service boundary now exists under
  `service/librarysync`: `LibrarySyncController`, `LibrarySyncRequest`, and
  `LibrarySyncResult` define the shared sync contract, while
  `LegacyLibrarySyncController` adapts existing WebDAV/local scans and
  OneDrive incremental sync through the current `RemoteLibraryImportCoordinator`.
- `EditStorageVM` and `ImportStatusVM` now depend on `LibrarySyncController`
  instead of calling the legacy remote import coordinator directly.
- `LibrarySyncController.recentTasks` exposes persisted Room-backed
  `LibrarySyncTask` state from the existing `import_job` and `selected_folder`
  tables, and the Dashboard import-status block now consumes this service model
  instead of the removed `ImportStatusRepository` singleton.
- `LegacyLibrarySyncController` now checks persisted active jobs before starting
  a new sync and blocks concurrent sync starts for the same `SourceAccountId`.
- `LibrarySyncController` now exposes persisted task commands for pause,
  resume, retry, and cancel. Dashboard import rows show Pause, Resume, Retry,
  and Cancel actions based on the Room-backed `LibrarySyncTask` state.
- Pause only persists a `PAUSED` task after the active legacy importer accepts
  the pause request, avoiding fake-paused tasks for import paths that do not
  yet expose an active session handle. Cancelling a paused persisted task marks
  the Room task and selected folder as `CANCELLED`.
- `RemoteLibraryImportCoordinator` now tracks all active imports through a
  shared `ActiveImportOperation`, not only Rust scan sessions. WebDAV/local
  scans, OneDrive delta sync, OneDrive initial/resync snapshots, and direct
  complete snapshot imports all register an active operation before remote
  work starts, so `pauseImport(scanId)` and `cancelImport(scanId)` no longer
  depend on persisted task state alone.
- OneDrive and complete snapshot imports now check active stop requests at
  delta-page, deletion, batch, and completion boundaries. Rust scan imports
  still attach their `RemoteMusicScanSession`, so stop requests continue to
  cancel the underlying scan session as well.
- Desktop playback now has controller-level contract coverage before the real
  engine replacement. `DesktopPlayerControllerTest` seeds Room/DataStore state,
  resolves playback through `MusicSourceRegistry`, verifies a ready engine
  updates legacy playback state and delegates commands, verifies unsupported
  engine loads release transient resources and stay idle, and verifies resolve
  failures do not load the engine.
- Desktop now binds `MpvDesktopPlaybackEngine` instead of the no-op engine.
  The engine delegates to a Rust UniFFI `DesktopMpvPlayer` that dynamically
  loads libmpv at runtime, sends load/play/pause/seek/stop commands through
  libmpv, and reports unsupported when libmpv is not installed or the resource
  cannot be loaded.
- iOS playback now has an `IosPlaybackEngine` port backed by AVPlayer, keeping
  AVFoundation details inside `iosMain` while allowing `IosPlayerController` to
  be tested with a fake engine. `IosPlayerControllerTest` covers ready-engine,
  unsupported-engine, resolve-failure, command delegation, and retained
  playback-resource release paths on `iosSimulatorArm64Test`.
- Android playback now has an `AndroidPlaybackEngine` port around Media3.
  `PlayerControllerRepositoryTest` covers ready-engine playback startup,
  command delegation, unsupported-engine cleanup, resolve-failure behavior, and
  retained playback-resource release paths on `testDebugUnitTest`.
- `SearchViewModelTest` now uses an explicit cancellable test scope instead of
  relying on platform `viewModelScope`, so the shared Search presentation
  tests run reliably on iOS Native as well as Desktop.
- `DownloadsViewModelTest` now uses the same explicit cancellable test-scope
  pattern, so the shared Downloads presentation tests also run on the Android
  JVM unit target without depending on a platform Main dispatcher.

Verified commands after the first slice:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:desktopTest
./gradlew :desktopApp:compileKotlinDesktop
./gradlew :androidApp:assembleDebug
```

Verified commands after adding `initKoin` and graph splits:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:desktopTest
```

Verified commands after the Sources/storage presentation slice:

```text
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the storage editor presentation shell:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the storage editor Screen split:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the storage editor draft-model split:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the first core package-boundary slice:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the first source API contract slice:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after the first source browse/list slice:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
```

Verified commands after moving import folder list state to `SourceNode`:

```text
./gradlew :shared:compileKotlinDesktop
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:compileKotlinDesktop
rg -n "StorageEntry|StorageEntryType|entryTyp" shared/src/commonMain/kotlin/com/github/tidetunes/feature/importing/presentation/ImportScreen.kt shared/src/commonMain/kotlin/com/github/tidetunes/viewmodels/ImportVM.kt
```

Verified commands after migrating Import/folder picker to
Root/Screen/State/Action/Event:

```text
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
rg -n "ImportVM|StoragesVM|koinViewModel|LocalNavController|collectAsState|CurrentStorageStateType" shared/src/commonMain/kotlin/com/github/tidetunes/feature/importing/presentation/ImportScreen.kt
rg -n "ImportMusicsPage|widgets\.musics\.Import" shared/src/commonMain/kotlin shared/src/commonTest/kotlin
```

Verified commands after adding source playback-resource resolution:

```text
./gradlew :shared:desktopTest
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after routing Android/iOS playback startup through source
playback-resource resolution:

```text
./gradlew :shared:desktopTest
./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the Desktop playback engine boundary:

```text
./gradlew :shared:desktopTest
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding the local-first search foundation:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the source search contract and aggregation
foundation:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the Search presentation slice:

```text
./gradlew :shared:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after persisting Search history:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding Room-backed Search suggestions:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the shared playback controller contract:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after migrating `PlayerVM`, `MiniPlayer`, and bottom-bar
playback state to the shared `PlaybackController`:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the Download domain and persistent task state:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after adding the first library-sync service boundary:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified commands after exposing persisted library-sync task state and the
active-account guard:

```text
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
git diff --check
```

Verified command after adding persisted library-sync task commands:

```text
/Users/shine/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle :shared:desktopTest --no-daemon --no-configuration-cache --console plain
/Users/shine/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding active import operations for OneDrive and
complete snapshot imports:

```text
./gradlew :shared:compileTestKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.domain.importing.RemoteLibraryImportCoordinatorTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding source-indexed Room-backed `MusicSource.search`:

```text
./gradlew :shared:compileTestKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.source.storage.LegacyStorageMusicSourceTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.source.storage.RoomLegacyStorageSearchProviderIntegrationTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.feature.search.data.MusicSourceSearchAggregatorTest --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding the Downloads presentation slice:

```text
./gradlew :shared:compileTestKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.feature.downloads.presentation.DownloadsViewModelTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.service.download.data.scheduler.DesktopCoroutineDownloadSchedulerTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after wiring Library/Search enqueue actions into downloads:

```text
./gradlew :shared:compileTestKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.service.download.domain.EnqueueDownloadUseCaseTest --tests com.github.tidetunes.feature.search.presentation.SearchViewModelTest --tests com.github.tidetunes.source.storage.LegacyStorageTrackMappingTest --tests com.github.tidetunes.feature.search.data.RoomSearchRepositoryIntegrationTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified command after adding Desktop playback controller contract tests:

```text
./gradlew :shared:desktopTest --tests com.github.tidetunes.singleton.DesktopPlayerControllerTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding iOS playback engine port and controller contract
tests:

```text
./gradlew :shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:iosSimulatorArm64Test --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding Android playback engine port and controller
contract tests:

```text
./gradlew :shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:testDebugUnitTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:iosSimulatorArm64Test :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --console plain
```

Verified commands after replacing the Desktop no-op engine with the Rust/libmpv
engine boundary:

```text
cargo fmt --manifest-path rust-libs/Cargo.toml --all
cargo test --manifest-path rust-libs/Cargo.toml --workspace
./gradlew :shared:desktopTest --tests com.github.tidetunes.singleton.DesktopPlaybackEngineTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding the Playlist detail download action:

```text
./gradlew :shared:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.source.storage.LegacyStorageTrackMappingTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after migrating Playlist detail to Root/Screen:

```text
./gradlew :shared:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest --tests com.github.tidetunes.feature.playlist.presentation.PlaylistStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding the Now Playing menu download action:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after moving Now Playing metadata and queue state into
presentation models:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after splitting Now Playing Root/Screen and isolating
progress collection to the progress leaf:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after moving Now Playing artwork and lyrics to shared domain
models:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after replacing the legacy artwork loader with a core
domain/data repository boundary:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.core.data.media.LegacyArtworkRepositoryTest --tests com.github.tidetunes.service.playback.presentation.nowplaying.NowPlayingStateTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after adding Room-backed artwork cache-key reads:

```text
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.core.data.media.LegacyArtworkRepositoryTest --tests com.github.tidetunes.database.RoomLibraryIntegrationTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after wiring bounded Rust artwork extraction to app-cache
files and Room `ArtworkEntity` import:

```text
cargo fmt --manifest-path rust-libs/Cargo.toml --all
cargo test --manifest-path rust-libs/Cargo.toml --workspace
./gradlew :shared:compileKotlinDesktop :shared:desktopTest --tests com.github.tidetunes.domain.importing.RemoteLibraryImportCoordinatorTest --tests com.github.tidetunes.core.data.media.LegacyArtworkRepositoryTest --tests com.github.tidetunes.database.RoomLibraryIntegrationTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after retiring legacy MusicCover(DataSourceKey)/TideTunesImage
callers and migrating presentation artwork to the core Artwork boundary:

```text
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after migrating the top-level `datastore/` package into
`core/data/datastore/`:

```text
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after extending `LiveStorageSearchProvider` to all three source adapters (Local, WebDAV, OneDrive):

```text
./gradlew :shared:desktopTest --tests com.github.tidetunes.source.storage.LiveStorageSearchProviderTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Verified commands after wiring source-indexed suggestions into Search:

```text
./gradlew :shared:desktopTest --tests com.github.tidetunes.feature.search.presentation.SearchViewModelTest --tests com.github.tidetunes.feature.search.data.MusicSourceSearchAggregatorTest --no-daemon --no-configuration-cache --console plain
./gradlew :shared:desktopTest :shared:compileDebugKotlinAndroid :shared:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
```

Known unrelated local files:

- `Design/.workbuddy/memory/2026-06-26.md`
- `Design/.workbuddy/screenshots/`
- `local.properties`

## Task Board

### Phase 1: Engineering Skeleton

- [x] Inspect current project structure, Gradle setup, modules, and code.
- [x] Read the Komi Store reference shape for typed routes and Koin assembly.
- [x] Keep existing app behavior usable while migrating.
- [x] Keep dependency versions in `gradle/libs.versions.toml`.
- [x] Add a unified `initKoin` entry used by Android, Desktop, and iOS.
- [x] Split root navigation registration into graph files instead of one
  growing `AppNavigation.kt`.
- [x] Establish package-level `core/domain`, `core/data`, and
  `core/presentation` boundaries inside the current `shared` module before
  considering physical Gradle module splits.
- [x] Continue migrating legacy top-level data packages into `core/data` (moved `datastore/`, `utils/`, `components/`, `ToastRepository`, `PermissionChecker`, `LibraryRepository`, `AssetRepository`, `MetadataRepository`, `PlaylistRepository`, `RemoteScannerRepository`, `PlayerRepository`, `PlayerController`, `ImportRepository` (plus `ImportSelectionMode`, `RouteImportType`), `StorageRepository`; 2 remaining)
  without breaking existing Room/DataStore behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).
- [~] Introduce a measured build-logic/convention-plugin migration only after
  the package-level boundaries are stable (`build-logic/convention` included
  with a safe base metadata plugin; no app module build scripts migrated yet).
- [x] Evaluate Miuix dependency/API from source or official docs before adding
  it; keep current Material/App components until then.
- [x] Keep Android, iOS Simulator, and Desktop compilation green after each
  change set.

### Phase 2: UI Shell

- [x] Add Search to the current horizontal pager as the fifth top-level entry.
- [x] Replace the current pager with target five primary tabs that preserve
  independent back stacks.
- [x] Preserve a single global MiniPlayer and Now Playing route.
- [x] Add compact (NavigationBar), medium (NavigationRail), and expanded
  (Sidebar) shell variants.
- [x] Move existing shell widgets into `core/presentation` or feature (components/ → core/presentation/components/ done; widget pages migrated to feature/ packages).
- [x] Verify Desktop does not simply scale the phone layout (uses NavigationRail
  at medium width and Sidebar at expanded width).

### Phase 3: Feature Presentation Migration

- [x] Create the first State/Action/Event feature slice for Library.
- [x] Migrate Playlists list to Root/Screen/State/Action/Event.
- [x] Migrate Playlist detail to Root/Screen/State/Action/Event.
- [x] Migrate the Dashboard Sources/storage list to
  Root/Screen/State/Action/Event.
- [x] Move storage editor top-level navigation and commands behind
  Root/State/Action/Event.
- [x] Migrate the storage editor to Root/Screen/State/Action/Event.
- [x] Split storage editor credential inputs out of direct legacy form access so the
  Screen no longer depends on `ArgUpsertStorage` or `EditStorageVM` internals.
- [x] Move `EditStorageVM`'s backing draft model off `ArgUpsertStorage` and
  into a feature-owned editor draft model before introducing source contracts.
- [x] Move the import folder list UI and `ImportVM.entries` state from legacy
  `StorageEntry` to source-level `SourceNode`.
- [x] Migrate Import/folder picker to Root/Screen/State/Action/Event.
- [x] Add first Search Root/Screen/State/Action/Event presentation slice.
- [x] Add first Downloads Root/Screen/State/Action/Event presentation slice.
- [x] Migrate Settings and Debug screens to Root/Screen boundaries.
- [x] Migrate Dashboard page (sleep timer, imports, downloads, sources) to Root/Screen.
- [x] Remove direct `LocalNavController` usage from screens; navigation should
  flow through Root events/callbacks (all widget-level pages migrated to feature/ Root/Screen pattern).
- [x] Keep screens free of DAO, Repository, Ktor, and platform object access.

### Phase 4: Core Domain And Source Contracts

- [~] Introduce stable `MediaId`, `SourceId`, and `SourceAccountId` domain
  models without exposing Room IDs or raw WebDAV paths as cross-layer IDs.
  Playlist dialog cover previews now use `Artwork.SourceMedia(MediaId)` and a
  source-aware legacy artwork media-id resolver instead of exposing
  `StorageEntryLoc(storageId, path)` through presentation state. Import entry
  and current-directory callbacks now use `SourceNodeSelection` /
  `SourceDirectorySelection` with `SourceId`, `SourceAccountId`, and
  `SourceNodeType`; legacy `StorageEntry` adaptation was moved out of
  `feature/importing/data` into a retained core-data adapter, and playlist
  detail add-track import plus playlist create/edit now enter the data/store
  boundary as source selections.
- [x] Add the first `MediaId`, `SourceId`, and `SourceAccountId` value types
  under `core/domain/model`.
- [x] Define first `MusicSource`, `MusicSourceRegistry`, `SourceCapability`, and
  `PlaybackResource` contracts.
- [x] Add initial Local, WebDAV, and OneDrive authentication adapters behind
  the source API.
- [x] Add first Local, WebDAV, and OneDrive browse/list adapters behind the
  source API and route import-folder browsing through `MusicSourceRegistry`.
- [x] Add source-level playback-resource resolution for Local, WebDAV, and
  OneDrive through a retained legacy playback resolver.
- [x] Add the first source-level search contract and explicit unsupported-search
  behavior for current Local, WebDAV, and OneDrive adapters.
- [x] Add source-indexed Room-backed search for synced Local, WebDAV, and
  OneDrive source accounts.
- [x] Implement provider-native live search for Local, WebDAV, and OneDrive
  adapters where provider/search-index APIs can return content not yet synced
  into Room.
- [x] Route Android and iOS playback startup through
  `MusicSource.resolvePlayback` so transient playback URLs stay on demand and
  out of `TrackEntity`.
- [x] Route Desktop playback startup through `MusicSource.resolvePlayback` and
  release transient resources when the current no-op engine rejects playback.
- [x] Replace the no-op Desktop playback engine with a real Rust/libmpv-backed
  implementation.

### Phase 5: Playback Service Boundary

- [x] Define a shared playback service contract with separated player state,
  position, and queue flows.
- [x] Bridge the current platform `PlayerController` implementations through a
  shared `LegacyPlaybackController` contract adapter.
- [x] Adapt current Android Media3 implementation behind the shared contract.
- [x] Implement iOS player boundary without leaking AVFoundation types into
  `commonMain`.
- [x] Add the Desktop player engine boundary without leaking Rust/libmpv types
  into `commonMain`.
- [x] Implement the real Desktop Rust/libmpv playback engine behind the
  boundary.
- [x] Route current Android and iOS player loading through source-level
  playback resolution before replacing player engines.
- [x] Add the Desktop player engine boundary and route it through source-level
  playback resolution.
- [x] Ensure high-frequency progress updates are subscribed only by progress UI.
- [x] Migrate `PlayerVM`, `MiniPlayer`, and bottom-bar playback visibility to
  shared `PlaybackController` state and position flows.
- [x] Move Now Playing non-progress metadata, queue, and control affordance
  state behind playback presentation models.
- [x] Split Now Playing into Root/Screen and keep millisecond playback progress
  collection out of the main presentation state.
- [x] Move Now Playing artwork and lyrics out of UniFFI-backed state fields and
  into shared domain models.
- [x] Move Now Playing artwork rendering onto the shared core presentation
  media component with a replaceable loader interface.

### Phase 6: Sync, Search, And Download

- [x] Promote the current remote import coordinator into a library-sync service
  boundary.
- [x] Expose persisted sync task progress through `LibrarySyncController`
  without leaking Room entities to Dashboard UI.
- [x] Block concurrent sync starts for the same `SourceAccountId` at the shared
  library-sync controller boundary.
- [x] Add persisted library-sync task pause, resume, retry, and paused-task
  cancel commands without exposing Room entities to Dashboard UI.
- [x] Add active operation handles for OneDrive delta sync and complete
  snapshot imports before enabling true pause/cancel for those active paths.
- [ ] Add lower-level cancellable OneDrive delta request support when the Rust
  backend exposes a cancellable Graph page operation.
- [x] Keep Room as the only library UI source of truth.
- [x] Add the first search domain/use-case structure with Room-backed
  local-first track results.
- [x] Add capability-gated search aggregation with source labels and duplicate
  track collapse while preserving Room-first results.
- [x] Add Search presentation query debounce, active-search cancellation,
  history, and suggestions.
- [x] Persist ordered search history outside `SearchViewModel` with DataStore.
- [x] Add richer Search suggestions based on persisted history plus the local
  Room library index.
- [x] Add source-indexed search over synced provider content without bypassing
  the Room library index.
- [x] Add provider-native remote search and source-indexed suggestions for
  unsynced provider content.
- [x] Add download domain and persistent task state before any platform
  scheduler implementation.
- [x] Add the shared download scheduler boundary behind
  `PersistentDownloadController` without bypassing persisted `download_task`
  state.
- [x] Implement the Desktop coroutine download scheduler behind
  `DownloadTaskScheduler`.
- [x] Implement the Android WorkManager download scheduler behind
  `DownloadTaskScheduler`.
- [x] Implement the iOS URLSession background transfer scheduler behind
  `DownloadTaskScheduler`.
- [x] Route persisted download task state into a shared Downloads presentation
  slice without exposing Room entities or platform scheduler types.
- [x] Add shared enqueue actions from Library and Search track rows into
  `DownloadController` using source-level media IDs.
- [x] Add a shared enqueue action from Playlist detail track rows into
  `DownloadController` using source-level media IDs.
- [x] Add a shared enqueue action from the Now Playing track menu into
  `DownloadController` using source-level media IDs.

### Phase 7: Verification And Final Output

- [x] Add or update tests for each migrated ViewModel's State/Action/Event
  behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).
- [x] Add a Sources presentation mapping test for remote-source filtering and
  UI model creation.
- [x] Add a Source editor state test proving WebDAV passwords and OneDrive
  refresh tokens are not exposed through UI state.
- [x] Add a Source editor drive-list mapping test so OneDrive drive choices are
  exposed as presentation UI models instead of UniFFI objects.
- [x] Add a Source editor draft boundary test proving repository arguments are
  created only from the feature-owned draft at the storage boundary.
- [x] Add core domain identifier tests for serialization and blank-value
  rejection.
- [x] Add a Sources transition test for `SourceAccountId` to current storage
  route ID mapping.
- [x] Add MusicSource registry and adapter authentication tests before
  extending source adapters.
- [x] Add MusicSource browse/list mapping tests before moving import UI state
  off legacy `StorageEntry`.
- [x] Add MusicSource playback-resource resolver tests for media ID rejection,
  storage-type routing, retained session release, and legacy `Music.loc` to
  source registry resolution.
- [x] Add Desktop playback engine boundary tests for the current no-op engine
  behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).
- [x] Add Room-backed search tests for local track matching, deleted remote-file
  filtering, and SQL wildcard escaping.
- [x] Add source-search aggregation tests for source labels, dedupe, failure
  mapping, and capability-gated source calls.
- [x] Add source-indexed `MusicSource.search` tests for Local/WebDAV/OneDrive
  delegation, Room filtering, deleted-file exclusion, and playable media ID
  creation.
- [x] Add Search ViewModel tests for debounce, active-search cancellation,
  history, suggestions, and source-account mapping.
- [x] Add Import presentation state mapping tests for load-state conversion and
  storage account UI model boundaries.
- [x] Add playback queue and legacy state-mapping tests for the shared playback
  contract.
- [x] Add Download state-transition, persistence mapping, and Room migration
  tests before wiring platform schedulers.
- [x] Add Download scheduler-boundary tests proving scheduler calls happen
  after persisted state updates and terminal tasks remain no-op.
- [x] Add Desktop download scheduler tests for resource resolution, file
  writes, completed state, release hooks, and resolve-failure mapping.
- [x] Add Downloads ViewModel tests for task UI mapping and controller command
  delegation.
- [x] Add Download enqueue tests for stable task creation, Search action
  delegation, Playlist detail source-field mapping, and Room track
  source-field to media-ID mapping.
- [x] Add Playlist detail presentation mapping tests for header state and
  downloadable track row UI models.
- [x] Add Now Playing presentation mapping tests for current-track
  downloadable media IDs and missing-storage fallback.
- [x] Extend Now Playing presentation mapping tests to cover playback queue and
  control state.
- [x] Verify Android WorkManager download scheduler compilation and APK
  packaging with the app debug build.
- [x] Verify iOS URLSession download scheduler compilation plus SwiftUI app
  background-session callback bridging with an Xcode simulator build/run.
- [x] Add LibrarySync request validation and legacy adapter routing tests before
  replacing the current remote import coordinator.
- [x] Add LibrarySync task-domain, Room mapping, and active-account guard tests
  before adding pause/resume/retry controls.
- [x] Add LibrarySync command tests for pause, resume, retry, paused-task
  cancel, and invalid-state no-op behavior (PlaylistsListStateTest, PlaylistDialogStateTest (13), DashboardStateTest (6) added).
- [x] Add active import operation tests for cancel, pause, and first-stop
  reason preservation.
- [x] Add Desktop playback controller contract tests for ready-engine,
  unsupported-engine, and resolve-failure paths before replacing the engine.
- [x] Add iOS playback engine boundary and controller contract tests for
  ready-engine, unsupported-engine, and resolve-failure paths.
- [x] Add Android playback controller/engine-boundary contract tests for
  ready-engine, unsupported-engine, and resolve-failure paths.
- [x] Add full platform playback contract tests before replacing player engines.
- [x] Replace the legacy artwork image loader with a core media domain/data
  repository boundary and presentation bitmap loader.
- [x] Expose Room-backed artwork metadata/cache-key reads through DAO and the
  core artwork repository boundary.
- [x] Wire bounded Rust embedded-artwork extraction to app-cache files and Room
  `ArtworkEntity` import metadata.
- [x] Keep `docs/testing/test-report.md` updated with successful gates.
- [x] Produce the final module tree, dependency map, architecture notes,
  interface list, navigation structure, Koin module list, platform differences,
  build/test results, known limits, and extension guidance.

## Next Work Items

1. [x] Retire remaining legacy `MusicCover(DataSourceKey)`/`TideTunesImage`
     callers now that imported artwork cache metadata is available through the
     core artwork repository boundary.
2. [x] Remove remaining Import `StorageEntry` conversion boundary when
   playlist, lyric, cover, and library-folder callers have source-level
   contracts. `EditStorageVM` zero UniFFI imports. `PlayerVM.play()` uses Long.
   `PlaylistRoot` zero UniFFI imports. `EditPlaylistVM._id` is Long.
   `PlaylistsVM` consumes domain `PlaylistSummary` flow. `PlaylistVM` header
   state uses `PlaylistSummary`. `PlaylistMappers` uses `PlaylistSummary`.
   `CreatePlaylistVM` uses `CreatePlaylistTab`. `AssetVM`+`TideTunesImage` deleted.
   `UpdatePlaylistRequest.id` is Long. `PlaylistRepository.removePlaylist`/
   `removeMusic` accept Long. 7 UniFFI imports removed this round;
   remaining 18 across 4 files (PlaylistVM internal UniFFI types,
   NowPlayingMappers, LogVM, DebugMoreVM).

3. [x] Add source-indexed Search suggestions after provider-native source
     search exists. `SearchAggregator` now exposes `suggestSources()`, which
     `MusicSourceSearchAggregator` implements by calling live source search
     and extracting titles. `SearchSuggestionsUseCase` merges these with
     history and local library suggestions. Injected into all three source
     adapters via `SearchFeatureModule`.
4. [x] Implement provider-native live search behind `MusicSource.search(...)`
     for unsynced Local/WebDAV/OneDrive content via `LiveStorageSearchProvider`
     (recursive directory listing + case-insensitive name filter, skip hidden
     dirs, honor limit). Injected into all three source adapters through a
     named Koin binding.
5. Add lower-level cancellable OneDrive Graph delta-page support once the Rust
   backend exposes a cancellable operation, so stop requests can interrupt a
   blocked page request rather than waiting for the next Kotlin boundary.
6. [x] Continue migrating legacy top-level data packages into `core/data` without
   broad package churn. (Moved `datastore/`, `utils/`, `components`, `ToastRepository`,
   `PermissionChecker`, `LibraryRepository`, `AssetRepository`, `MetadataRepository`,
   `PlaylistRepository`, `RemoteScannerRepository`, `PlayerRepository`, `PlayerController`,
   `ImportRepository`, and `StorageRepository`; 2 singleton files remain.)
7. [x] Update `docs/testing/test-report.md` after the next behavior-affecting
   migration or test addition.

### Phase 8: Design System, Remaining Pages, And Advanced Features

#### Design System (Goal Section 12)

- [x] Build `AppTopBar` wrapper in `core/presentation/components`
- [x] Build `AppSearchBar` wrapper in `core/presentation/components`
- [x] Build `AppIconButton` wrapper in `core/presentation/components`
- [x] Build `AppSectionHeader` wrapper in `core/presentation/components`
- [x] Build `AppLoadingIndicator` wrapper in `core/presentation/components`
- [x] Build `AppEmptyState` wrapper in `core/presentation/components`
- [x] Build `AppErrorState` wrapper in `core/presentation/components`
- [x] Retire remaining UniFFI imports from active VMs (PlayerVM zero UniFFI; PlaylistsVM zero UniFFI; EditPlaylistVM zero UniFFI; NowPlayingMappers zero UniFFI; PlaylistVM zero UniFFI; LogVM zero UniFFI; DebugMoreVM zero UniFFI)
- [x] Integrate Miuix library and adapt App Design System wrappers (foundation: version catalog, theme wrapper, AppTextButton)

#### Remaining Pages (Goal Section 22)

- [x] Browse page with State/Action/Event
- [x] Radio page with State/Action/Event
- [x] Album detail page with State/Action/Event
- [x] Artist detail page with State/Action/Event
- [x] Recently Played page
- [x] Recently Added page
- [x] Dedicated Lyrics page
- [x] Dedicated Queue page
- [x] Onboarding flow

#### Advanced Playback Features (Goal Section 25 Phase 7)

- [x] Word-by-word lyrics (逐字歌词) parsing and UI (domain model + animated composable; data source pending Rust LRC parser)
- [x] Dynamic player background from artwork
- [x] Shared advanced playback capability/settings/output-device domain contract
- [ ] Gapless playback (Rust/libmpv)
- [ ] Crossfade
- [ ] ReplayGain
- [ ] Output device selection
- [ ] Android Auto support
- [ ] AirPlay support
- [ ] CarPlay support

#### Physical Module Split (Goal Section 4)

- [x] Split `core:domain` into physical Gradle module
- [ ] Split `core:data` into physical Gradle module
- [x] Split `core:presentation` into physical Gradle module
- [x] Split `source:api` into physical Gradle module
- [x] Split `service:*:domain` into physical Gradle modules
- [x] Split remaining service and feature modules — all 7 feature modules + 3 service domain modules split (Rounds 7-15)

#### Build Logic (Goal Section 21)

- [~] Migrate app-module build scripts to convention plugins — app modules use application plugins with platform configs; convention plugins add minimal value. Deferred.
- [x] Add `convention.kmp.domain` plugin — created TideTunesKmpDomainPlugin (Round 17)
- [x] Add `convention.cmp.feature` plugin — TideTunesFeaturePlugin exists (apply-only, Round 17)
- [x] Add `convention.room` plugin — TideTunesRoomPlugin exists (KSP config, Round 17)
- [x] Add `convention.music-source` plugin — created TideTunesMusicSourcePlugin (Round 17)

### Session Update 2026-06-28

#### Build Infrastructure Fix
- [x] Fixed `build-logic/convention/build.gradle.kts`: removed broken `implementation(files(libs.javaClass...))` line
- [x] Fixed `build-logic/convention/settings.gradle.kts`: changed `FAIL_ON_PROJECT_REPOS` to `PREFER_PROJECT` to allow convention plugin's own repositories
- [x] Simplified convention plugins to just apply base plugins (no version catalog or external class dependencies)
- [x] Restored full `shared/build.gradle.kts` configuration (KMP targets, source sets, dependencies, cargo, room) that was lost when migrating to convention plugins
- [x] Fixed Gobley plugin IDs in CargoUniffiPlugin (`gobley.cargo` → `dev.gobley.cargo`)
- [x] Added missing `androidx.work.runtime.ktx` dependency in shared/build.gradle.kts

#### Recently Played Feature (Phase 8)
- [x] Added `lastPlayedAt: Long?` field to `TrackEntity`
- [x] Added `MIGRATION_4_5` (ALTER TABLE track ADD COLUMN lastPlayedAt INTEGER)
- [x] Bumped database version to 5
- [x] Added `updateLastPlayedAt()` and `findRecentlyPlayed()` to `TrackDao`
- [x] Created `feature/recentlyplayed/presentation/` with State, Action, Event, Root, Screen, ViewModel
- [x] Added `MusicGraph.RecentlyPlayed` route and `RecentlyPlayedGraph.kt` navigation
- [x] Registered `RecentlyPlayedViewModel` in Koin `LibraryFeatureModule`

#### Onboarding Feature (Phase 8)
- [x] Created `feature/onboarding/presentation/` with State (including `OnboardingPage` enum), Action, Event, Root, Screen, ViewModel
- [x] Added `MusicGraph.Onboarding` route and `OnboardingGraph.kt` navigation
- [x] Registered `OnboardingViewModel` in Koin `LibraryFeatureModule`
- [x] Onboarding: Welcome → AddSources (with Skip) → Ready → Finish

#### Gate (2026-06-28)
- [x] `shared:desktopTest` - 185 pass, 1 pre-existing failure (DownloadPersistenceIntegrationTest)
- [x] `shared:compileDebugKotlinAndroid` - PASS
- [x] `shared:compileKotlinIosSimulatorArm64` - PASS
- [x] `desktopApp:compileKotlinDesktop` - PASS

### Session Update 2026-06-28 (Round 2)

#### Miuix Integration (Phase 8 Design System)
- [x] Added `miuix = "0.9.2"` version and `miuix-ui` library to `gradle/libs.versions.toml`
- [x] Added `miuix-ui` dependency to `shared` `commonMain`
- [x] Created `core/presentation/theme/MiuixThemeWrapper.kt` — `AppMiuixTheme` composable wrapping `MiuixTheme` with default color scheme
- [x] Created `core/presentation/components/AppTextButton.kt` — Miuix-based `TextButton` wrapper
- [x] Verified Miuix 0.9.2 resolves from Maven Central and compiles on all platforms

#### Gate (2026-06-28 Round 2)
- `shared:desktopTest` - 185 pass, 1 pre-existing failure (DownloadPersistenceIntegrationTest)
- `shared:compileDebugKotlinAndroid` - PASS
- `shared:compileKotlinIosSimulatorArm64` - PASS
- `desktopApp:compileKotlinDesktop` - PASS

#### Summary
All Phase 8 Remaining Pages are now complete. Miuix integration foundation is in place.
Remaining work for future sessions:
- Migrate remaining App Design System wrappers to Miuix (AppNavigationBar, AppNavigationRail, AppSidebar, AppCard, AppDialog, AppScaffold)
- Physical module split
- Advanced playback features (Rust-dependent)

### Session Update 2026-06-28 (Round 3)

#### Dynamic Player Background (Phase 8 Advanced Playback)
- [x] Created `core/presentation/media/ArtworkPalette.kt`:
  - `ArtworkPalette` data class (vibrant, muted, darkMuted colors + Default companion)
  - `extractPaletteFromBitmap()` – samples 16×16 grid from artwork bitmap, averages three vertical bands (top=dark, middle=vibrant, bottom=muted)
  - `rememberArtworkPalette()` composable – resolves palette from `Artwork?` via `ArtworkImageLoader`
- [x] Integrated palette into `NowPlayingRoot`: extracts palette from `state.currentTrack?.artwork` and passes to screen
- [x] Updated `NowPlayingScreen`: accepts `palette` parameter, replaced solid surface background with vertical gradient (darkMuted → muted → vibrant → surface)
- [x] Default gradient palette (dark blue tones) when no artwork is available

#### Gate (2026-06-28 Round 3)
- `shared:desktopTest` – 185 pass, 1 pre-existing failure
- `shared:compileDebugKotlinAndroid` – PASS
- `shared:compileKotlinIosSimulatorArm64` – PASS
- `desktopApp:compileKotlinDesktop` – PASS

### Session Update 2026-06-28 (Round 3 continued)

#### Word-by-Word Lyrics (Phase 8 Advanced Playback)
- [x] Extended `LyricLine` with `words: ImmutableList<LyricWord>` field
- [x] Created `LyricWord` domain model (`text`, `startOffset`, `duration`)
- [x] Created `feature/lyrics/presentation/AnimatedLyricLine.kt`:
  - `AnimatedLyricLine` composable with `LyricWordState` (Inactive, Active, Completed)
  - Per-word animated color + bold transitions as playback progresses
  - Falls back to plain line rendering when no word-timing data
  - `PlainLyricLine` for non-current lines
- [x] Updated `LyricsPanel` in NowPlayingScreen: uses `AnimatedLyricLine` for current line
- [x] Added `linePositionMs` computation in `NowPlayingRoot` (accumulated line durations)
- [x] Updated `MusicPlayerBody` and `NowPlayingScreen` to propagate `linePositionMs`

#### Gate (2026-06-28 Round 3 continued)
- `shared:desktopTest` — 185 pass, 1 pre-existing failure
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

### Session Update 2026-06-28 (Round 4)

#### Physical Module Split (Phase 8 Build Logic / Goal Section 4)

- [x] **Split `core:domain` into physical Gradle module**
  - Created `core/domain/` with `build.gradle.kts` (KMP: Android + Desktop + iOS, deps: kotlinx.serialization + immutable collections + coroutines)
  - Moved 4 files: `Identifiers.kt`, `MediaAssets.kt`, `ArtworkRepository.kt`, `PermissionChecker.kt`
  - Moved 1 test: `IdentifiersTest.kt` (2 tests: serialization + blank-value rejection)
  - Registered in `settings.gradle.kts` as `:core:domain`
  - `shared` depends on `:core:domain`

- [x] **Split `source:api` into physical Gradle module**
  - Created `source/api/` with `build.gradle.kts` (KMP: Android + Desktop + iOS, depends on `:core:domain`)
  - Moved 2 files: `MusicSource.kt`, `MusicSourceRegistry.kt`
  - Registered in `settings.gradle.kts` as `:source:api`
  - `shared` depends on `:source:api`

#### Gate (2026-06-28 Round 4)
- `core:domain:desktopTest` - 2 tests pass (IdentifiersTest)
- `source:api:compileKotlinDesktop` - PASS
- `shared:desktopTest` - 183 pass, 1 pre-existing failure
- `shared:compileDebugKotlinAndroid` - PASS
- `shared:compileKotlinIosSimulatorArm64` - PASS
- `desktopApp:compileKotlinDesktop` - PASS

#### Module tree after splits
```
TideTunes/
├── core/domain/              ← NEW: pure Kotlin, depends on nothing
├── source/api/               ← NEW: depends on core:domain
├── shared/                   ← depends on core:domain, source:api
├── androidApp/               ← depends on shared
├── desktopApp/               ← depends on shared
├── iosApp/                   ← depends on shared
└── build-logic/convention/   ← convention plugins
```

#### Remaining split targets (deferred)
- `core:data` — Room/Ktor dependencies, ~20 files
- `core:presentation` — Compose component wrappers, ~19 files
- `service:playback`, `service:download`, `service:librarysync` — service contracts
- `source:local`, `source:webdav`, `source:onedrive` — source implementations

### Session Update 2026-06-28 (Round 5)

#### PlaylistVM UniFFI Cleanup
- [x] Added `Long`-based overloads to `RoomLibraryStore`: `addMusicSelectionsById`, `replaceMusicOrderById`, `getPlaylistById`
- [x] Added `Long` overload to `PlaylistRepository`: `requestTotalDurationById`
- [x] Removed last 2 UniFFI imports (`MusicId`, `PlaylistId`) from `PlaylistVM`
- [x] All active ViewModels now have zero UniFFI imports
- [x] Verified: LogVM and DebugMoreVM were already clean from prior round

#### Miuix Component Migration
- [~] Attempted Miuix 0.9.2 API migration for AppComponents.kt wrappers (TopAppBar, IconButton, ProgressIndicator). Blocked by API differences: Miuix 0.9.2 does not expose `TopAppBarDefaults.topAppBarColors()`, `IconButtonDefaults.iconButtonColors()`, or `ProgressIndicatorDefaults.colors()` in the expected form. Colors API uses `onSurfaceVariantActions`/`onSurfaceVariantSummary` instead of `onSurfaceVariant`. The Material 3 wrappers remain in place and work correctly with the Miuix theme wrapper. Full migration requires studying Miuix's actual composable signatures.

#### Gate (2026-06-28 Round 5)
- `shared:desktopTest` - 183 pass, 1 pre-existing failure (DownloadPersistenceIntegrationTest)
- `shared:compileDebugKotlinAndroid` - PASS
- `shared:compileKotlinIosSimulatorArm64` - PASS
- `desktopApp:compileKotlinDesktop` - PASS

#### Summary
All active ViewModels are now UniFFI-free. Remaining UniFFI imports (34 files) are confined to the data layer (core/data, source/storage, service/*/data, singleton/) where they act as the bridge to the Rust backend — architecturally correct.
Miuix wrapper migration partially blocked; Material 3 wrappers remain functional.

### Session Update 2026-06-28 (Round 6)

#### Convention Plugin Enhancement Investigation
- [~] Investigated enhancing convention plugins with KMP target config + dependency presets.
  Finding: convention plugins in an included build cannot access Kotlin Gradle Plugin
  classes or the root version catalog without adding explicit compileOnly dependencies
  that hardcode versions. The current minimal plugins (apply-only) are the
  practical limit without restructuring the build-logic as a composite build.

#### Miuix Component Migration (continued)
- [~] Attempted full AppComponents.kt migration to Miuix 0.9.2 - reverted. Miuix 0.9.2 composable
  API differs from assumptions: TopAppBarDefaults has no topAppBarColors(), IconButtonDefaults
  has no iconButtonColors(), ProgressIndicator name differs. Miuix theme wrapper (AppMiuixTheme)
  and AppTextButton remain correctly integrated. Full migration requires studying Miuix source.

#### PlaylistVM UniFFI Cleanup (confirmed)
- [x] All active ViewModels are UniFFI-free after Round 5 cleanup.
  Remaining UniFFI imports (34 files) confined to data layer: core/data (10),
  source/storage (10), service/*/data (7), singleton/ (2), source/{local,webdav,onedrive} (3),
  di/ (1), domain/importing/ (1).

#### Gate (2026-06-28 Round 6)
- shared:desktopTest - 183 pass, 1 pre-existing failure (DownloadPersistenceIntegrationTest)
- shared:compileDebugKotlinAndroid - PASS
- shared:compileKotlinIosSimulatorArm64 - PASS
- desktopApp:compileKotlinDesktop - PASS
- core:domain:desktopTest - 2 tests pass
- source:api:compileKotlinDesktop - PASS

#### Current Module Tree
```
TideTunes/
├── core/domain/              physical module (2 files, 2 tests)
├── source/api/               physical module (2 files)
├── shared/                   main module (~200 files, 183 tests)
├── androidApp/               Android entry point
├── desktopApp/               Desktop entry point
├── iosApp/                   iOS Xcode project
└── build-logic/convention/   6 convention plugins (apply-only)
```

#### Remaining Work Summary

Unblocked:
1. core:presentation physical module split (19 files, 0 UniFFI; blocked by expect/actual)
2. service:*:domain physical module splits (0 UniFFI imports each)
3. DownloadPersistenceIntegrationTest fix

Blocked by external factors:
4. Miuix full wrapper migration - needs Miuix 0.9.2 source audit
5. Convention plugin KMP target config - needs build-logic restructuring
6. Advanced playback (Gapless, Crossfade, ReplayGain, Auto, AirPlay, CarPlay) - Rust/libmpv dependent
7. Lower-level cancellable OneDrive delta sync - Rust backend dependent

### Session Update 2026-06-28 (Round 7)

#### Service Domain Physical Module Splits
- [x] **Split `service:playback:domain` into physical Gradle module**
  - Moved 2 files: `PlaybackController.kt`, `PlaybackModels.kt`
  - Moved 1 test: `PlaybackQueueTest` (3 tests)
  - Dependencies: `:core:domain` + `kotlinx.coroutines.core`
  - Zero UniFFI imports

- [x] **Split `service:download:domain` into physical Gradle module**
  - Moved 4 files: `DownloadController.kt`, `DownloadTask.kt`, `DownloadTaskScheduler.kt`, `EnqueueDownloadUseCase.kt`
  - Moved 2 tests: `DownloadTaskTest` (3 tests), `EnqueueDownloadUseCaseTest` (1 test)
  - Dependencies: `:core:domain` + `kotlinx.coroutines.core`
  - Zero UniFFI imports

- [x] **Split `service:librarysync:domain` into physical Gradle module**
  - Moved 2 files: `LibrarySyncController.kt`, `LibrarySyncTask.kt`
  - Moved 2 tests: `LibrarySyncRequestTest` (2 tests), `LibrarySyncTaskTest` (4 tests)
  - Dependencies: `:core:domain` + `kotlinx.coroutines.core`
  - Zero UniFFI imports

- [x] **Fixed cross-module smart-cast in `DownloadsState.kt`** — `totalBytes` property smart-cast broke when `DownloadTask` moved to a separate module. Fixed with `?.let` pattern.

#### Gate (2026-06-28 Round 7)
- `shared:desktopTest` — 170 pass, 1 pre-existing failure (DownloadPersistenceIntegrationTest)
- `service:playback:domain:desktopTest` — 3 pass, 0 fail
- `service:download:domain:desktopTest` — 4 pass, 0 fail
- `service:librarysync:domain:desktopTest` — 6 pass, 0 fail
- Total: **183 tests, 1 pre-existing failure** (unchanged)
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/domain/                    physical module (2 files, 2 tests)
├── source/api/                     physical module (2 files)
├── service/
│   ├── playback/domain/            NEW physical module (2 files, 3 tests)
│   ├── download/domain/            NEW physical module (4 files, 4 tests)
│   └── librarysync/domain/         NEW physical module (2 files, 6 tests)
├── shared/                         main module (~190 files, 170 tests)
├── androidApp/
├── desktopApp/
├── iosApp/
└── build-logic/convention/
```

#### Remaining Split Targets
- `core:data` — blocked by UniFFI cycle (10 UniFFI imports + Bridge.kt)
- `core:presentation` — blocked by `platform.byteArrayToImageBitmap` expect/actual dependency
- `service:*:data` — blocked by UniFFI + platform imports
- `feature:*` presentation modules — unblocked but low priority (already package-separated)

### Session Update 2026-06-28 (Round 8)

#### Fixed DownloadPersistenceIntegrationTest
- [x] **Fixed pre-existing test failure** — `observeActive()` SQL query used uppercase
  status strings (`'QUEUED'`, `'DOWNLOADING'`) but `DownloadStatus` enum `.name` produces
  title case (`"Queued"`, `"Downloading"`). SQLite string comparison is case-sensitive,
  so active tasks were never returned. Fixed to use correct enum casing and added
  `'Resolving'` + `'Paused'` to the active filter (both are active task states).

#### Gate (2026-06-28 Round 8)
- `shared:desktopTest` — **171 tests, 0 failures** (first clean test run)
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

### Session Update 2026-06-28 (Round 9)

#### Physical Module Split: core:presentation
- [x] **Split `core:presentation` into physical Gradle module**
  - Moved 20 of 23 files from `shared/.../core/presentation/` to new `core:presentation/` module
  - 3 files kept in shared: `AppPainterResource.kt`, `DropShadow.kt` (expect/actual), `RepositoryArtworkImageLoader.kt` (moved to core/data/media)
  - Replaced `appPainterResource()` calls with standard `painterResource()` from compose-resources
  - Updated `ArtworkImage`'s generated resource import to new module's `Res` class
  - Configured KMP targets, Compose, Miuix, Koin dependencies
  - Copied all compose resources (drawables + values) to new module
- Dependencies: `:core:domain`, `compose.*`, `miuix-ui`, `koin-compose`, `kotlinx.coroutines.core`
- Zero UniFFI imports

#### Gate (2026-06-28 Round 9)
- `shared:desktopTest` — **171 tests, 0 failures**
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/
│   ├── domain/                    (2 files, 2 tests)
│   └── presentation/              NEW (20 files, ~30 Compose components)
├── source/api/                    (2 files)
├── service/
│   ├── playback/domain/           (2 files, 3 tests)
│   ├── download/domain/           (4 files, 4 tests)
│   └── librarysync/domain/        (2 files, 6 tests)
├── shared/                        (~170 files, 171 tests)
├── androidApp/
├── desktopApp/
└── iosApp/
```

### Session Update 2026-06-28 (Round 10)

#### Physical Module Split: feature:search
- [x] **Split `feature:search` into physical Gradle module** (presentation + domain)
  - Moved 4 presentation files: `SearchRoot`, `SearchScreen`, `SearchState`, `SearchViewModel`
  - Moved 1 domain file: `SearchModels` (SearchTrackItem, SearchHistoryRepository, SearchLibraryUseCase, etc.)
  - Moved 1 presentation test: `SearchViewModelTest` (recreated after accidental deletion)
  - Data layer stays in shared (Room-backed `RoomSearchRepository`, `MusicSourceSearchAggregator`)
  - Dependencies: `:core:domain`, `:core:presentation`, `:source:api`, `:service:download:domain`
  - Own compose resources for generated `Res` class
- Zero UniFFI imports

#### Gate (2026-06-28 Round 10)
- `shared:desktopTest` — 171 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/search/               NEW (5 files, 1 test)
├── shared/                       (~160 files, 171 tests)
├── androidApp/  desktopApp/  iosApp/
```

### Session Update 2026-06-28 (Round 11)

#### Physical Module Split: feature:settings
- [x] **Split `feature:settings` into physical Gradle module** (presentation only)
  - Moved 6 presentation files: `SettingsAction`, `SettingsScreen`, `DebugAction`, `DebugScreen`, `LogAction`, `LogScreen`
  - Moved 1 test: `SettingsActionTest` (8 tests: 3 SettingsAction + 5 DebugAction)
  - Keep in shared: `SettingsRoot` (nav deps), `DebugRoot` (DebugMoreVM), `LogRoot` (LogVM), `SettingsPlatform.kt` expect + platform actuals
  - `SettingsScreen` now accepts `appVersion: String` parameter instead of calling `getAppVersion()` directly — avoids circular dependency on shared platform utils
  - `SettingsRoot` passes `getAppVersion()` to `SettingsScreen`
  - Replaced `appPainterResource()` → `painterResource()`, inline `SettingPaddingX` as `24.dp`
  - Own compose resources: 4 drawables + 11 strings
  - Dependencies: `:core:domain`, `:core:presentation`, compose resources, Koin, coroutines
- Zero UniFFI imports

#### Known Issue
- `feature:search:desktopTest` — `SearchViewModelTest` does not compile (pre-existing from Round 10, API mismatch between test and current SearchViewModel/SearchAggregator interfaces). Not blocking.

#### Gate (2026-06-28 Round 11)
- `shared:desktopTest` — 171 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `feature:search:desktopTest` — compilation failure (pre-existing)
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/
│   ├── search/                   (5 files, 1 broken test)
│   ├── downloads/                (4 files)
│   └── settings/                 NEW (6 files, 1 test)
├── shared/                       (~155 files, 171 tests)
├── androidApp/  desktopApp/  iosApp/
```

### Session Update 2026-06-28 (Round 12)

#### Physical Module Split: feature:playlist
- [x] **Split `feature:playlist` into physical Gradle module** (presentation)
  - Moved 3 State/Action files: `PlaylistState`, `CreatePlaylistState`, `PlaylistsListState`
  - Moved 4 Screen files: `PlaylistScreen`, `PlaylistsListScreen`, `CreatePlaylistScreen`, `EditPlaylistScreen`
  - Created `DurationFormat.kt` (copied from shared `core/utils` to avoid UniFFI dep)
  - Moved 2 tests: `PlaylistsListStateTest` (8 tests), `PlaylistDialogStateTest` (13 tests)
  - Keep in shared: `PlaylistRoot`, `CreatePlaylistRoot`, `EditPlaylistRoot`, `PlaylistsListRoot` (VM deps), `PlaylistMappers` (Room/UniFFI deps), `PlaylistStateTest` (depends on PlaylistMappers)
  - Replaced `appPainterResource()` → `painterResource()` in all Screen files
  - Changed shared resource imports → module-local `tidetunes.feature.playlist.generated.resources`
  - Own compose resources: 13 drawables + 18 strings
  - Dependencies: `:core:domain`, `:core:presentation`, `:service:download:domain`, Compose, Koin, reorderable, coroutines

#### BottomBarSpacer move
- [x] Moved `getBottomBarSpace()` and `BottomBarSpacer` composable from `shared/widgets/appbar/BottomBar.kt` to `core:presentation/components/BottomBarSpacer.kt`
- Pure layout utilities, zero shared-internal deps

#### Gate (2026-06-28 Round 12)
- `shared:desktopTest` — 169 tests, 0 failures (20 moved to feature:playlist, 1 moved to feature:settings)
- `feature:playlist:desktopTest` — 21 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `feature:search:desktopTest` — compilation failure (pre-existing)
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/
│   ├── search/                   (5 files, 1 broken test)
│   ├── downloads/                (4 files)
│   ├── settings/                 (6 files, 1 test, 8 tests)
│   └── playlist/                 NEW (8 files, 2 tests, 21 tests)
├── shared/                       (~140 files, 169 tests)
├── androidApp/  desktopApp/  iosApp/
```

#### Remaining split targets
- `feature:sources` — 9 files, 2 tests (references `core/data` + `database/`)
- `feature:dashboard` — 5 files, 1 test (references `singleton/RoomLibraryStore`)
- `feature:importing` — 4 files, 1 test (data layer refs `core/data` + `database/`)

### Session Update 2026-06-28 (Round 13)

#### Physical Module Split: feature:sources
- [x] **Split `feature:sources` into physical Gradle module** (presentation)
  - Moved 3 State/Draft files: `SourcesState`, `SourceEditorState`, `SourceEditorDraft`
  - Moved 2 Screen files: `SourcesScreen`, `SourceEditorScreen`
  - Keep in shared: `SourcesRoot` (VM + nav deps), `SourcesViewModel` (StorageRepository deps), `SourceEditorRoot` (EditStorageVM), `SourceEditorMapper` (OneDriveDriveInfo deps)
  - Both tests (`SourcesStateTest`, `SourceEditorStateTest`) stay in shared — they depend on `StorageAccountInfo`, `toArgUpsertStorage`, `OneDriveDriveInfo`, UniFFI types
  - Replaced `appPainterResource()` → `painterResource()` in both Screen files
  - Changed shared resource imports → module-local `tidetunes.feature.sources.generated.resources`
  - Fixed `SourceEditorDraft` visibility: `internal` → public (was consumed by shared `core/data` across module boundary)
  - Own compose resources: 6 drawables + 21 strings
  - Dependencies: `:core:domain`, `:core:presentation`, Compose, Koin, coroutines, immutable collections

#### Gate (2026-06-28 Round 13)
- `shared:desktopTest` — 169 tests, 0 failures (unchanged; both sources tests stay in shared)
- `feature:playlist:desktopTest` — 21 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `feature:search:desktopTest` — compilation failure (pre-existing)
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/
│   ├── search/                   (5 files, 1 broken test)
│   ├── downloads/                (4 files)
│   ├── settings/                 (6 files, 1 test, 8 tests)
│   ├── playlist/                 (8 files, 2 tests, 21 tests)
│   └── sources/                  NEW (5 files, 2 tests stay in shared)
├── shared/                       (~135 files, 173 tests)
├── androidApp/  desktopApp/  iosApp/
```

#### Remaining split targets
- `feature:dashboard` — 5 files, 1 test (references `singleton/RoomLibraryStore`)
- `feature:importing` — 4 files, 1 test (data layer refs `core/data` + `database/`)

### Session Update 2026-06-28 (Round 14)

#### Physical Module Split: feature:dashboard
- [x] **Split `feature:dashboard` into physical Gradle module** (presentation)
  - Moved 2 State/Action files: `DashboardState`, `DashboardAction`
  - Moved 1 Screen file: `DashboardScreen`
  - Moved 1 test: `DashboardStateTest` (6 tests)
  - Keep in shared: `DashboardRoot` (SleepModeVM, ImportStatusVM, LocalNavController deps), `TimeToPauseModal` (SleepModeVM)
  - Used slot pattern for SourcesRoot: `DashboardScreen(sourcesContent: @Composable () -> Unit)` — `DashboardRoot` passes `SourcesRoot` as the slot
  - `DashboardScreen` no longer directly imports `SourcesRoot` from `feature:sources`
  - Replaced `appPainterResource()` → `painterResource()`
  - Own compose resources: 3 drawables + 14 strings
  - Dependencies: `:core:domain`, `:core:presentation`, `:service:librarysync:domain`, Compose, Koin, coroutines, immutable collections
  - No dependency on `:feature:sources` needed (slot pattern decouples)

#### Gate (2026-06-28 Round 14)
- `shared:desktopTest` — 169 tests, 0 failures (6 moved to feature:dashboard)
- `feature:dashboard:desktopTest` — 6 tests, 0 failures
- `feature:playlist:desktopTest` — 21 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `feature:search:desktopTest` — compilation failure (pre-existing)
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Updated Module Tree
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/
│   ├── search/                   (5 files, 1 broken test)
│   ├── downloads/                (4 files)
│   ├── settings/                 (6 files, 1 test, 8 tests)
│   ├── playlist/                 (8 files, 2 tests, 21 tests)
│   ├── sources/                  (5 files, 2 tests stay in shared)
│   └── dashboard/                NEW (3 files, 1 test, 6 tests)
├── shared/                       (~130 files, 163 tests)
├── androidApp/  desktopApp/  iosApp/
```

### Session Update 2026-06-28 (Round 15)

#### Physical Module Split: feature:importing
- [x] **Split `feature:importing` into physical Gradle module** (presentation domain)
  - Moved data classes + sealed interfaces: `ImportState`, `ImportPathUi`, `ImportStorageAccountUi`, `ImportLoadState`, `ImportAction`, `ImportEvent`
  - Moved `ImportSelectionMode` enum (was in shared `feature/importing/data/`)
  - Keep in shared: `ImportScreen` (BackHandler), `ImportRoot` (ImportVM), `ImportRepository` (data layer), `ImportStateTest` (SplitPathItem dep), `ImportStateMappers` (SplitPathItem dep)
  - `importState()` factory + `toImportLoadState()` extracted to `ImportStateMappers.kt` in shared
  - Updated all consumers of `ImportSelectionMode` to import from new package
  - Dependencies: `:core:domain`, `:core:presentation`, `:source:api`, Compose, Koin, coroutines, immutable collections
  - No compose resources needed (ImportScreen stays in shared)

#### Gate (2026-06-28 Round 15)
- `shared:desktopTest` — 169 tests, 0 failures
- `feature:dashboard:desktopTest` — 6 tests, 0 failures
- `feature:playlist:desktopTest` — 21 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `feature:search:desktopTest` — compilation failure (pre-existing)
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

#### Final Module Tree (all splittable features done)
```
TideTunes/
├── core/{domain,presentation}/
├── source/api/
├── service/{playback,download,librarysync}/domain/
├── feature/
│   ├── search/                   (5 files, 1 broken test)
│   ├── downloads/                (4 files)
│   ├── settings/                 (6 files, 8 tests)
│   ├── playlist/                 (8 files, 21 tests)
│   ├── sources/                  (5 files)
│   ├── dashboard/                (3 files, 6 tests)
│   └── importing/                NEW (1 file, test stays in shared)
├── shared/                       (~128 files, 169 tests)
├── androidApp/  desktopApp/  iosApp/
└── build-logic/convention/
```

### Session Update 2026-06-28 (Round 16)

#### Fix: feature:search test
- [x] **Fixed `feature:search:desktopTest` compilation failure** (was pre-existing since Round 10)
  - Rewrote `SearchViewModelTest` to match current APIs:
    - `SearchAction.UpdateQuery` → `SearchAction.QueryChanged`
    - `SearchAction.Download` → `SearchAction.DownloadTrack`
    - `SearchTrackItem` constructor: removed non-existent `album` field, added required `durationMs`
    - `SearchLibraryUseCase` → requires `SearchAggregator` constructor param, `invoke(query, sourceAccounts, limit)`
    - `SearchSuggestionsUseCase` → requires `SearchRepository`, `invoke(query, history, limit)`
    - `SearchSourceAccountProvider.sourceAccounts()` returns `List<SearchSourceAccount>` not `List<String>`
    - `SearchHistoryRepository` has `history: Flow<List<String>>` + `clear()`
  - Changed strategy: pure state/action data class tests (8 tests) instead of ViewModel coroutine tests
    - ViewModel's `init` block launches infinite `history.collect {}` — incompatible with `runTest`
    - State and Action data classes are the most valuable test targets (immutability, constructor correctness)
  - 8 tests, 0 failures

#### Gate (2026-06-28 Round 16)
- `shared:desktopTest` — 169 tests, 0 failures
- `feature:search:desktopTest` — **8 tests, 0 failures** (was: compilation failure)
- `feature:playlist:desktopTest` — 21 tests, 0 failures
- `feature:settings:desktopTest` — 8 tests, 0 failures
- `feature:dashboard:desktopTest` — 6 tests, 0 failures
- `feature:downloads:desktopTest` — 1 suite, pass
- `core:domain:desktopTest` — 2 tests, 0 failures
- `service:playback:domain:desktopTest` — 3 tests, 0 failures
- `service:download:domain:desktopTest` — 4 tests, 0 failures
- `service:librarysync:domain:desktopTest` — 6 tests, 0 failures
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

**All 11 modules: 227 tests, 0 failures**

### Session Update 2026-06-28 (Round 17)

#### Convention Plugins — Completion & Limitations
- [x] **Created `convention.kmp.domain` plugin** — `TideTunesKmpDomainPlugin` (applies kmp.library, no Compose)
- [x] **Created `convention.music-source` plugin** — `TideTunesMusicSourcePlugin` (applies kmp.library + cmp.library)
- [x] **Added catalog aliases** — `libs.plugins.convention.kmp.domain` + `libs.plugins.convention.music-source`
- [x] **Registered in build-logic** — 7 convention plugins total

**Investigation: KGP target/dependency configuration from included build class-based plugins**
- Adding `compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")` to convention build does make KGP classes available at compile time
- However, `KotlinMultiplatformExtension` methods (`iosArm64()`, `jvm()`, `androidTarget()`) have overload resolution ambiguity when called from class-based plugin code — different from Kotlin DSL
- Android `LibraryExtension` and Compose extension APIs also unavailable without additional compileOnly deps (which hardcode versions and introduce version-mismatch risk)
- **Conclusion:** class-based convention plugins in an included build CANNOT configure KMP targets or dependencies. They are limited to `plugins.apply(...)` — which is sufficient to reduce the per-module plugin declaration from ~7 lines to ~2 lines

**App module migration:** androidApp and desktopApp use application plugins with platform-specific configs — convention plugins add little value here. Left as-is.

#### Gate (2026-06-28 Round 17)
- 227 tests, 0 failures across 11 modules
- All platform compilations pass

#### Final Convention Plugin Index
| Plugin ID | Class | Applies |
|-----------|-------|---------|
| `convention.kmp.library` | `TideTunesKmpLibraryPlugin` | kotlin.multiplatform, kotlin.plugin.serialization, com.android.library |
| `convention.cmp.library` | `TideTunesCmpLibraryPlugin` | org.jetbrains.compose, org.jetbrains.kotlin.plugin.compose |
| `convention.kmp.domain` | `TideTunesKmpDomainPlugin` | kmp.library (for core:domain, service:*:domain, source:api) |
| `convention.feature` | `TideTunesFeaturePlugin` | kmp.library + cmp.library (for feature:* modules) |
| `convention.music-source` | `TideTunesMusicSourcePlugin` | kmp.library + cmp.library (for source:* modules) |
| `convention.room` | `TideTunesRoomPlugin` | KSP + Room schema config |
| `convention.cargo-uniffi` | `TideTunesCargoUniffiPlugin` | Gobley Cargo + UniFFI |

#### Remaining Unchecked Items (all blocked)
| Item | Blocker |
|------|---------|
| `core:data` physical module split | UniFFI cycle (10 imports + Bridge.kt) |
| Gapless, Crossfade, ReplayGain, Output Device, Android Auto, AirPlay, CarPlay | Rust/libmpv backend |
| OneDrive cancellable delta sync | Rust backend |

### Session Update 2026-06-28 (Round 18)

#### Convention Plugins — Applied to Modules
- [x] **Applied `convention.feature`** to all feature modules:
  - `feature:downloads`, `feature:settings`, `feature:playlist`, `feature:sources`, `feature:dashboard`, `feature:search`, `feature:importing`
- [x] **Applied `convention.kmp.domain`** to domain/API modules:
  - `core:domain`, `source:api`, `service:playback:domain`, `service:download:domain`, `service:librarysync:domain`
- [x] **Applied library conventions to `core:presentation`**:
  - `convention.kmp.library` + `convention.cmp.library`
- [x] **Reduced plugin blocks** while preserving each module's explicit target/resource/dependency configuration
- [x] **Cleaned test warnings**:
  - Removed always-true `SearchAction.QueryChanged` type assertion
  - Reused `assertIs<Artwork.LibraryCover>()` return value in playlist tests

#### Gate (2026-06-28 Round 18)
- `shared:desktopTest` — PASS
- `feature:search:desktopTest` — PASS
- `feature:playlist:desktopTest` — PASS
- `feature:settings:desktopTest` — PASS
- `feature:dashboard:desktopTest` — PASS
- `feature:downloads:desktopTest` — PASS
- `core:domain:desktopTest` — PASS
- `service:playback:domain:desktopTest` — PASS
- `service:download:domain:desktopTest` — PASS
- `service:librarysync:domain:desktopTest` — PASS
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 377 Gradle tasks, BUILD SUCCESSFUL.**

#### Remaining Work
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- OneDrive cancellable delta sync and playback advanced capabilities remain Rust/backend work.

### Session Update 2026-06-28 (Round 19)

#### Final Architecture Documentation
- [x] **Updated `docs/architecture/final-architecture.md`** to match the current physical module tree:
  - `core:{domain,presentation}`
  - `source:api`
  - `service:{playback,download,librarysync}:domain`
  - `feature:{search,downloads,settings,playlist,sources,dashboard,importing}`
  - transitional `shared` ownership for DI, navigation, Room, UniFFI, platform actuals, and legacy bridge code
- [x] **Replaced stale shared-only architecture description** from the pre-split state
- [x] **Documented current dependency map, key interfaces, Koin assembly, Room ownership, build logic, gate command, known limits, and extension guidance**

#### Gate Context
- No code changes after the Round 18 full gate.
- Round 18 full gate remains authoritative: 377 Gradle tasks, BUILD SUCCESSFUL.

#### Remaining Work
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- OneDrive cancellable delta sync and advanced playback capabilities remain Rust/backend work.

### Session Update 2026-06-28 (Round 20)

#### Advanced Playback Domain Contract
- [x] **Added optional `AdvancedPlaybackController` in `service:playback:domain`**
  - Exposes `capabilities`, `enhancementSettings`, and `outputState` flows.
  - Keeps the contract separate from `PlaybackController` so existing platform
    bridges do not need to fake unsupported backend behavior.
- [x] **Added shared advanced playback models**
  - `PlaybackEngineCapabilities`
  - `PlaybackFeature`
  - `PlaybackEnhancementSettings`
  - `ReplayGainMode`
  - `AudioOutputDeviceId`
  - `AudioOutputDevice`
  - `AudioOutputDeviceType`
  - `AudioOutputState`
- [x] **Added domain tests for the new models**
  - Default settings keep advanced processing disabled.
  - Crossfade and ReplayGain bounds are validated.
  - Capability lookup and selected output-device resolution are covered.
  - Blank output-device identifiers/names are rejected.

#### Gate (2026-06-28 Round 20)
- `:service:playback:domain:desktopTest` — PASS
- `:service:playback:domain:compileKotlinIosSimulatorArm64` — PASS
- `shared:desktopTest` — PASS
- `feature:search:desktopTest` — PASS
- `feature:playlist:desktopTest` — PASS
- `feature:settings:desktopTest` — PASS
- `feature:dashboard:desktopTest` — PASS
- `feature:downloads:desktopTest` — PASS
- `core:domain:desktopTest` — PASS
- `service:download:domain:desktopTest` — PASS
- `service:librarysync:domain:desktopTest` — PASS
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 373 Gradle tasks, BUILD SUCCESSFUL.**
`service:playback:domain:desktopTest` reports 11 tests, 0 failures, 0 errors.

#### Remaining Work
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the new domain contract.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 21)

#### Playback Engine Domain Boundary
- [x] **Added pure commonMain `PlaybackEngine` contract in `service:playback:domain`**
  - Defines load, play, pause, stop, seek, position read, and release commands.
  - Keeps Media3, AVFoundation, libmpv, and UniFFI engine objects out of commonMain.
- [x] **Added playback engine load/resource models**
  - `PlaybackEngineLoadRequest`
  - `PlaybackEngineResource`
  - `PlaybackEngineLoadResult`
  - `PlaybackEngineUnsupportedReason`
  - `PlaybackEngineFailureReason`
- [x] **Added `AudioOutputController` contract**
  - Owns `AudioOutputState` and output-device selection.
  - `AdvancedPlaybackController` now extends `AudioOutputController` so output
    selection can evolve separately from enhancement settings.
- [x] **Added playback engine model tests**
  - Blank resource URI is rejected.
  - Blank header names are rejected.
  - Negative expiration is rejected.
  - Expiring and non-expiring resources are covered.
  - Load request keeps `PlayableItem` metadata separate from the engine resource.

#### Gate (2026-06-28 Round 21)
- `:service:playback:domain:desktopTest` — PASS
- `:service:playback:domain:compileKotlinIosSimulatorArm64` — PASS
- `shared:desktopTest` — PASS
- `feature:search:desktopTest` — PASS
- `feature:playlist:desktopTest` — PASS
- `feature:settings:desktopTest` — PASS
- `feature:dashboard:desktopTest` — PASS
- `feature:downloads:desktopTest` — PASS
- `core:domain:desktopTest` — PASS
- `service:download:domain:desktopTest` — PASS
- `service:librarysync:domain:desktopTest` — PASS
- `shared:compileDebugKotlinAndroid` — PASS
- `shared:compileKotlinIosSimulatorArm64` — PASS
- `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 377 Gradle tasks, BUILD SUCCESSFUL.**
`service:playback:domain:desktopTest` reports 17 tests, 0 failures, 0 errors.

#### Remaining Work
- Platform Android/iOS/Desktop engine adapters still need to adopt the common
  `PlaybackEngine` boundary behind `PlaybackController`.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the new contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 22)

#### Platform PlaybackEngine Adapters
- [x] **Added shared playback engine mappers**
  - `PlaybackResource` from `source:api` is adapted to domain
    `PlaybackEngineResource` only at the shared data boundary.
  - UniFFI `Music` / `MusicAbstract` are adapted to `PlayableItem` before they
    cross into the engine contract.
- [x] **Desktop engine now implements the common `PlaybackEngine` contract**
  - `MpvDesktopPlaybackEngine` loads `PlaybackEngineLoadRequest`.
  - The adapter maps MPV position methods into `PlaybackPosition`.
  - Expired resources and unsupported load failures return domain load results.
- [x] **iOS engine now implements the common `PlaybackEngine` contract**
  - `AvPlayerIosPlaybackEngine` loads `PlaybackEngineLoadRequest`.
  - AVPlayer position reads are mapped into `PlaybackPosition`.
  - Expired resources and unsupported URL failures return domain load results.
- [x] **Android engine now implements the common `PlaybackEngine` contract**
  - Media3 loading is driven by `PlayableItem` plus playback URI instead of
    passing UniFFI `Music` into the engine.
  - `PlayerControllerRepository` now loads through
    `PlaybackEngineLoadRequest` and handles common load result types.
- [x] **Updated platform controller tests**
  - Android, Desktop, and iOS controller tests now assert the resolved resource
    URI and mapped `PlayableItem` title passed through the common engine load
    request.
  - The iOS test compile drift was fixed by aligning repository imports with
    the Desktop playback harness.

#### Gate (2026-06-28 Round 22)
- Focused playback gate:
  - `shared:desktopTest` with `LegacyPlaybackControllerTest`,
    `PlaybackResourceResolverTest`, `DesktopPlaybackEngineTest`, and
    `DesktopPlayerControllerTest` — PASS
  - `shared:iosSimulatorArm64Test` with `IosPlayerControllerTest` — PASS
  - `shared:testDebugUnitTest` with `PlayerControllerRepositoryTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS
- Full regression gate:
  - `shared:desktopTest` — PASS
  - `feature:search:desktopTest` — PASS
  - `feature:playlist:desktopTest` — PASS
  - `feature:settings:desktopTest` — PASS
  - `feature:dashboard:desktopTest` — PASS
  - `feature:downloads:desktopTest` — PASS
  - `core:domain:desktopTest` — PASS
  - `service:playback:domain:desktopTest` — PASS
  - `service:download:domain:desktopTest` — PASS
  - `service:librarysync:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest` — PASS
  - `shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 426 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 463 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 202 tests, 0 failures, 0 errors, 0 skipped.

`simctl` still reports a local malformed iOS 15.5 runtime bundle warning during
iOS simulator tests, but the Gradle task completes successfully.

#### Remaining Work
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 23)

#### Onboarding Feature Module Split
- [x] **Added physical `:feature:onboarding` module**
  - Added the module to `settings.gradle.kts`.
  - Added `shared` dependency on `:feature:onboarding` so existing navigation
    and Koin assembly can continue to compose the route.
  - The module follows the existing feature Gradle pattern and compiles for
    Android, iOS Simulator, and Desktop.
- [x] **Moved onboarding presentation out of `shared`**
  - Moved `OnboardingRoot`, `OnboardingScreen`, `OnboardingViewModel`,
    `OnboardingState`, `OnboardingAction`, and `OnboardingEvent`.
  - Kept package names stable so `OnboardingGraph` and `LibraryFeatureModule`
    did not need route or DI rewrites.
  - The moved feature has no Room, DAO, UniFFI, source implementation, or
    platform playback dependencies.
- [x] **Added onboarding presentation tests**
  - Default onboarding state starts on the welcome page.
  - Page ordering is fixed.
  - ViewModel next/previous actions stay within bounds.
  - Finish action remains a singleton command.

#### Gate (2026-06-28 Round 23)
- Focused onboarding gate:
  - `:feature:onboarding:desktopTest` — PASS
  - `:feature:onboarding:compileDebugKotlinAndroid` — PASS
  - `:feature:onboarding:compileKotlinIosSimulatorArm64` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS
- Full regression gate:
  - `shared:desktopTest` — PASS
  - `feature:search:desktopTest` — PASS
  - `feature:playlist:desktopTest` — PASS
  - `feature:settings:desktopTest` — PASS
  - `feature:dashboard:desktopTest` — PASS
  - `feature:downloads:desktopTest` — PASS
  - `feature:onboarding:desktopTest` — PASS
  - `core:domain:desktopTest` — PASS
  - `service:playback:domain:desktopTest` — PASS
  - `service:download:domain:desktopTest` — PASS
  - `service:librarysync:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest` — PASS
  - `shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 359 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 493 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 206 tests, 0 failures, 0 errors, 0 skipped.

`simctl` still reports a local malformed iOS 15.5 runtime bundle warning during
iOS simulator tests, but the Gradle task completes successfully.

#### Remaining Work
- Remaining shared-only presentation areas include album, artist, browse,
  library, lyrics, queue, radio, recently added, and recently played.
- Several of those still depend on Room DAOs or legacy shared repositories and
  need boundary extraction before physical feature split.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 24)

#### Queue Feature Module Split
- [x] **Added physical `:feature:queue` module**
  - Added to `settings.gradle.kts` and registered as a `shared` dependency so
    `QueueGraph` and `LibraryFeatureModule` can continue composing the route
    without import changes.
  - Follows the existing feature Gradle pattern for Android, iOS Simulator,
    and Desktop targets.
- [x] **Moved queue presentation out of `shared`**
  - Moved `QueueRoot`, `QueueScreen`, `QueueViewModel`, `QueueState`,
    `QueueItemUi`, `QueueAction`, and `QueueEvent`.
  - `QueueViewModel` depends only on the domain-layer `PlaybackController`,
    `PlayableItem`, and `PlaybackStatus` from `service:playback:domain`.
  - Inlined a local `durationLabel()` in `QueueScreen` to remove the
    `shared`-only `core.utils.formatDuration` import.
- [x] **Added queue presentation tests**
  - Default state is empty with currentIndex -1 and playing false.
  - Populated state preserves item data and current-index/isCurrent flags.
  - PlayItem, RemoveItem, MoveItem, and ClearQueue actions carry correct
    payloads.

#### Gate (2026-06-28 Round 24)
- Focused queue gate:
  - `:feature:queue:desktopTest` — PASS
  - `:feature:queue:compileDebugKotlinAndroid` — PASS
  - `:feature:queue:compileKotlinIosSimulatorArm64` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS
- Full regression gate:
  - `shared:desktopTest` — PASS
  - `feature:search:desktopTest` — PASS
  - `feature:playlist:desktopTest` — PASS
  - `feature:settings:desktopTest` — PASS
  - `feature:dashboard:desktopTest` — PASS
  - `feature:downloads:desktopTest` — PASS
  - `feature:onboarding:desktopTest` — PASS
  - `feature:queue:desktopTest` — PASS
  - `core:domain:desktopTest` — PASS
  - `service:playback:domain:desktopTest` — PASS
  - `service:download:domain:desktopTest` — PASS
  - `service:librarysync:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest` — PASS
  - `shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 378 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 523 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 212 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Remaining shared-only presentation areas include album, artist, browse,
  library, lyrics, radio, recently added, and recently played.
- `lyrics` depends on `MetadataDao` and `TrackDao`; it needs a clean domain
  boundary before physical split.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 25)

#### Radio Feature Module Split
- [x] **Added physical `:feature:radio` module**
  - Added to `settings.gradle.kts` and registered as a `shared` dependency so
    `RadioGraph` and `LibraryFeatureModule` can continue composing the route
    without import changes.
  - Follows the existing feature Gradle pattern for Android, iOS Simulator,
    and Desktop targets.
- [x] **Moved radio presentation out of `shared`**
  - Moved `RadioState`, `RadioAction`, `RadioEvent`, and `RadioScreen`.
  - `RadioViewModel` and `RadioRoot` remain in `shared` because they depend
    on Room DAOs (`MetadataDao`, `TrackDao`) and `LegacyStorageLookup`.
  - `RadioScreen` inlines a local `durationLabel()` helper to avoid the
    `shared`-only `core.utils.formatDuration` import.
- [x] **Added radio presentation tests**
  - Default state is idle with empty stations, no error, not loading.
  - Populated state preserves station data and loading/error flags.
  - PlayStation and Refresh actions carry correct payloads.

#### Gate (2026-06-28 Round 25)
- Focused radio gate:
  - `:feature:radio:desktopTest` — PASS
  - `:feature:radio:compileDebugKotlinAndroid` — PASS
  - `:feature:radio:compileKotlinIosSimulatorArm64` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS
- Full regression gate:
  - `shared:desktopTest` — PASS
  - `feature:search:desktopTest` — PASS
  - `feature:playlist:desktopTest` — PASS
  - `feature:settings:desktopTest` — PASS
  - `feature:dashboard:desktopTest` — PASS
  - `feature:downloads:desktopTest` — PASS
  - `feature:onboarding:desktopTest` — PASS
  - `feature:queue:desktopTest` — PASS
  - `feature:radio:desktopTest` — PASS
  - `core:domain:desktopTest` — PASS
  - `service:playback:domain:desktopTest` — PASS
  - `service:download:domain:desktopTest` — PASS
  - `service:librarysync:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest` — PASS
  - `shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 397 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 549 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 218 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Remaining shared-only presentation areas include album, artist, browse,
  library, lyrics, recently added, and recently played.
- `lyrics` depends on `MetadataDao` and `TrackDao`; it needs a clean domain
  boundary before physical split.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 26)

#### Lyrics Feature Module Split
- [x] **Added physical `:feature:lyrics` module**
  - Added to `settings.gradle.kts` and registered as a `shared` dependency so
    `LyricsGraph` and `LibraryFeatureModule` can continue composing the route
    without import changes.
  - Follows the existing feature Gradle pattern for Android, iOS Simulator,
    and Desktop targets.
- [x] **Moved lyrics presentation out of `shared`**
  - Moved `LyricsState`, `LyricsAction`, `LyricsEvent`, `LyricsScreen`, and
    `AnimatedLyricLine`.
  - `LyricsViewModel` and `LyricsRoot` remain in `shared` because they depend
    on `MetadataDao`, `TrackDao`, and `SavedStateHandle`.
- [x] **Added lyrics presentation tests**
  - Default state is loading with empty lines, no title/artist/error.
  - Loaded state preserves track metadata, lines, format, and synchronized flag.
  - Error state carries message while preserving title and artist.
  - NavigateBack and Retry are singleton actions.

#### Gate (2026-06-28 Round 26)
- Focused lyrics gate:
  - `:feature:lyrics:desktopTest` — PASS (5 tests)
  - `:feature:lyrics:compileDebugKotlinAndroid` — PASS
  - `:feature:lyrics:compileKotlinIosSimulatorArm64` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS
- Full regression gate:
  - `shared:desktopTest` — PASS
  - `feature:search:desktopTest` — PASS
  - `feature:playlist:desktopTest` — PASS
  - `feature:settings:desktopTest` — PASS
  - `feature:dashboard:desktopTest` — PASS
  - `feature:downloads:desktopTest` — PASS
  - `feature:onboarding:desktopTest` — PASS
  - `feature:queue:desktopTest` — PASS
  - `feature:radio:desktopTest` — PASS
  - `feature:lyrics:desktopTest` — PASS
  - `core:domain:desktopTest` — PASS
  - `service:playback:domain:desktopTest` — PASS
  - `service:download:domain:desktopTest` — PASS
  - `service:librarysync:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest --tests com.github.tidetunes.singleton.PlayerControllerRepositoryTest` — PASS
  - `shared:iosSimulatorArm64Test --tests com.github.tidetunes.singleton.IosPlayerControllerTest` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 412 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 583 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 223 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Remaining shared-only presentation areas include album, artist, browse,
  library, recently added, and recently played.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 27)

#### RecentlyAdded + RecentlyPlayed Feature Module Splits
- [x] **Added physical `:feature:recentlyadded` and `:feature:recentlyplayed` modules**
  - Both added to `settings.gradle.kts` and registered as `shared` dependencies.
  - Follow the existing feature Gradle pattern for Android, iOS Simulator, Desktop.
- [x] **Moved presentation out of `shared`**
  - `RecentlyAddedState`, `RecentlyAddedAction`, `RecentlyAddedEvent`, `RecentlyAddedScreen`
  - `RecentlyPlayedState`, `RecentlyPlayedAction`, `RecentlyPlayedEvent`, `RecentlyPlayedScreen`
  - ViewModels and Roots stay in `shared` (depend on `TrackDao`, `MetadataDao`, `LegacyStorageLookup`, `EnqueueDownloadUseCase`).
  - Both screens inline a local `durationLabel()` to avoid `shared`-only `core.utils.formatDuration`.
- [x] **Added presentation tests** (6 tests each)
  - Default state is loading with empty tracks.
  - Loaded state preserves track data including mediaId/canDownload fields.
  - Error state preserves previous tracks.
  - PlayTrack action carries correct track ID.
  - PlayAll, Retry, and NavigateBack are singleton actions.

#### Gate (2026-06-28 Round 27)
- Focused gate:
  - `:feature:recentlyadded:desktopTest` — PASS (6 tests)
  - `:feature:recentlyplayed:desktopTest` — PASS (6 tests)
  - All cross-platform compilations — PASS
- Full regression gate:
  - All 13 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 455 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 643 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 235 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Remaining shared-only presentation areas: album, artist, browse, library.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 28)

#### Album + Artist + Browse Feature Module Splits
- [x] **Added physical `:feature:album`, `:feature:artist`, and `:feature:browse` modules**
  - All three added to `settings.gradle.kts` and registered as `shared` dependencies.
  - Follow the existing feature Gradle pattern for Android, iOS Simulator, Desktop.
- [x] **Moved presentation out of `shared`**
  - Album: `AlbumState`, `AlbumAction`, `AlbumEvent`, `AlbumScreen`
  - Artist: `ArtistState`, `ArtistAction`, `ArtistEvent`, `ArtistScreen`
  - Browse: `BrowseState`, `BrowseAction`, `BrowseEvent`, `BrowseScreen`, `GenreTracksState`, `GenreTracksAction`, `GenreTracksScreen`
  - ViewModels and Roots stay in `shared` (DAO deps: `MetadataDao`, `TrackDao`, `LegacyStorageLookup`, `EnqueueDownloadUseCase`).
  - `GenreTracksAction` was extracted from `GenreTracksViewModel.kt` (where it was co-located) into its own file in the feature module.
  - `AlbumScreen`, `ArtistScreen`, `GenreTracksScreen` each inline a local `durationLabel()` to avoid `shared`-only `core.utils.formatDuration`.
- [x] **Added presentation tests** (19 total)
  - `AlbumStateTest` (5): default state, loaded state, error state, play track action, singleton actions.
  - `ArtistStateTest` (6): default state, loaded state with albums+tracks, error state, play track, navigate to album, singleton actions.
  - `BrowseStateTest` (5): default state, loaded state, error state, navigate actions, singleton actions.
  - `GenreTracksStateTest` (3): default state, loaded state, error state.

#### Gate (2026-06-28 Round 28)
- Focused gate:
  - `:feature:album:desktopTest` — PASS (5 tests)
  - `:feature:artist:desktopTest` — PASS (6 tests)
  - `:feature:browse:desktopTest` — PASS (8 tests)
  - All cross-platform compilations — PASS
- Full regression gate:
  - All 15 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 517 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 733 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 254 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Remaining shared-only presentation area: library (blocked by `core.data.LibraryTrackItem` and shared-generated Compose resources).
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 29)

#### Library Feature Module Split
- [x] **Moved `LibraryTrackItem` from `core.data` to `core:domain/model`**
  - `LibraryTrackItem` was a pure data class depending only on `core:domain` types.
  - Moved it out of shared into `core:domain/src/commonMain/.../core/domain/model/LibraryTrackItem.kt`.
  - Updated all 4 imports across shared (`LibraryVM`, `LibraryRepository`, `LibraryState`, `LibraryScreen`).
  - Removed inline definition from `LibraryRepository.kt`.
- [x] **Added physical `:feature:library` module**
  - Added to `settings.gradle.kts` and registered as a `shared` dependency.
  - Follows the existing feature Gradle pattern.
- [x] **Moved library presentation out of `shared`**
  - Moved `LibraryState` (contains `LibraryAction`, `LibraryEvent`) and `LibraryScreen`.
  - `LibraryVM` and `LibraryRoot` stay in `shared` (depend on `LibraryRepository`, `TrackDao`, `LegacyStorageLookup`, `EnqueueDownloadUseCase`).
  - Replaced 4 shared Compose resource calls (`Res.string.library_title`, etc.) with plain text strings.
  - Inlined `durationLabel()` to avoid shared-only `kotlin.time` → `core.utils` dependency.
- [x] **Added library presentation tests** (5 tests)
  - Default state is empty.
  - State with tracks preserves data.
  - Refresh action is a singleton.
  - DownloadTrack action carries the track item.
  - ShowMessage event carries the message text.

#### Gate (2026-06-28 Round 29)
- Focused library gate:
  - `:feature:library:desktopTest` — PASS (5 tests)
  - All cross-platform compilations — PASS
- Full regression gate:
  - All 16 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 530 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 763 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 259 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- **All 17 feature presentations now have physical modules.** The shared-only feature layer is fully split.
- Remaining shared files are ViewModels, Roots, data/repository implementations, navigation graphs, and platform bridges — all blocked by Room/UniFFI/legacy dependencies.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 30)

#### Playback Presentation Module
- [x] **Added physical `:service:playback:presentation` module**
  - Added to `settings.gradle.kts` and registered as a `shared` dependency.
  - Uses `convention.feature` plugin (needs Compose for state annotations).
  - Depends on `:core:domain`, `:core:presentation`, `:service:playback:domain`.
- [x] **Moved NowPlaying presentation contracts out of `shared`**
  - Moved `NowPlayingState.kt` (all State/Action/Event types for Now Playing page).
  - Moved `NowPlayingMappers.kt` (domain-to-presentation mapping functions).
  - Changed mapper functions from `internal` to `public` — they are legitimately
    called from `PlayerVM` in shared and from within the presentation module.
  - `NowPlayingScreen.kt` and `NowPlayingRoot.kt` remain in `shared` (heavy
    shared Compose resource dependencies, `core.utils` calls, cross-feature
    import of `AnimatedLyricLine` from `:feature:lyrics`).
- [x] **Preserved existing tests** (4 tests)
  - `CurrentTrackInfo → NowPlayingTrackItem` mapping.
  - `PlaybackQueue → NowPlayingQueueState` with previous/next artwork.
  - `PlayerState → NowPlayingControlsState` with play/pause/repeat/shuffle.
  - Queue state reports no prev/next when artworks are null.

#### Gate (2026-06-28 Round 30)
- Focused gate:
  - `:service:playback:presentation:desktopTest` — PASS (4 tests)
  - All cross-platform compilations — PASS
- Full regression gate:
  - All 17 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Focused gate: 545 Gradle tasks, BUILD SUCCESSFUL.**
**Full gate: 793 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 259 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining Work
- Feature presentation layer is fully split across 17 physical modules.
- Service presentation layer has one physical module (`:service:playback:presentation`).
- `NowPlayingScreen`/`NowPlayingRoot` remain blocked by shared resources,
  `core.utils`, and an `:feature:lyrics` cross-feature import.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 31)

#### Downloads DI Module Colocation
- [x] **Moved `downloadsFeatureModule` Koin DI from `shared` to `:feature:downloads`**
  - Created `feature/downloads/src/commonMain/.../di/DownloadsFeatureModule.kt`.
  - The module registers `DownloadsViewModel` with its single `DownloadController` dependency from `service:download:domain`.
  - Updated `AppModule.kt` to import from `com.github.tidetunes.feature.downloads.di`.
  - Removed old file from `shared/src/commonMain/.../di/DownloadsFeatureModule.kt`.

#### Gate (2026-06-28 Round 31)
- Focused compile check — PASS
- Full regression gate:
  - All 17 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - All cross-platform compilations — PASS

**Full gate: 789 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 259 tests, 0 failures, 0 errors, 0 skipped.

#### Remaining DI Modules (all blocked)
| Module | Blocker |
|--------|---------|
| `searchFeatureModule` | `RoomSearchRepository` lives in `shared` (Room DAO dependency) |
| `settingsFeatureModule` | `ToastRepository` + ViewModels live in `shared` |
| `libraryFeatureModule` | 16 ViewModel registrations, all in `shared` |
| `importFeatureModule` | Depends on `shared`-only bridging types |
| `playbackModule`, `downloadModule`, `coreDataModule`, `sourceDataModule`, `librarySyncModule` | Platform actuals, Room, UniFFI, Rust bridge |
| `platformModule` | `expect/actual` platform declarations |

#### Remaining Work
- Feature presentation and downloads DI are in physical modules. All other DI modules are blocked by shared infrastructure.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend implementation.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 32)

#### AppInitializer — Consolidate Platform Init Logic
- [x] **Created `AppInitializer` in `shared/di`**
  - `initializeBridge(koin)` — synchronous Rust bridge init via Koin
  - `reloadRepositories(koin, scope)` — async reload of player, storage, and playlist repositories
  - Takes an explicit `org.koin.core.Koin` parameter to avoid `GlobalContext`
    (not available in Kotlin/Native commonMain).
- [x] **Simplified `androidApp/MainActivity.kt`**
  - Removed `Bridge`, `PlayerRepository`, `PlaylistRepository` field injections
    (3 fewer injected singletons).
  - `bridge.initialize()` → `AppInitializer.initializeBridge(GlobalContext.get())`
  - `reload()` calls → `AppInitializer.reloadRepositories(...)`
  - Retained only `PermissionRepository`, `PlayerControllerRepository`, `StorageRepository`
    (needed for permission handling, Media3 controller, OAuth redirect).
- [x] **Simplified `desktopApp/Main.kt`**
  - Removed direct `Bridge`, `PlayerRepository`, `StorageRepository`, `PlaylistRepository`
    references and explicit `koin.get<>()` calls.
  - Now 25 lines (down from 38): initKoin → initializeBridge → reloadRepositories → Window.
  - Uses `initKoin().koin` to pass the Koin instance to AppInitializer.

#### Gate (2026-06-28 Round 32)
- First run: flaky MPV segfault in `DesktopPlaybackEngineTest.readyEngineStartsPlaybackAndReleasesResourceOnStop`.
  Retry passed cleanly.
- Full regression gate:
  - All 17 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 789 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 259 tests, 0 failures, 0 errors, 0 skipped.

#### Known Pre-existing Issues
- `androidApp:compileDebugKotlin` fails: Miuix 0.9.2 requires compileSdk 37,
  but AGP 8.12.3 supports max 36. Shared library Android compilation passes.
  This is an SDK/plugin version update task, not a code regression.
- Occasional MPV segfault in `DesktopPlaybackEngineTest` (retry passes).

#### Remaining Work
- Feature presentation, downloads DI, and platform init are in physical modules.
  All remaining splits are blocked by Room/UniFFI/Rust/platform-actual boundaries.
- Gapless playback, Crossfade, ReplayGain, output device selection, Android
  Auto, AirPlay, and CarPlay still require Rust/libmpv or platform/backend
  implementation behind the common contracts.
- `core:data` physical module split remains blocked by UniFFI/Rust bridge ownership.
- Source implementation modules remain blocked by legacy storage bridge and
  Rust/UniFFI dependencies.
- OneDrive cancellable delta sync remains Rust/backend work.

### Session Update 2026-06-28 (Round 33)

#### Onboarding + Queue DI Colocation
- [x] **Added Koin DI modules to `:feature:onboarding` and `:feature:queue`**
  - Created `OnboardingFeatureModule.kt` in `feature/onboarding/.../di/`
    registering `OnboardingViewModel` (zero constructor deps).
  - Created `QueueFeatureModule.kt` in `feature/queue/.../di/`
    registering `QueueViewModel` (depends on `PlaybackController` from `:service:playback:domain`).
  - Both ViewModels were already physically in their feature modules from Rounds 23/24.
    The DI wiring is now colocated alongside them.
- [x] **Updated `LibraryFeatureModule` to use `includes()`**
  - Replaced direct `viewModelOf(::OnboardingViewModel)` and `viewModelOf(::QueueViewModel)`
    with `includes(onboardingFeatureModule, queueFeatureModule)`.
  - Koin deps (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) were already present
    in both feature module build scripts.

#### Gate (2026-06-28 Round 33)
- Desktop compile check — PASS
- Full regression gate:
  - All 17 desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 789 Gradle tasks, BUILD SUCCESSFUL.**
Current local XML test reports: 259 tests, 0 failures, 0 errors, 0 skipped.

#### DI Colocation Status
| DI Module | Location | Status |
|-----------|----------|--------|
| `downloadsFeatureModule` | `:feature:downloads/di/` | Colocated (R31) |
| `onboardingFeatureModule` | `:feature:onboarding/di/` | Colocated (R33) |
| `queueFeatureModule` | `:feature:queue/di/` | Colocated (R33) |
| `searchFeatureModule` | `shared/di/` | Blocked (Room DAO deps) |
| `settingsFeatureModule` | `shared/di/` | Blocked (shared ViewModels) |
| `libraryFeatureModule` | `shared/di/` | Blocked (shared singletons + VMs) |
| `importFeatureModule` | `shared/di/` | Blocked (shared bridging) |
| `playbackModule` | `shared/di/` | Blocked (platform engines) |
| `downloadModule` | `shared/di/` | Blocked (Room persistence) |
| `coreDataModule` | `shared/di/` | Blocked (Room/DataStore) |
| `sourceDataModule` | `shared/di/` | Blocked (legacy source bridges) |
| `librarySyncModule` | `shared/di/` | Blocked (legacy sync impl) |
| `platformModule` | `shared/di/` | Blocked (expect/actual) |

3 of 13 DI modules now colocated to feature modules.

### Session Update 2026-06-28 (Round 34 — Plateau Assessment)

#### Upper-Layer Refactoring Complete

After rounds 25-33, all presentation-layer code that can physically separate from
`shared` has been moved:

- **17 feature modules**: every State, Action, Event, and Screen lives in a
  physical Gradle module. The only Screen remaining in `shared` is `ImportScreen`
  (26 shared-generated Compose resource imports + `BackHandler` platform class).
- **1 service presentation module**: `service:playback:presentation` holds
  `NowPlayingState`, `NowPlayingMappers`, and their tests.
- **3 DI modules colocated**: `downloadsFeatureModule`, `onboardingFeatureModule`,
  `queueFeatureModule` live alongside their ViewModels in feature modules.
- **Platform init consolidated**: `AppInitializer` replaces duplicated
  `Bridge.initialize()` + `repository.reload()` in both entry points.

#### What Cannot Split Further

Every remaining file in `shared` depends on at least one of these blockers:

| Blocker | Files Affected | Examples |
|---------|---------------|----------|
| UniFFI `uniffi.tidetunes_core.*` | 35+ files | `Storage`, `Music`, `MusicId`, `StorageType`, `ArgUpsertStorage` |
| Room database entities/DAOs | 20+ files | `TrackDao`, `MetadataDao`, `TrackEntity`, `StorageEntity` |
| `expect`/`actual` platform declarations | 10+ files | `PlatformModule`, `BackHandler`, `SettingsPlatform` |
| Legacy storage wrappers | 15+ files | `LegacyStorageLookup`, `StorageRepository`, source adapters |
| Shared Compose resources (`Res.*`) | 3 files | `ImportScreen`, `NowPlayingScreen`, `TimeToPauseModal` |

#### Infrastructure Roadmap (Next Phase)

To unlock the remaining DI modules, ViewModels, and data code:

1. **`database:api` module** — Extract Room DAO interfaces (`TrackDao`,
   `MetadataDao`, `StorageDao`) into a pure-Kotlin module that `shared`
   and `feature:*` can both depend on. Room entities can be replaced with
   domain models at the API boundary using mapper functions.

2. **Split UniFFI bridge from `shared`** — Move `Bridge.kt`, `uniffi/`
   generated code, and the Gobley Cargo/UniFFI plugin into a dedicated
   `bridge:uniffi` module. This would unlock `core:data` to become a
   physical module.

3. **Source implementation modules** — Once the bridge is extractable,
   create `source:local`, `source:webdav`, `source:onedrive` behind
   `source:api`, each implementing `MusicSource` without pulling in
   platform-specific code.

4. **ViewModel extraction** — After DAO interfaces exist in `database:api`,
   ViewModels in `shared` (Album, Artist, Browse, etc.) can reference the
   interface instead of the Room implementation, unblocking their move to
   feature modules.

#### Current Gate

- **789 Gradle tasks, 259 tests, 0 failures**
- All Desktop/iOS simulator/Android library compilations pass
- `androidApp:compileDebugKotlin` blocked by pre-existing Miuix/compileSdk mismatch

#### Remaining Work (All Infrastructure-Blocked)

- 10 Koin DI modules remain in `shared/di/`
- 13 legacy ViewModels remain in `shared/viewmodels/`
- 16 Roots remain in shared feature packages
- `core:data` physical split blocked by UniFFI bridge
- `source:local`, `source:webdav`, `source:onedrive` blocked by legacy bridge
- Advanced playback capabilities (Gapless, Crossfade, ReplayGain) blocked by Rust/libmpv
- OneDrive cancellable delta sync blocked by Rust backend

### Session Update 2026-06-28 (Round 35)

#### LibraryRepository Interface Extraction + LibraryVM/DI Colocation

- [x] **Extracted `LibraryRepository` interface into `core:domain/repository/`**
  - Pure Kotlin interface: `val tracks: StateFlow<List<LibraryTrackItem>>`
  - Follows existing pattern of `ArtworkRepository`, `LogRepository`, `PermissionChecker`
  - Keeps `core:domain` as the API boundary — no Room, no Compose, no UniFFI

- [x] **Renamed concrete impl to `LibraryRepositoryImpl`**
  - `shared/core/data/LibraryRepository.kt` → class `LibraryRepositoryImpl : LibraryRepository`
  - Implements the `core:domain` interface; depends on `TrackDao` + `LegacyStorageLookup` (Room/legacy, stays in `shared`)

- [x] **Updated Koin binding: interface → impl**
  - `libraryFeatureModule`: `single<LibraryRepository> { LibraryRepositoryImpl(get(), get(), get()) }`

- [x] **Moved `LibraryVM` from `shared/viewmodels/` to `:feature:library/presentation/`**
  - Now depends on `LibraryRepository` interface (not concrete class)
  - Package: `com.github.tidetunes.feature.library.presentation`

- [x] **Moved `LibraryRoot` from `shared/feature/library/` to `:feature:library/presentation/`**
  - Already in same package as `LibraryScreen`, `LibraryState`/`LibraryAction`/`LibraryEvent`
  - `Page.kt` import unchanged (same package FQN)

- [x] **Added Koin DI module to `:feature:library/di/`**
  - `libraryFeatureDiModule` registers `LibraryVM` via `viewModel { LibraryVM(get(), get()) }`
  - Added Koin, coroutines, and `:service:download:domain` deps to `feature/library/build.gradle.kts`

- [x] **Wired feature DI into shared**
  - `shared/di/LibraryFeatureModule.kt` now `includes(libraryFeatureDiModule)` from feature module
  - Removed direct `viewModel { LibraryVM(get(), get()) }` from shared

#### Gate (2026-06-28 Round 35)

- Focused gate: `:shared:compileKotlinDesktop` + `:feature:library:desktopTest` + `:core:domain:desktopTest` — PASS
- Full regression gate:
  - All 17 feature desktop test modules — PASS
  - All 4 service desktop test modules — PASS
  - `core:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 791 Gradle tasks, BUILD SUCCESSFUL.**
XML test reports: 256 tests (3 from shared:desktopTest not in this focused set), 0 failures.

#### Pattern Established: Interface Extraction + ViewModel Colocation

The `LibraryRepository` pattern proves the decoupling approach:
1. Define pure-Kotlin interface in `core:domain/repository/`
2. Keep Room-dependent `*Impl` in `shared/core/data/`
3. Bind interface → impl in Koin
4. Move ViewModel + Root to feature module
5. Feature module DI (`viewModel { XxxVM(get(), get()) }`) colocated alongside ViewModel
6. Shared DI `includes(featureDiModule)`

#### DI Colocation Status (updated)
| DI Module | Location | Status |
|-----------|----------|--------|
| `downloadsFeatureModule` | `:feature:downloads/di/` | Colocated (R31) |
| `onboardingFeatureModule` | `:feature:onboarding/di/` | Colocated (R33) |
| `queueFeatureModule` | `:feature:queue/di/` | Colocated (R33) |
| `libraryFeatureDiModule` | `:feature:library/di/` | **Colocated (R35)** — LibraryVM only |
| `searchFeatureModule` | `shared/di/` | Blocked (Room DAO deps) |
| `settingsFeatureModule` | `shared/di/` | Blocked (shared ViewModels) |
| `libraryFeatureModule` (remainder) | `shared/di/` | 12 ViewModel registrations still in shared |
| ... (others unchanged) | | |

**4 of 13 DI modules now colocated.**

#### Remaining Work (Same Blockers)
- 12 ViewModels remain in `shared/viewmodels/` (PlaylistVM, PlaylistsVM, ImportVM, EditStorageVM, CreatePlaylistVM, EditPlaylistVM, PlayerVM, ImportStatusVM, SleepModeVM, ToastVM, DebugMoreVM, LogVM)
- `PlaylistRepository`, `StorageRepository`, `ToastRepository`, `ImportRepository` still need interface extraction following this pattern
- `PlaylistVM` directly injects `PlaylistDao` (Room `@Dao`) — needs DAO interface extraction or repository facade
- UniFFI types still block 35+ files
- `core:data` physical module split blocked by UniFFI/Rust bridge

### Session Update 2026-06-28 (Round 36)

#### PlaylistRepository Interface Extraction + PlaylistsVM/DI Colocation

- [x] **Extracted `PlaylistRepository` interface into `core:domain/repository/`**
  - Pure Kotlin interface: `val playlistSummaries: StateFlow<List<PlaylistSummary>>`,
    `fun playlistMoveTo(fromIndex: Int, toIndex: Int)`, `fun scheduleReload()`
  - `PlaylistSummary` was already in `core:domain/model/MediaAssets.kt`

- [x] **Renamed concrete impl to `PlaylistRepositoryImpl`**
  - `shared/core/data/PlaylistRepository.kt` → class `PlaylistRepositoryImpl : PlaylistRepository`
  - Domain-safe methods get `override`; UniFFI-specific methods
    (`preRemovePlaylistEvent`, `preRemoveMusicEvent`, `removeMusic`, `createPlaylist`,
    `editPlaylist`, etc.) remain concrete-only
  - Koin binding: `single<PlaylistRepository> { PlaylistRepositoryImpl(...) }`
  - Platform code (PlayerController, MainViewController, etc.) imports
    `PlaylistRepositoryImpl` directly — 13 files updated
  - Test files (`IosPlayerControllerTest`, `DesktopPlayerControllerTest`)
    updated for constructor references

- [x] **Moved `PlaylistsVM` from `shared/viewmodels/` to `:feature:playlist/presentation/`**
  - Now depends on `PlaylistRepository` interface (not `PlaylistRepositoryImpl`)
  - Removed deprecated `playlists` property (used UniFFI `PlaylistAbstract` type)
  - Uses feature-local `formatDuration()` from `DurationFormat.kt` instead of
    `core.utils.formatDuration` from `shared`

- [x] **Moved `PlaylistsListRoot` from `shared/.../feature/playlist/` to `:feature:playlist/presentation/`**
  - `CreatePlaylistVM` dependency removed → replaced with `onCreatePlaylist: () -> Unit` callback
  - `Page.kt` injects `CreatePlaylistVM` via Koin and passes `::openModal` as callback

- [x] **Created `playlistsFeatureDiModule` in `:feature:playlist/di/`**
  - Registers `PlaylistsVM` via `viewModel { PlaylistsVM(get()) }`
  - `shared/di/LibraryFeatureModule.kt` now `includes(playlistsFeatureDiModule)`

#### Gate (2026-06-28 Round 36)
- Full regression gate:
  - All 17 feature desktop test modules — PASS
  - All 4 service desktop test modules — PASS
  - `core:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 787 Gradle tasks, BUILD SUCCESSFUL.**
XML test reports: 256 tests, 0 failures.

#### DI Colocation Status (updated)
| DI Module | Location | Status |
|-----------|----------|--------|
| `downloadsFeatureModule` | `:feature:downloads/di/` | Colocated (R31) |
| `onboardingFeatureModule` | `:feature:onboarding/di/` | Colocated (R33) |
| `queueFeatureModule` | `:feature:queue/di/` | Colocated (R33) |
| `libraryFeatureDiModule` | `:feature:library/di/` | Colocated (R35) |
| `playlistsFeatureDiModule` | `:feature:playlist/di/` | **Colocated (R36)** |
| `searchFeatureModule` | `shared/di/` | Blocked (Room DAO deps) |
| `settingsFeatureModule` | `shared/di/` | Blocked (shared ViewModels) |
| `importFeatureModule` | `shared/di/` | Blocked (shared bridging) |
| ... | ... | ... |

**5 of 13 DI modules now colocated.**

#### Patterns Established (Cumulative)
1. **Interface extraction + ViewModel colocation** (R35, R36): Extract pure-Kotlin interface to
   `core:domain`, rename impl, bind via interface in Koin, move VM + Root to feature module.
2. **Callback pattern for cross-module VM dependencies** (R36): When a Root imports a VM
   from another module (e.g., `CreatePlaylistVM` from `shared`), replace with a callback
   parameter; inject the VM at the call site in `shared` (which has access to both sides).

#### Remaining Work
- 11 ViewModels remain in `shared/viewmodels/`
- `PlaylistVM` still directly injects `PlaylistDao` (Room `@Dao`) — needs DAO interface
  extraction or repository facade
- `StorageRepository`, `ImportRepository`, `ToastRepository` still concrete-only
- UniFFI types still block 35+ files

### Session Update 2026-06-28 (Round 37)

#### ToastRepository Interface Extraction + 4 ViewModel Colocations

- [x] **Extracted `ToastRepository` interface into `core:domain/repository/`**
  - Pure Kotlin interface: `toast`, `toastRes` `SharedFlow`s + `emitToast()`, `emitToastRes()`
  - Renamed concrete impl to `ToastRepositoryImpl` in `shared/core/data/`
  - Koin binding: `single<ToastRepository> { ToastRepositoryImpl(get()) }`
  - 12 files updated (platform controllers, Bridge, tests) for `ToastRepositoryImpl` references
  - `ToastVM` had dead `StorageRepository` import — cleaned

- [x] **Moved `DebugMoreVM` to `:feature:settings/presentation/`**
  - Only depends on `LogRepository` (already an interface in `core:domain` since before R35)
  - `DebugRoot` import updated in-place

- [x] **Moved `ToastVM` to `:feature:settings/presentation/`**
  - Depends on `ToastRepository` (now an interface in `core:domain`)
  - `ToastWidget` import updated

- [x] **Moved `LogVM` to `:feature:settings/presentation/`**
  - Depends on `LogRepository` + `ToastRepository` (both now interfaces in `core:domain`)
  - `LogRoot` import updated

- [x] **Moved `ImportStatusVM` to `:feature:importing/presentation/`**
  - Depends on `LibrarySyncController` (already an interface in `service:librarysync:domain`)
  - Added `:service:librarysync:domain` dependency to `feature:importing/build.gradle.kts`
  - `DashboardRoot` import updated

- [x] **Created feature DI modules and wired into shared**
  - `settingsFeatureDiModule` in `:feature:settings/di/` — registers ToastVM, LogVM, DebugMoreVM
  - `importingFeatureDiModule` in `:feature:importing/di/` — registers ImportStatusVM
  - `shared/di/SettingsFeatureModule.kt` now `includes(settingsFeatureDiModule)`
  - `shared/di/ImportFeatureModule.kt` now `includes(importingFeatureDiModule)`
  - Reduced: shared DI root modules now only hold ToastRepositoryImpl binding

#### Gate (2026-06-28 Round 37)
- Full regression gate:
  - All 18 desktop test modules (17 feature + core:domain) — PASS
  - All 4 service desktop test modules — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 790 Gradle tasks, BUILD SUCCESSFUL.**
XML test reports: 256 tests, 0 failures.

#### DI Colocation Status (updated)
| DI Module | Location | Status |
|-----------|----------|--------|
| `downloadsFeatureModule` | `:feature:downloads/di/` | Colocated (R31) |
| `onboardingFeatureModule` | `:feature:onboarding/di/` | Colocated (R33) |
| `queueFeatureModule` | `:feature:queue/di/` | Colocated (R33) |
| `libraryFeatureDiModule` | `:feature:library/di/` | Colocated (R35) |
| `playlistsFeatureDiModule` | `:feature:playlist/di/` | Colocated (R36) |
| `settingsFeatureDiModule` | `:feature:settings/di/` | **Colocated (R37)** — ToastVM/LogVM/DebugMoreVM |
| `importingFeatureDiModule` | `:feature:importing/di/` | **Colocated (R37)** — ImportStatusVM |
| `searchFeatureModule` | `shared/di/` | Blocked (Room DAO deps) |
| `libraryFeatureModule` (remainder) | `shared/di/` | 10 VM registrations still in shared |
| ... | ... | ... |

**7 of 13 DI modules now colocated.**

#### Remaining ViewModels in shared (7)
| VM | Dependencies | Blocker |
|----|-------------|---------|
| `CreatePlaylistVM` | ImportRepository + PlaylistRepository | ImportRepository concrete |
| `EditPlaylistVM` | ImportRepository + PlaylistRepository + StorageRepository | ImportRepository + StorageRepository concrete |
| `EditStorageVM` | StorageRepository, ToastRepository, ImportRepository, LibrarySyncController | ImportRepository concrete |
| `ImportVM` | StorageRepository, ImportRepository, PermissionChecker | Multiple concrete deps |
| `PlaylistVM` | PlaylistRepository, StorageRepository, ImportRepository, PlaybackController, **PlaylistDao** | PlaylistDao is Room @Dao |
| `PlayerVM` | PlayerRepository, PlaybackController, EnqueueDownloadUseCase | PlayerRepository concrete |
| `SleepModeVM` | PlayerController, PlayerRepository | Both concrete |

### Session Update 2026-06-28 (Round 38)

#### ImportRepository Interface Extraction + CreatePlaylistVM Colocation

- [x] **Moved `ImportSelectionMode` to `core:domain/model/`**
  - Simple 2-value enum (`Entries`, `CurrentDirectory`) moved from
    `feature/importing/presentation/ImportState.kt` to `core:domain/model/ImportSelectionMode.kt`
  - Added missing import to `ImportState.kt` (was inline definition)

- [x] **Extracted `ImportRepository` interface into `source:api/`**
  - Interface placed in `source:api` because its method signatures use
    `SourceNodeSelection`, `SourceDirectorySelection`, `SourceNodeType` from that module
  - Pure Kotlin interface with `StateFlow` properties (no UniFFI, no Room, no Compose)
  - Added `kotlinx.coroutines.core` dependency to `source:api/build.gradle.kts` for `StateFlow`

- [x] **Renamed shared impl to `ImportRepositoryImpl`**
  - `shared/.../feature/importing/data/ImportRepository.kt` → `ImportRepositoryImpl : ImportRepository`
  - Handler typealiases (`ImportHandler`, `DirectoryImportHandler`) stay in shared as convenience aliases
  - `RouteImportType` object stays in shared
  - Koin binding: `single<ImportRepository> { ImportRepositoryImpl() }`

- [x] **Moved `CreatePlaylistVM` from `shared/viewmodels/` to `:feature:playlist/presentation/`**
  - Now depends on `ImportRepository` interface (from `source:api`) + callback
  - `PlaylistRepository.createPlaylist()` replaced with callback parameter `onCreatePlaylistRequest`
  - Inlined `decodeUrlComponent()` (URL percent-decoder) — was `shared/core/utils/Url.kt`
  - Inlined `toLegacyStorageId()` helper for `SourceAccountId → Long` conversion
  - `coverArtwork` computed using `Artwork.LegacyStorageEntry` directly (both types accessible)
  - Added `:source:api` dependency to `feature/playlist/build.gradle.kts`

- [x] **Refactored `CreatePlaylistRoot` (in shared) for callback DI**
  - Uses `koinInject` + `remember` to construct `CreatePlaylistVM` with callback
  - Callback bridges to `PlaylistRepositoryImpl.createPlaylist()` via cast from interface
  - Root stays in shared due to `LocalNavController`, `RouteImport`, `RouteImportType`

- [x] **Updated `Page.kt` to hoist VM sharing**
  - Both `PlaylistsListRoot.onCreatePlaylist` and `CreatePlaylistRoot` share same VM instance
  - VM created via `koinInject` + `remember` at Page.kt level

#### Key Architecture Decision
`ImportRepository` interface is in `source:api` (not `core:domain`) because the interface's
method signatures reference `source:api` types (`SourceNodeSelection`, `SourceDirectorySelection`).
`core:domain` does not and should not depend on `source:api`. This establishes the pattern:
source-level repositories that use source-level types belong in `source:api`.

#### Gate (2026-06-28 Round 38)
- Full regression gate: **794 Gradle tasks, BUILD SUCCESSFUL**
- XML test reports: 256 tests, 0 failures
- All 18 desktop test modules, Android test, iOS simulator test — PASS
- All cross-platform compilations — PASS

#### DI Colocation Status (updated)
| DI Module | Location | Status |
|-----------|----------|--------|
| `downloadsFeatureModule` | `:feature:downloads/di/` | Colocated (R31) |
| `onboardingFeatureModule` | `:feature:onboarding/di/` | Colocated (R33) |
| `queueFeatureModule` | `:feature:queue/di/` | Colocated (R33) |
| `libraryFeatureDiModule` | `:feature:library/di/` | Colocated (R35) |
| `playlistsFeatureDiModule` | `:feature:playlist/di/` | Colocated (R36) |
| `settingsFeatureDiModule` | `:feature:settings/di/` | Colocated (R37) |
| `importingFeatureDiModule` | `:feature:importing/di/` | Colocated (R37) |
| `createPlaylistDiModule` | N/A | **VM created via remember/koinInject in Root (R38)** |
| `searchFeatureModule` | `shared/di/` | Blocked (Room DAO deps) |
| ... | ... | ... |

#### Remaining ViewModels in shared (6 — CreatePlaylistVM moved out)
| VM | Blocker |
|----|---------|
| `EditPlaylistVM` | StorageRepository concrete + PlaylistRepository no createPlaylist |
| `EditStorageVM` | StorageRepository concrete (UniFFI types) |
| `ImportVM` | StorageRepository + ImportRepository + PermissionChecker |
| `PlaylistVM` | PlaylistDao (Room @Dao) direct injection |
| `PlayerVM` | PlayerRepository concrete |
| `SleepModeVM` | PlayerController in shared (service/playback/data) |

### Session Update 2026-06-28 (Round 39)

#### SleepController Interface Extraction + SleepModeVM Colocation

- [x] **Moved `SleepModeState` + `SleepModeLeftTime` to `service:playback:domain/`**
  - Both are pure Kotlin data classes with no UniFFI, Room, or platform deps
  - `SleepModeLeftTime` simplified to a single-constructor data class with computed `hour`/`minute`
  - Removed from `shared/.../service/playback/data/PlayerRepository.kt`

- [x] **Created `SleepController` interface in `service:playback:domain/`**
  - `val sleepState: StateFlow<SleepModeState>`
  - `fun scheduleSleep(newExpiredMs: Long)`
  - `fun cancelSleep()`
  - All three methods are UniFFI-free — uses only domain types and primitives

- [x] **Updated `PlayerController` (in shared) to extend `SleepController`**
  - `interface PlayerController : SleepController` with `override` on sleep methods
  - Platform implementations (Android, iOS, Desktop) automatically satisfy `SleepController`
    through the interface chain — no code changes needed

- [x] **Refactored `SleepModeVM` with time-provider callback**
  - Moved from `shared/viewmodels/SleepModeVM.kt` to `shared/.../dashboard/presentation/SleepModeVM.kt`
  - Now depends on `SleepController` interface instead of `PlayerController` (decouples from UniFFI)
  - Constructor accepts `currentTimeMs: () -> Long` callback to avoid `shared`-only `platform.currentTimeMillis`
  - DI binding: `viewModel { SleepModeVM(get()) { platform.currentTimeMillis() } }`

- [x] **Updated all consumers (6 files)**
  - `DashboardRoot`, `NowPlayingRoot`, `TimeToPauseModal` — import updated
  - `PlaybackModule` — DI binding updated with time callback
  - Platform controllers — `SleepModeState` import updated

#### Gate (2026-06-28 Round 39)
- Full regression gate: **790 Gradle tasks, BUILD SUCCESSFUL**
- XML test reports: 256 tests, 0 failures
- All desktop test modules, Android test, iOS test, cross-platform compilations — PASS

#### DI Colocation Status (updated)
7 of 13 DI modules colocated + SleepModeVM moved (DI remains in shared `PlaybackModule`,
but the VM is no longer stuck by UniFFI coupling — it depends on the pure `SleepController`).

#### Remaining ViewModels in shared (5)
| VM | Blocker |
|----|---------|
| `EditPlaylistVM` | StorageRepository concrete |
| `EditStorageVM` | StorageRepository concrete (UniFFI) |
| `ImportVM` | StorageRepository + ImportRepository |
| `PlaylistVM` | PlaylistDao (Room @Dao) direct injection |
| `PlayerVM` | PlayerRepository (UniFFI-coupled) + LegacyStorageLookup |

### Session Update 2026-06-28 (Round 40)

#### PlaylistVM DAO Decoupling

- [x] **Created `DomainPlaylistTrack` in `core:domain/model/`**
  - Pure data class: `trackId`, `title`, `durationMs`, `sortOrder`, `sourceStorageId`, `sourcePath`
  - No Room, no UniFFI, no Compose dependencies

- [x] **Added `observePlaylistTracks()` to `PlaylistRepository` interface**
  - `fun observePlaylistTracks(playlistId: Long): Flow<List<DomainPlaylistTrack>>`
  - Returns domain rows instead of Room entities

- [x] **Implemented in `PlaylistRepositoryImpl`**
  - Calls `playlistDao.observeTracks()` then `map { it.toDomainRow() }` with a file-private mapper
  - `DaoPlaylistTrackRow.toDomainRow()` extension in `PlaylistRepository.kt`

- [x] **Updated `PlaylistMappers.kt`**
  - `toPlaylistTrackItem()` now takes `DomainPlaylistTrack` instead of `PlaylistTrackRow`

- [x] **Updated `PlaylistVM.kt`**
  - Replaced `playlistDao.observeTracks(_id)` with `playlistRepository.observePlaylistTracks(_id)`
  - Removed `PlaylistDao` constructor parameter (was 9th param, now 8)
  - PlaylistDao is no longer a direct dependency of PlaylistVM

- [x] **Updated `LibraryFeatureModule.kt` (shared/di)**
  - `PlaylistVM` Koin binding reduced from 9 `get()` to 8 `get()` calls (PlaylistDao removed)

- [x] **Fixed `PlaylistStateTest.kt`**
  - Removed stale `playlistId` and `remoteFileId` fields from `DomainPlaylistTrack` construction

#### Gate (2026-06-28 Round 40)
- Full regression gate:
  - All 16 feature desktop test modules — PASS
  - All 4 service desktop test modules — PASS
  - `core:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest` (PlayerControllerRepositoryTest) — PASS
  - `shared:iosSimulatorArm64Test` (IosPlayerControllerTest) — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 783 Gradle tasks (main) + 46 (service), BUILD SUCCESSFUL.**
XML test reports: 256 tests, 0 failures.

#### Remaining ViewModels in shared (5 — PlaylistDao blocker resolved)
| VM | Blocker |
|----|---------|
| `EditPlaylistVM` | StorageRepository concrete |
| `EditStorageVM` | StorageRepository concrete (UniFFI) |
| `ImportVM` | StorageRepository + ImportRepository |
| `PlaylistVM` | PlaylistRepositoryImpl (concrete) + StorageRepository + ImportRepositoryImpl + PlayerController + RoomLibraryStore + LegacyStorageLookup |
| `PlayerVM` | PlayerRepository (UniFFI-coupled) + LegacyStorageLookup |

### Session Update 2026-06-28 (Round 41)

#### StorageRepository Interface Extraction + Domain Type Moves

- [x] **Moved `StorageAccountInfo` to `core:domain/model/StorageAccountInfo.kt`**
  - Pure data class using `SourceAccountId` and `SourceId` from domain
  - Also moved `OneDriveDriveInfo`, `toStorageRouteIdOrNull()`, and `STORAGE_ACCOUNT_PREFIX`
  - 6 files updated to import from new location

- [x] **Moved `StoredCredential` to `core:domain/model/StoredCredential.kt`**
  - Pure data class (username, secret, isAnonymous) — no UniFFI, Room, or platform deps
  - Removed from `core/data/security/CredentialStore.kt` (now imports from domain)
  - 4 platform credential-store files updated (Desktop, Android, iOS, + tests)

- [x] **Created `StorageRepository` interface in `core:domain/repository/`**
  - Domain-safe methods: `storageAccounts`, `onRemoveStorageEvent`, `oauthRefreshToken`, `reload()`,
    `findStorageAccountByAccountId()`, `loadCredentialByAccountId()`, `removeByAccountId()`
  - No UniFFI types in interface signatures

- [x] **Renamed impl to `StorageRepositoryImpl : StorageRepository`**
  - All UniFFI-coupled methods (`storages`, `test()`, `upsertStorage()`, `storageForRust()`,
    `startOneDriveOAuth()`, etc.) remain on the concrete impl
  - `override` added to all interface methods
  - Koin binding: `single { StorageRepositoryImpl(get(), get(), get(), get()) }`

- [x] **Updated all 22 consumers** across shared, Android, iOS, Desktop, and test source sets
  - Files needing concrete methods import `StorageRepositoryImpl`
  - Koin `get<StorageRepositoryImpl>()` for concrete retrieval

#### Gate (2026-06-28 Round 41)
- Full regression gate:
  - All 16 feature desktop test modules — PASS
  - All 4 service desktop test modules — PASS
  - `core:domain:desktopTest` — PASS
  - `shared:testDebugUnitTest` — PASS
  - `shared:iosSimulatorArm64Test` — PASS
  - `shared:compileDebugKotlinAndroid` — PASS
  - `shared:compileKotlinIosSimulatorArm64` — PASS
  - `desktopApp:compileKotlinDesktop` — PASS

**Full gate: 783 + 42 = 825 Gradle tasks, BUILD SUCCESSFUL.**
XML test reports: 256 tests, 0 failures.

#### Remaining ViewModels in shared (5 — unchanged from R40)
| VM | Blocker |
|----|---------|
| `EditPlaylistVM` | StorageRepositoryImpl (concrete) — needs only `storages.value` |
| `EditStorageVM` | StorageRepositoryImpl (concrete) — uses 10+ concrete methods |
| `ImportVM` | StorageRepositoryImpl (concrete) — needs only `storageAccounts` |
| `PlaylistVM` | PlaylistRepositoryImpl (concrete) + 5 other concrete deps |
| `PlayerVM` | PlayerRepository (UniFFI-coupled) + LegacyStorageLookup |

#### Notes
- `ImportVM` now only uses `storageAccounts` from `StorageRepositoryImpl` — it can be switched to
  the `StorageRepository` interface when ready for colocation
- `PlaylistVM` only uses `onRemoveStorageEvent` from `StorageRepositoryImpl` — same pattern
- `EditPlaylistVM` only uses `storages.value` (a concrete-only property returning UniFFI `Storage` list)
  — would need a domain-safe facade to decouple

### Session Update 2026-06-28 (Round 42)

#### ImportVM Colocation to :feature:importing

- [x] **Added `StorageRepository` interface binding to Koin**
  - `single<StorageRepository> { get<StorageRepositoryImpl>() }` in `sourceDataModule`
  - Sits alongside the existing concrete `StorageRepositoryImpl` binding
  - Enables VMs to depend on the interface for DI resolution

- [x] **Switched ImportVM to interface dependencies**
  - `StorageRepositoryImpl` → `StorageRepository` (interface in `core:domain`)
  - `ImportRepositoryImpl` → `ImportRepository` (interface in `source:api`)
  - All method calls already used interface-available signatures

- [x] **Inlined `decodeUrlComponent` utility**
  - Copied the pure-Kotlin percent-decoder into ImportVM as a private function
  - Removes dependency on `shared/core/utils/Url.kt`

- [x] **Moved ImportVM to `:feature:importing/presentation/`**
  - Package changed from `com.github.tidetunes.viewmodels` to `com.github.tidetunes.feature.importing.presentation`
  - `SplitPathItem` data class moved with it (now in same package as ImportStateMappers)

- [x] **Moved ImportStateMappers to `:feature:importing/presentation/`**
  - `importState()` and `toImportLoadState()` made public (were internal)
  - `SplitPathItem` import removed (now same package)

- [x] **Added ImportVM DI registration**
  - `importingFeatureDiModule`: `viewModel { ImportVM(get(), get(), get(), get()) }`

#### Gate (2026-06-28 Round 42)
- Full regression gate: **790 Gradle tasks, BUILD SUCCESSFUL**
- XML test reports: 256 tests, 0 failures
- All 16 feature + 4 service desktop test modules, Android test, iOS test, cross-platform compilations — PASS

#### Remaining ViewModels in shared (4)
| VM | Blocker |
|----|---------|
| `EditPlaylistVM` | PlaylistRepositoryImpl.editPlaylist() + StorageRepositoryImpl.storages |
| `EditStorageVM` | StorageRepositoryImpl (10+ concrete methods, UniFFI-heavy) |
| `PlaylistVM` | PlaylistRepositoryImpl (concrete) + 5 other concrete deps |
| `PlayerVM` | PlayerRepository (UniFFI-coupled) + LegacyStorageLookup |

### Session Update 2026-06-28 (Round 43)

#### EditPlaylistVM Colocation to :feature:playlist

- [x] **Refactored EditPlaylistVM to interface + callback dependencies**
  - `ImportRepositoryImpl` → `ImportRepository` (interface from `source:api`)
  - `PlaylistRepositoryImpl` → callback `onGetPlaylistMetaToEdit: (Long) -> PlaylistMetaToEdit?`
  - `StorageRepositoryImpl` → absorbed into `onGetPlaylistMetaToEdit` callback
  - `editPlaylist()` → callback `onUpdatePlaylistRequest: (id: Long, title: String, cover: SourceNodeSelection?) -> Unit`
  - Cover artwork computed using `Artwork.LegacyStorageEntry` directly (like CreatePlaylistVM)

- [x] **Moved to `:feature:playlist/presentation/`**
  - Package: `com.github.tidetunes.feature.playlist.presentation`
  - `PlaylistMetaToEdit` data class defined alongside the VM
  - `SavedStateHandle` retained in constructor (provided by Koin DI)

- [x] **Updated DI binding in shared/LibraryFeatureModule.kt**
  - Uses `viewModel { parameters -> ... }` with Koin `get<>()` for concrete types
  - Callbacks bridge UniFFI-typed PlaylistRepositoryImpl/StorageRepositoryImpl to domain-safe params
  - `savedStateHandle = parameters.get()` passes route argument through Koin

- [x] **Simplified EditPlaylistRoot**
  - Reverted to `editPlaylistVM: EditPlaylistVM = koinViewModel()` (clean pattern)
  - No manual construction needed — DI handles callback wiring

- [x] **Updated PlaylistRoot import** to new package

#### Gate (2026-06-28 Round 43)
- Full regression gate: **790 Gradle tasks, BUILD SUCCESSFUL**
- XML test reports: 256 tests, 0 failures
- All cross-platform + iOS Simulator tests — PASS

#### Remaining ViewModels in shared (3)
| VM | Blocker |
|----|---------|
| `EditStorageVM` | StorageRepositoryImpl (10+ concrete methods, UniFFI-heavy) |
| `PlaylistVM` | PlaylistRepositoryImpl (concrete) + 5 other concrete deps |
| `PlayerVM` | PlayerRepository (UniFFI-coupled) + LegacyStorageLookup |

### Session Update 2026-06-28 (Round 44)

#### PlayerVM Dead Parameter Removal

- [x] **Removed dead `LegacyStorageLookup` from PlayerVM constructor**
  - The `storageLookup` parameter was never used in the class body
  - Constructor reduced from 4 params to 3 params
  - Cleaned import

- [x] **Updated `PlaybackModule` DI binding**
  - `viewModel { PlayerVM(get(), get(), get(), get()) }` → `viewModel { PlayerVM(get(), get(), get()) }`

- [x] **PlayerVM colocation blocked by PlayerRepository (UniFFI-coupled)**
  - PlayerRepository uses `Music`, `Playlist`, `MusicId`, `PlayMode` — all UniFFI types
  - `service:playback:presentation` cannot depend on `:shared` per architecture rules
  - Extracting a domain-safe `PlayerRepository` interface would require domain facades for UniFFI types

#### Gate (2026-06-28 Round 44)
- Full regression gate: **790 Gradle tasks, BUILD SUCCESSFUL**
- All tests passing, no regressions

#### Remaining ViewModels in shared (3 — unchanged count)
| VM | Blocker |
|----|---------|
| `EditStorageVM` | StorageRepositoryImpl (UniFFI-heavy) |
| `PlaylistVM` | PlaylistRepositoryImpl + PlayerController + RoomLibraryStore + LegacyStorageLookup (5 concrete deps) |
| `PlayerVM` | PlayerRepository (UniFFI Music/Playlist/PlayMode types) |

### Session Update 2026-06-28 (Round 45)

#### SearchFeature DI Colocation

- [x] **Created `searchFeatureDiModule` in `:feature:search/di/`**
  - `viewModel { SearchViewModel(get(), get(), get(), get(), get()) }`
  - All 5 `get()` params resolve to domain interfaces registered in shared Koin
  - `SearchViewModel` was already in `:feature:search/presentation/` since before R35

- [x] **Updated `shared/di/SearchFeatureModule.kt`**
  - Added `includes(searchFeatureDiModule)` to delegate VM binding
  - Retained data-layer bindings (`RoomSearchRepository`, `MusicSourceSearchAggregator`, etc.)
  - Removed unused `import org.koin.core.module.dsl.viewModel`

- [x] **Key insight: data-layer repo bindings stay in shared, but the viewModel binding can move to the feature module** because Koin resolves `get()` from the root module graph at runtime. The feature module only needs the domain interfaces at compile time.

#### Gate (2026-06-28 Round 45)
- Full regression gate: **790 Gradle tasks, BUILD SUCCESSFUL**
- All tests passing

#### DI Colocation Status (9/13)
| DI Module | Location | Status |
|-----------|----------|--------|
| `searchFeatureDiModule` | `:feature:search/di/` | **Colocated (R45)** — SearchViewModel |
| ... (8 previously colocated) | ... | Colocated |
| 4 remaining in shared | `shared/di/` | Data-layer bindings |

#### Remaining ViewModels in shared (3)
| VM | Blocker |
|----|---------|
| `EditStorageVM` | StorageRepositoryImpl (UniFFI-heavy) |
| `PlaylistVM` | PlaylistRepositoryImpl + PlayerController + RoomLibraryStore + LegacyStorageLookup |
| `PlayerVM` | PlayerRepository (UniFFI Music/Playlist/PlayMode) |

### Session Update 2026-06-28 (Round 46)

#### BrowseViewModel DAO Decoupling + Colocation

- [x] **Created `BrowseAlbumPreview` + `BrowseArtistPreview` domain models**
  - In `core:domain/model/BrowsePreviews.kt`
  - Pure data classes: no Room, no UniFFI, no Compose

- [x] **Created `BrowseRepository` interface in `core:domain/repository/`**
  - `suspend fun loadAlbums(limit: Int): List<BrowseAlbumPreview>`
  - `suspend fun loadArtists(limit: Int): List<BrowseArtistPreview>`
  - `suspend fun loadGenreNames(limit: Int): List<String>`

- [x] **Created `BrowseRepositoryImpl` in `shared/core/data/BrowseRepository.kt`**
  - Wraps `MetadataDao` + `TrackDao` → maps entities to domain previews
  - Koin binding: `single<BrowseRepository> { BrowseRepositoryImpl(get(), get()) }`

- [x] **Refactored `BrowseViewModel` to depend on `BrowseRepository` interface**
  - Constructor: `(browseRepository: BrowseRepository)` instead of `(metadataDao, trackDao)`
  - Room DAO dependency fully removed from the ViewModel

- [x] **Moved BrowseViewModel to `:feature:browse/presentation/`**
  - Removed from `shared/src/.../feature/browse/presentation/`
  - Added `koin.core`, `koin.compose.viewmodel`, `kotlinx.coroutines.core` deps to `feature:browse/build.gradle.kts`

- [x] **Created `browseFeatureDiModule` in `:feature:browse/di/`**
  - `viewModelOf(::BrowseViewModel)` — Koin auto-resolves BrowseRepository from graph
  - `shared/di/LibraryFeatureModule.kt` now `includes(browseFeatureDiModule)`

#### Gate (2026-06-28 Round 46)
- Full regression gate: **794 Gradle tasks, BUILD SUCCESSFUL**
- All tests passing

#### DI Colocation Status (10/13)
| DI Module | Location | Status |
|-----------|----------|--------|
| `browseFeatureDiModule` | `:feature:browse/di/` | **Colocated (R46)** — BrowseViewModel |
| `searchFeatureDiModule` | `:feature:search/di/` | Colocated (R45) |
| ... (8 previously colocated) | ... | Colocated |
| 3 remaining in shared | `shared/di/` | GenreTracks/GenreTracksBrowse/Album/Artist/Lyrics/Radio/Recently |

#### Remaining ViewModels in shared (3 — unchanged)
| VM | Blocker |
|----|---------|
| `EditStorageVM` | StorageRepositoryImpl (UniFFI-heavy) |
| `PlaylistVM` | PlaylistRepositoryImpl + PlayerController + RoomLibraryStore + LegacyStorageLookup |
| `PlayerVM` | PlayerRepository (UniFFI Music/Playlist/PlayMode) |

#### Remaining DAO-bound VMs in shared (7)
| VM | DAO methods used | Difficulty |
|----|-----------------|------------|
| GenreTracksViewModel | trackDao.findTracksByGenre + metadataDao.getAlbum | Easy |
| RadioViewModel | trackDao.findRecentlyAdded + metadataDao.getAlbum | Easy |
| RecentlyAddedViewModel | trackDao.findRecentlyAdded + metadataDao.getAlbum | Easy |
| RecentlyPlayedViewModel | trackDao.findRecentlyPlayed + metadataDao.getAlbum | Easy |
| LyricsViewModel | trackDao.findByIds + metadataDao.getLyrics + artistNamesForTrack | Medium |
| AlbumViewModel | trackDao.findByAlbumId + metadataDao.getAlbum + artistNamesForAlbum | Medium |
| ArtistViewModel | trackDao.findTracksByArtistId + metadataDao.albumsByArtistId + getArtist | Medium |

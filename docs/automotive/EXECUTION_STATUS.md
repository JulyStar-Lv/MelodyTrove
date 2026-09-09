# Automotive Expanded implementation status

## Objective and authority

Implement phase one of the user's **TidePlayer Android Automotive Core UI** brief:
an independent `:carApp`, independent `:car:presentation`, and reuse of the existing
database, sources, settings, playback controller and complete queue implementation.
The accepted visual target is Expanded **2496 × 1080**; these are viewport pixels,
not Compose dp. VehiclePanel **1728 × 1080** is audit/extension-boundary work only.

- Source brief: user attachment `pasted-text-1.txt`, attachment ID
  `4ed4eac5-f2c1-4b04-8592-6ba798356d7c`.
- Repository baseline: local and remote `main` both
  `45bba3c510ac4ca3425891ab750ae5ad838b85ef`, verified 2026-09-07.
- Development branch: `codex/automotive-expanded`.
- Figma: https://www.figma.com/design/5NO5nvLFdd2jPTwHmnJE8A/TidePlayer-Automotive-Core-UI?node-id=0-1
- Window reference: https://github.com/JulyStar-Lv/FileManager
- Model workflow: Sol analysis/contract/review, Luna implementation under contract.

## Gates and required evidence

Every unchecked item remains unproven. A design frame, passing compilation, or a
planned test cannot substitute for device/window or end-to-end evidence.

- [x] Phase 0 source-inference gate: `CAR_WINDOW_PROFILES.md` distinguishes
  physical/window/content bounds, derives the Expanded/VehiclePanel coordinate
  relationships, and records the exact FileManager OEM-key switch logic. On
  2026-09-07 the user explicitly accepted inference from the local FileManager
  code as the implementation contract. Device density/insets remain Phase 18
  runtime acceptance evidence and are not represented as measured facts.
- [x] Phase 1: `ARCHITECTURE_DECISION.md` records exact module dependencies,
  bootstrap/database/source/playback ownership, package identity and platform risks.
- [x] Phase 2: `FIGMA_EXPANDED_MAPPING.md` maps current Figma nodes, components,
  variables, styles, states and dimensions to target Compose source paths.
- [x] Phase 3: design-system and window-profile contracts use measured constraints
  and density, with no fixed 2496dp/1080dp root or guessed device threshold.
- [x] Phase 4: minimal `carApp` and `car:presentation` are implemented and both
  APK build gates pass. Runtime launch evidence remains a separate Phase 18 gate.
- [x] Phase 5: Figma-based Car theme/components implement default, selected,
  focused, pressed, disabled and playing states, with light/dark support.
- [x] Phase 6: navigation, header/exit, Home and mini player use real repositories.
- [x] Phase 7: playback reuse contract proves complete Library queue and shared
  controller/state paths before playback UI wiring.
- [x] Phase 8: Library categories, loading/empty/error/content and playing states;
  selecting item N creates the complete Library queue with startIndex N and
  synchronizes mini player through the shared controller and player state.
- [x] Phase 9: Now Playing metadata/artwork/lyrics/progress/buffering/seek/shuffle/
  repeat observe the existing playback state; UI never owns ExoPlayer.
- [x] Phase 10: Queue selection, current highlight and automatic scroll follow
  reuse queue state and ordering.
- [x] Phase 11: functional settings/search use shared stores and independent UI.
- [x] Phase 12: `FOCUS_INPUT_CONTRACT.md` defines one Expanded focus graph for
  initial focus, cross-pane movement, internal traversal, mini player, Now Playing,
  Queue, Back, restoration, dialogs and every required input family.
- [x] Phase 13: centralized focus/input code, stable semantic IDs, touch/Enter,
  D-pad, rotary, media keys, bringIntoView, scroll follow and route/queue restoration
  compile and pass unit tests. Physical-controller verification remains Phase 18
  runtime evidence rather than a code-completion condition.
- [ ] Phase 14: visual comparison against Figma at measured Expanded content bounds;
  no system UI occlusion, overall scale or px/dp conflation.
- [x] Phase 15: architecture, ViewModel/domain, Compose and profile behavior tests
  cover every item in the user brief, including complete queue and focus restore.
  Automated coverage now includes the architecture boundaries, Home repository
  states, complete queue mapping, Search duplicate/index mapping, playback progress,
  queue identity, focus restoration, real Compose navigation focus, song-row
  interaction/playing state, Mini Player Play/Pause/Previous/Next controls, and
  Compose Queue open/close/Back behavior.
- [x] Phase 16: relevant compile/test/static checks and both APK builds pass;
  failures report command/task/file/root cause and Code/Dependency/Environment class.
- [x] Phase 17: final diff review checks every prohibited duplication/coupling,
  demo data, swallowed error, unfinished core behavior and layout/focus violation.
- [ ] Phase 18: real-library end-to-end playback sequence and complete Expanded
  window/visual/input acceptance proven by captured runtime evidence.
- [ ] Existing Android behavior, Desktop and iOS/shared regression checks completed.
- [x] `VEHICLE_PANEL_LAYOUT_NOTES.md` records future layout impacts and resolver/
  metrics extension boundaries; no complete VehiclePanel UI implemented.
- [x] Final report contains architecture, window audit, Figma mapping, playback
  graph, files changed, real commands/results, and only VehiclePanel UI as deferred.

## Current evidence

- Initial source tree was clean; no automotive implementation existed in the
  working tree at task start.
- Figma page `0:1` is accessible. Home Dark node `1:2` reports 2496 × 1080.
  This proves design size only, not device bounds or density.
- `C:/Users/Shine/AppData/Local/Android/Sdk/platform-tools/adb.exe devices -l`
  returned no attached devices on 2026-09-07. No runtime acceptance is claimed.
- The Sol/Luna FileManager audit is saved in `CAR_WINDOW_PROFILES.md`. The
  source-inference gate is accepted: state `0` maps to Expanded, state `-1` maps
  to VehiclePanel, and the inferred coordinate relationships are 2496 within a
  2560 canvas and `2560 - (760 + 72) = 1728`. Physical/window dimensions,
  density and insets remain unmeasured and are deferred to runtime acceptance.
- The Phase 1 architecture decision is complete and verified in
  `ARCHITECTURE_DECISION.md`. It requires a minimal `:core:runtime` extraction
  before introducing either Car module. Stage A implementation is in progress.
- The Figma Expanded mapping and design-system/focus contracts are complete and
  verified in `FIGMA_EXPANDED_MAPPING.md` and `DESIGN_SYSTEM_CONTRACT.md`.
  Ten Expanded screen families have paired Dark/Light design-context evidence.
  The Figma file contains no dedicated Search results, Dialog, or page-level
  Loading/Empty/Error frames; those remain explicit design-contract gaps.
- Android baseline command, run with process-local
  `JAVA_HOME=C:/Software/Android Studio/jbr` (JDK 21.0.10):
  `./gradlew.bat :androidApp:assembleDebug --console=plain`.
  Result: **BUILD SUCCESSFUL in 18m 37s**, 1044 actionable tasks (592 executed,
  452 up-to-date). Log: `build/automotive-baseline-build.log`.
  This is the pre-implementation baseline, not proof of automotive delivery or
  post-change regression safety. The command's session subsequently returned exit 0.
- Windows reports that existing iOS `audioProcessingTap` Cinterop targets cannot
  be cross-compiled on this host. No iOS success is inferred from the Android build.
- Runtime Slice 0 initially configured Cargo/UniFFI in both `:shared` and the empty
  `:core:runtime`. Gradle 8.14 correctly rejected the two tasks claiming the same
  `rust-libs/target/.../libapp_backend.so` output. The corrected contract keeps
  `:shared` as the exclusive Cargo owner until bindings move atomically to runtime.
- Runtime Slice 0 corrected verification command compiled `:core:runtime` Common,
  Desktop, and Android plus `:shared` Desktop and Android with no source moves:
  **BUILD SUCCESSFUL in 5m 44s**, 846 actionable tasks (96 executed, 750 up-to-date).
- Runtime Slice 1 moved the neutral Home listening-statistics and Search contracts
  into `:core:domain`, removed Compose bitmap handling from `AssetRepository`,
  separated platform presentation declarations, and split runtime/presentation Koin
  declarations without changing the existing `appModule` composition. The mandatory
  static scans return no forbidden Compose, feature, core-presentation, or playback-
  presentation imports in the intended runtime candidates.
- Slice 1 Home/Search Android and Desktop tests pass. The complete `allTests` command
  reached and passed `:core:domain:allTests`, `:feature:home:allTests`, and
  `:feature:search:allTests`, then stopped at the existing iOS Rust target because
  Windows has no Apple `xcrun`/`clang`. The Windows-target suite ran 411 Android tests
  and 555 Desktop tests; the directly changed Koin tests pass. Two Android Compose UI
  tests cannot initialize because `android.os.Build.FINGERPRINT` is null outside a
  Robolectric runner. One unrelated Desktop download-finalization integration assertion
  remains reproducibly red; no download/finalizer source was changed in Slice 1.
- Runtime Slice 2 moves Room ownership and all 24 versioned schemas, DataStore and
  credential stores, migrations, diagnostics, bridge primitives, app-version/time
  platform APIs, and the minimum non-UI dependency closure into `:core:runtime`.
  Cargo/UniFFI, generated build information, Room KSP, and schema generation now
  have a single owner in `:core:runtime`; `:shared` consumes that module and keeps
  only platform presentation actuals. Schema files are byte-identical to `HEAD`,
  `APP_DATABASE_VERSION` remains 25, and static scans find no Compose, Mobile
  feature, core-presentation, playback-presentation, or `:shared` dependency edge
  in runtime. Runtime Desktop and Android main compilation plus
  `:core:runtime:desktopTest :core:runtime:testDebugUnitTest` pass. The downstream
  `:shared:compileTestKotlinDesktop :shared:compileDebugUnitTestKotlinAndroid`
  test-source compilation also passes. Windows cannot compile the moved iOS Rust
  Cinterop, so iOS verification remains a macOS gate.
- Runtime Slice 3 moves source/storage and remote-server orchestration, importing,
  metadata, plugin execution/install/non-UI management, audio DSP mapping/monitoring,
  library repositories, Home statistics data, and Search data into `:core:runtime`.
  The Mobile-only playlist editor, artwork image loader/bitmap adapter, Home history,
  and manual-metadata presentation service remain in `:shared`; the service now calls
  runtime-owned album/lyrics persistence helpers. Runtime has no Compose, presentation,
  `:shared`, or feature-module dependency edge. Its retained `feature.home.data` and
  `feature.search.data` package names contain only UI-neutral implementations and do
  not import feature presentation code. Android and Desktop main compilation, shared
  Android/Desktop main compilation, and downstream shared test-source compilation
  pass. Runtime behavior verification ran 238 Android unit tests and 353 Desktop tests
  with zero failures (one environment-dependent Desktop smoke test skipped). The Room
  integration fixtures now cancel and join repository scopes before closing their
  databases, preventing closed-statement exceptions from leaking into later tests.
- Runtime Slice 4 moves download, library-sync, playback, all platform scheduler/
  controller implementations, Android Media3 service/session ownership, notification
  resources, and the iOS audio-processing cinterop into `:core:runtime`. The platform
  `expect`/`actual` Koin module now moves atomically with those implementations.
  `SourceDataModule.kt` and `LibrarySyncModule.kt` also move as a dependency-closure
  amendment so their internal implementations stay internal. `PlaybackService`
  resolves the current package launcher through `PackageManager` and safely omits a
  session activity when no launcher exists; it contains no reflected Mobile activity.
  Runtime source/build scans contain no Compose, Mobile feature, `:shared`, core-
  presentation, or playback-presentation edge, and the Android tree retains one
  `ExoPlayer.Builder` and one `MediaLibrarySession.Builder` owner. Runtime verification
  passes 342 Android tests and 479 Desktop tests with zero failures; one environment-
  dependent Desktop smoke test is skipped. The Desktop playback tests now await
  persisted queue mode and repository initialization before asserting completion or
  closing Room, eliminating test-order races. The Rust metadata writer reuses the open
  temporary-file handle on Windows and relies on `ReplaceFileW` write-through instead
  of attempting a directory `fsync`; all 11 writer integration tests pass. Runtime and
  shared Android/Desktop main compilation plus downstream shared test-source
  compilation pass. Because the Android and Desktop entry points directly reference
  runtime-owned diagnostics, storage, playback, and platform types, both application
  modules now declare their runtime dependency explicitly. The post-move
  `:androidApp:assembleDebug :desktopApp:compileKotlinDesktop` regression command is
  **BUILD SUCCESSFUL in 3m 56s**, 1077 actionable tasks (167 executed, 910 up-to-date).
  Windows still cannot compile the moved Apple cinterop, so iOS verification remains
  a macOS gate.
- Runtime Slice 5 completes the bootstrap cutover. `runtimeModules` in
  `:core:runtime` is now the sole composition list for platform, persistence,
  source, download, library-sync, library, Search-data, Home-statistics and
  playback-runtime providers. `initKoin` starts that graph and accepts explicit
  additional modules; it has no knowledge of Mobile presentation. The existing
  Android, Desktop and iOS entry points add the presentation-only `appModule`, so
  each process still starts one Koin graph and preserves `AppInitializer` ordering.
  Runtime source/build scans contain no Compose, Mobile feature, `:shared`, core-
  presentation or playback-presentation edge. Android/Desktop runtime and shared
  main plus test-source compilation pass. The moved `AppInitializerTest` remains
  beside its internal implementation; affected Library/Search Koin composition
  tests pass on Android and Desktop. The complete shared Android test task still
  has the two previously recorded Compose navigation failures caused by a null
  `android.os.Build.FINGERPRINT` outside Robolectric; runtime tests complete without
  a Slice 5 failure. The post-cutover
  `:androidApp:assembleDebug :desktopApp:compileKotlinDesktop` regression command is
  **BUILD SUCCESSFUL in 3m 33s**, 1076 actionable tasks (74 executed, 1002
  up-to-date). Apple framework verification remains a macOS gate.
- `:car:presentation` now contains the Phase 4 theme, layout-profile, navigation,
  and focus seams. Its six resolver/OEM/inset tests pass, and
  `:car:presentation:testDebugUnitTest :car:presentation:assembleDebug` is
  **BUILD SUCCESSFUL**.
- `:carApp` is now an independent Android application with production ID
  `io.github.julystar.musicapp.car` and debug ID suffix `.debug`. It depends on
  `:core:runtime`, `:core:domain`, `:service:playback:domain`, and
  `:car:presentation`; static checks find no `:shared`, `:androidApp`, Mobile
  feature, core-presentation, or playback-presentation edge/import. Its
  `CarApplication` composes `runtimeModules + carPresentationModule` and calls the
  same `AppInitializer` bridge/repository sequence. Its Activity resolves layout
  metrics from live Compose constraints and attaches Media3 to the runtime-owned
  `PlayerControllerRepository`; it owns no player or queue state.
- The merged Car debug manifest resolves package
  `io.github.julystar.musicapp.car.debug`, declares required Automotive hardware,
  one exported launcher Activity and exactly one runtime `PlaybackService` with
  both media-library browse actions. The two ABI APKs contain their matching
  `libapp_backend.so`. After adding the required direct `:core:domain` API edge,
  `:carApp:assembleDebug` is **BUILD SUCCESSFUL in 1m 55s**, 425 actionable tasks
  (25 executed, 400 up-to-date). The final combined
  `:carApp:assembleDebug :androidApp:assembleDebug` gate is **BUILD SUCCESSFUL in
  1m 26s**, 1005 actionable tasks (41 executed, 964 up-to-date). No device is
  attached, so Activity launch and media-session connection are not claimed yet.
- Read-only ADB evidence collection has a Sol contract in the window audit. No device
  is attached, so density, inset, and real content-bound evidence remains unclaimed.
- Phase 5–11 implementation now includes an Expanded shell with the exact Home and
  Now Playing/Queue Figma geometry expressed through named window metrics, exact
  exported Figma vector paths for the implemented controls, semantic light/dark
  tokens, orthogonal focus/state visuals, real library/home/artwork/settings/search
  repositories, complete-Library playback selection, shared mini-player state,
  Now Playing lyrics/progress/seek/repeat controls, and the shared queue with current
  highlight and scroll following. `PLAYBACK_REUSE_CONTRACT.md` records the ownership
  and action graph; `SEARCH_AND_PAGE_STATE_CONTRACT.md` closes the explicitly missing
  Figma contract for Search and non-modal page states. Album, artist and playlist
  details use real repositories and expose loading, empty, error/retry and content
  states. A shared playback-request mapper preserves the complete selected list and
  start index; its unit tests cover order and invalid indices. Now Playing includes
  live shuffle and buffered-position presentation.
- The first `:carApp:lintDebug` run failed in `carApp/src/main/AndroidManifest.xml`
  because the exported media service did not declare
  `android.media.action.MEDIA_PLAY_FROM_SEARCH` (Code). The action was added and the
  obsolete Android Auto projection metadata was replaced with the AAOS
  `com.android.automotive` declaration. The corrected lint run is **BUILD SUCCESSFUL
  in 11m 28s**, 728 actionable tasks, with zero errors and two warnings: target SDK
  34 is older than the installed latest SDK, and the exported media browser service
  intentionally has no restrictive permission so Automotive hosts can discover it.
- The final combined command
  `:car:presentation:testDebugUnitTest :carApp:assembleDebug :androidApp:assembleDebug`
  is **BUILD SUCCESSFUL in 7m 18s**, 1010 actionable tasks (656 executed, 354
  up-to-date). Static scans still find one `ExoPlayer.Builder` and one
  `MediaLibrarySession.Builder`, both in runtime, and no Car dependency/import edge
  to `:shared` or Mobile presentation modules.
- The only installed AVD is `Pixel_10_Pro`, not AAOS. A launch attempt remained ADB
  `offline` for more than four minutes and was stopped. It is not runtime or window
  acceptance evidence.
- Phase 12 is recorded in `FOCUS_INPUT_CONTRACT.md`. FileManager contributes the
  accepted window-profile inference but has no reusable global focus graph; its UI
  relies on classic View focus and local Activity/widget key handlers. TidePlayer's
  centralized Compose coordinator now assigns stable semantic focus IDs, restores
  focus per route, falls back when a remembered target is absent, links Navigation,
  Content, Mini Player, Now Playing and Queue, calls `bringIntoView` for focused lazy
  items, and routes D-pad, rotary and foreground media keys. Queue opening focuses
  the current row and Queue Back restores the Queue button. Media Stop preserves the
  runtime's documented resumable-pause semantics. The post-change
  `:car:presentation:testDebugUnitTest :carApp:assembleDebug` command is **BUILD
  SUCCESSFUL in 13s**, 426 actionable tasks (24 executed, 402 up-to-date); 13 Car
  unit tests report zero failures/errors. AAOS hardware verification is not claimed.
- Final review found that `LibraryRepository` could leave consumers indefinitely in
  loading when an upstream Room flow failed. It now exposes a shared `loadError`
  state; the Car Home, Songs, Albums and Artists pages render that real error state.
  Cancellation still propagates and no exception is converted into success. The
  combined `:core:runtime:testDebugUnitTest :car:presentation:testDebugUnitTest
  :carApp:assembleDebug :androidApp:assembleDebug` regression is **BUILD SUCCESSFUL
  in 5m 37s**, 1019 actionable tasks (117 executed, 902 up-to-date). Runtime reports
  343 tests and Car reports 13 tests, with zero failures/errors.
- Final Figma structure review replaced the Album and Artist single-row carousels
  with four-column lazy grids and changed Settings from a single list into the
  mapped 560/1424 two-pane structure. The focused category links to the first detail
  preference, and detail preferences link back to the category. The latest
  `:car:presentation:testDebugUnitTest :carApp:assembleDebug` command is **BUILD
  SUCCESSFUL in 1m 46s**, 430 actionable tasks (28 executed, 402 up-to-date).
  The corresponding final `:carApp:lintDebug` run is **BUILD SUCCESSFUL in 2m
  3s**, 728 actionable tasks (35 executed, 693 up-to-date), with zero errors and
  the same two documented warnings.
- Final capability review removed synthetic Home recommendations. Home now obtains
  recently added, recently played and favorites from the real shared repositories,
  exposes independent loading/empty/error states, preserves each visible section as
  the playback queue, and links real Album/Artist/Playlist cards to their detail
  routes. “猜你喜欢” and “每日推荐” are disabled and explicitly reported as unsupported
  until a recommendation contract exists. Search playback now derives the selected
  queue index from the clicked result position, so filtered and duplicate results do
  not start the wrong occurrence. The unsupported Now Playing “More” action is also
  disabled, and the Settings category action moves focus into its detail pane.
- Robolectric 4.16 local Compose tests now exercise initial navigation focus and
  D-pad traversal, enabled/disabled song-row clicks, playing-state semantics, Mini
  Player open and Previous/Play/Pause/Next callbacks, its empty disabled state, and
  Now Playing Queue open/close/system-Back behavior. The Automotive suite reports
  **34 tests, zero failures/errors**. The broad final command
  `:core:runtime:testDebugUnitTest :car:presentation:testDebugUnitTest
  :carApp:assembleDebug :androidApp:assembleDebug :desktopApp:compileKotlinDesktop`
  is **BUILD SUCCESSFUL in 9m 49s**, 1153 actionable tasks; runtime reports 343
  tests and the pre-Queue Car run reports 31. The post-Queue Car-only suite is
  **BUILD SUCCESSFUL in 57s** with 34 tests. The final `:carApp:lintDebug` is **BUILD SUCCESSFUL in
  5m 23s**, 728 tasks, zero errors and the same two documented warnings.
  `:car:presentation:lintDebug` is **BUILD SUCCESSFUL in 46s**, zero errors and
  four existing Compose modifier-parameter ordering warnings.

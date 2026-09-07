# TidePlayer Automotive Phase 1 Architecture Decision

Status: **accepted for phased implementation** on 2026-09-07. Phase 0 was explicitly skipped by the user; it did not pass. `CAR_WINDOW_PROFILES.md` still contains unresolved window, density, inset, and transition evidence. This decision authorizes the module/runtime skeleton, not guessed layout thresholds or a claim that 2496 × 1080 is Compose dp.

Repository baseline: `45bba3c510ac4ca3425891ab750ae5ad838b85ef` (`main`). Development branch: `codex/automotive-expanded`.

## Decision

1. Add independent Gradle modules `:carApp` and `:car:presentation`. `:carApp` is an Android application; `:car:presentation` owns the complete Automotive Compose root, navigation, theme, layout profiles, focus behavior, and Car-specific view models/state holders.
2. Add `:core:runtime`. It is necessary because the current `:shared` module is both the implementation/bootstrap container and the aggregate for every Mobile/Desktop presentation module. Making `carApp` depend directly on `shared` would package the Mobile screens and fail the requested boundary even if Car source code never imports a screen.
3. Keep the runtime extraction minimal. Move existing implementation files; do not rewrite the Room schema, repositories, source engines, queue engine, or playback algorithms. Extract only the transitive implementation and DI closure needed to construct Library, Sources, Settings, Search, download/library sync, and Playback. `shared` remains the existing Mobile/Desktop/iOS presentation shell and depends on `core:runtime`.
4. `car:presentation` must not depend on `:shared`, any `:feature:*` presentation module, or `:service:playback:presentation` as a whole. It consumes domain contracts from `:core:domain`, `:service:playback:domain`, `:service:librarysync:domain`, and `:source:api`.
5. `:core:presentation` is not categorically forbidden. It contains reusable primitives and assets, but also the existing app theme/navigation and phone/desktop-shaped components. Car may reuse a reviewed primitive such as the byte-backed artwork loader contract or a neutral formatter. If Gradle-level dependency on `:core:presentation` is introduced, Sol must first verify that it does not make Car use `MusicGraph`, the existing `Theme`, system-bar policy, or Mobile player chrome. Default Phase 4 does not need this dependency.
6. `:service:playback:presentation` contains useful state and mappers, but it also contains `MiniPlayer`, `NowPlayingRoot`, `NowPlayingScreen`, `PlaybackShell`, and navigation. Car must not depend on that module. Move only a demonstrably UI-neutral state/mapper into `:service:playback:domain` if reuse is valuable; otherwise Car observes `PlaybackController` directly. Do not copy or import its screens.
7. Koin is split into runtime and presentation composition. Both applications create one Koin graph per process from common runtime modules, the Android runtime module, and their own presentation module. `androidApp` adds Mobile presentation modules; `carApp` adds `carPresentationModule`.
8. Keep one `PlaybackController`, one `PlayerRepository`, one `PlaybackService`, one `ExoPlayer`, and one `MediaLibrarySession` **inside each running application process**. `PlaybackService` remains the only Android owner of `ExoPlayer`/`MediaLibrarySession`; neither Activity nor Car composables instantiate a player.
9. Use `applicationId = "io.github.julystar.musicapp.car"` for the independent Car APK, with debug suffix `.debug`. This allows both APKs to be installed. The same Room/DataStore/credential implementation is compiled into both APKs, but Android gives each application ID a different private sandbox. They do **not** use the same physical `library.db`, DataStore file, credential preferences, Koin singleton, queue, or live player. Content synchronization would require an explicit account/server/export-import design and is outside this architecture step.
10. Treat the custom Compose Activity and the platform media surface as two clients of the same playback runtime:
    - the embedded AAOS `MainActivity` renders TidePlayer's custom UI;
    - `MediaLibraryService`/`MediaLibrarySession` exposes browse, notification, Assistant, Android Auto/AAOS media-center, Bluetooth, and hardware media-key behavior;
    - the platform-rendered media UI is not the custom Activity, and `automotive_app_desc.xml` does not create the custom UI.
11. Phase 4 may proceed because the user explicitly skipped Phase 0. The resolver may define `Expanded` and reserve `VehiclePanel`, but must accept injected/measured constraints. It must not encode `2496.dp`, `1728.dp`, `1080.dp`, `widthPx == 2496`, a guessed density, guessed Insets, or a guessed switch threshold.

### Target dependency graph

```text
:androidApp ---------------------> :shared
                                      |
                                      +--> Mobile/Desktop presentation modules
                                      +--> :core:runtime

:carApp ------------------------> :car:presentation
       \------------------------> :core:runtime

:car:presentation -------------> :core:domain
              |-----------------> :service:playback:domain
              |-----------------> :service:librarysync:domain   (only where needed)
              \-----------------> :source:api                   (only where needed)

:core:runtime ------------------> :core:domain, :core:data
             |------------------> :service:playback:domain
             |------------------> :service:download:{domain,data}
             |------------------> :service:librarysync:{domain,data}
             \------------------> :source:{api,local,webdav,onedrive,smb,openlist,server}

Forbidden edges:
:carApp -X-> :androidApp
:car:presentation -X-> :shared
:car:presentation -X-> feature/*/presentation
:car:presentation -X-> :service:playback:presentation
:core:runtime -X-> :core:presentation or any presentation module
```

## Evidence

| Finding | Source evidence |
|---|---|
| No Car modules exist | `settings.gradle.kts:24-62` lists all modules and ends with `:androidApp` and `:desktopApp`. |
| `shared` aggregates runtime and all UI | `shared/build.gradle.kts:91-145` depends on core data/domain/presentation, all source modules, playback domain/presentation, and all feature modules plus Compose/Koin UI libraries. |
| Existing app graph mixes data and presentation | `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/AppModule.kt:8-23` includes platform, data, source, playback, download/sync, and all feature modules in one `appModule`. |
| Koin bootstrap has no graph-selection seam | `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/Koin.kt:7-13` always starts `appModule`. |
| Android process initializes the shared graph | `androidApp/src/main/java/io/github/julystar/musicapp/AppApplication.kt:25-48` assigns Android context, calls `initKoin`, initializes the bridge, and reloads repositories. |
| Startup ownership is already cross-platform | `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/AppInitializer.kt:32-48` describes and implements common initialization; lines `55-97` load Settings, Room, migration, Rust bridge, and plugin sources; lines `112-135` recover sync/download/playback and reload storage/playlist. |
| Room is a single Koin singleton per graph | `CoreDataModule.kt:71-88` binds one coroutine scope, `buildDatabase()`, its DAOs, and DataStore; lines `89-120` bind Settings, maintenance, credentials, bridge, and Library store. |
| Database schema is reusable and UI-neutral | `shared/src/commonMain/kotlin/io/github/julystar/musicapp/database/AppDatabase.kt:10-42` defines Room entities and `AppDatabase`; lines `61-97` show version 25 and the migration chain. |
| Android DB is package-private storage | `shared/src/androidMain/kotlin/io/github/julystar/musicapp/database/DatabaseBuilder.kt:9-16` resolves `context.getDatabasePath(...)`; `AppIdentifiers.kt:9-10` names `library.db` and `settings.preferences_pb`. |
| Settings is package-private DataStore | `shared/src/androidMain/kotlin/io/github/julystar/musicapp/core/data/datastore/AppDataStore.android.kt:9-13` stores preferences under the current application's `filesDir`. |
| Credentials are also process/package scoped | `AndroidCredentialStore.kt:20-28` uses Android Keystore plus current app private SharedPreferences. A second application ID cannot silently share these values. |
| Source construction is already centralized | `SourceDataModule.kt:69-116` binds account material, auth/session, repository and playback resolvers; lines `191-234` creates all concrete sources and one `MusicSourceRegistry`; lines `235-242` creates metadata/scanner/import coordination. |
| Domain playback contract is UI-neutral and complete enough | `PlaybackController.kt:5-27` exposes `state`, `position`, `queue`, complete-list `play(items,startIndex)`, transport, seek, shuffle/repeat, queue move/remove/clear. `PlaybackModels.kt:5-21` defines `PlayableItem`; lines `55-91` define queue/current index behavior. |
| Existing Library click preserves full queue | `feature/library/.../LibraryRoot.kt:50-61` maps all `state.tracks`, finds the selected index, and calls `playbackController.play(items,startIndex)`; lines `70-78` assign the synthetic Library playlist ID. `LibraryPlayback.kt:3-9` reserves `-1` as in-memory Library playback context. |
| Queue UI already observes the same controller | `feature/queue/.../QueueViewModel.kt:18-48` combines controller queue/state/favorites; lines `55-82` delegates play, move, remove, and clear to `PlaybackController`. Car must reproduce this contract, not import the screen. |
| Playback graph currently mixes presentation | `PlaybackModule.kt:32-34` includes `playbackPresentationModule`; lines `55-89` construct the actual source repository, `PlayerRepository`, `LegacyPlaybackController`, now-playing repository and playlist sync. The include must be split without changing implementations. |
| Android Player/MediaSession ownership is correct | `PlaybackService.kt:82-103` injects the existing repositories/controller; lines `106-157` build the sole `ExoPlayer`; lines `195-206` wrap it and build the sole `MediaLibrarySession`; lines `548-566` release player/session/resources. |
| Media3 queue is a bounded projection, not the app queue | `AndroidMedia3Queue.kt:24-38` states the full queue remains in `Playlist`; lines `41-73` projects at most 101 nearby items. Car Queue must observe `PlaybackController.queue`, not the Media3 timeline. |
| Platform browse currently exposes only current queue | `PlaybackService.kt:327-383` returns root/current-queue children. A platform host does not receive the full TidePlayer Home/Library IA from the custom Activity. |
| Service has a Mobile Activity coupling bug | `PlaybackService.kt:123-133` reflects `io.github.julystar.musicapp.MainActivity` for notification content intent. This fails the independent Car shell unless replaced by a current-package launcher intent or an app-provided intent factory. |
| Existing Android manifest is media/projection oriented | `androidApp/src/main/AndroidManifest.xml:28-45` declares Google car metadata and `MediaLibraryService`; lines `47-72` declare the existing launcher Activity. It does not declare `android.hardware.type.automotive`. |
| Existing application identity | `androidApp/build.gradle.kts:22-35` defines namespace and `applicationId = io.github.julystar.musicapp`; lines `56-59` append `.debug`. Car therefore needs a distinct ID for co-installation. |
| Presentation modules are not interchangeable with domain | `core/presentation/build.gradle.kts:24-43` brings Compose/Miuix/Koin UI and Android activity APIs. `service/playback/presentation/...` contains `MiniPlayer.kt`, `NowPlayingRoot.kt`, `NowPlayingScreen.kt`, `PlaybackShell.kt`, and navigation alongside `PlayerVM` and state/mappers. |

Official platform references used to interpret the manifest/service split:

- Android Media3 recommends placing Player and MediaSession in a `MediaSessionService`/`MediaLibraryService`, allowing external clients to control playback without accessing the Activity: <https://developer.android.com/media/media3/session/background-playback>
- AAOS distribution requires the Automotive hardware feature declaration appropriate to the selected track: <https://developer.android.com/training/cars/platforms/automotive-os>
- Android for Cars uses `MediaLibraryService` and `MediaLibrarySession` for the platform-provided driver-safe media surface: <https://developer.android.com/media/implement/surfaces/cars>

Missing evidence: there is no connected ADB device; physical display, actual Activity window, Compose content bounds, density, Insets, and the 2496-to-1728 trigger are unverified. Phase 0 was skipped by explicit user direction, not passed.

## Affected Modules

| Module | Decision |
|---|---|
| `:carApp` | New Android application. Owns manifest, `Application`, Activity, resources, and app-level MediaController attachment. |
| `:car:presentation` | New independent Automotive Compose presentation. Owns Car screens and Car state holders. |
| `:core:runtime` | New UI-neutral implementation/bootstrap module. Holds the minimum moved runtime closure and runtime Koin modules. |
| `:shared` | Retained as existing platform presentation shell; imports runtime bindings and continues to aggregate Mobile/Desktop/iOS UI. |
| `:core:domain` | Reused unchanged in Phase 4. Only move genuinely neutral feature contracts here if required to remove a runtime-to-feature-presentation edge. |
| `:core:data` | Reused. Do not duplicate its contracts/implementations for Car. |
| `:core:presentation` | Optional reviewed primitive/resource reuse; not part of Phase 4 minimum. Never reuse its theme/navigation as the Car root. |
| `:service:playback:domain` | Reused as Car playback/queue contract. |
| `:service:playback:presentation` | Kept for existing UI. No Car module dependency. Neutral state/mappers may be extracted later, never copied. |
| `:service:download:*`, `:service:librarysync:*` | Reused by runtime. No Car-specific duplicate service/repository. |
| `:source:*` | Reused by runtime. One source registry per application process. |
| `:feature:*` | Existing presentation remains unchanged. Car does not import feature Screens/Roots/ViewModels. |
| `:androidApp`, `:desktopApp`, iOS framework | Existing entry points remain behaviorally unchanged and are regression gates for runtime extraction. |

## Affected Files

Only this decision document is changed by the audit. The following is the exact Phase 4 implementation file contract.

### Create

- `core/runtime/build.gradle.kts`
- `core/runtime/src/commonMain/kotlin/io/github/julystar/musicapp/runtime/RuntimeModules.kt`
- `core/runtime/src/commonMain/kotlin/io/github/julystar/musicapp/runtime/RuntimeInitializer.kt` (or move `AppInitializer.kt` here while retaining its public FQN)
- `core/runtime/src/androidMain/kotlin/io/github/julystar/musicapp/runtime/AndroidRuntimeModule.kt`
- `core/runtime/src/androidMain/res/drawable/notification_small_icon.xml`
- `core/runtime/src/androidMain/res/drawable/icon_mode_list.xml`
- `core/runtime/src/androidMain/res/values/strings.xml`
- `core/runtime/src/androidMain/res/values-zh-rCN/strings.xml`
- `car/presentation/build.gradle.kts`
- `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/CarRoot.kt`
- `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/di/CarPresentationModule.kt`
- `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/layout/CarLayoutProfile.kt`
- `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/layout/CarLayoutMetrics.kt`
- `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/layout/CarLayoutProfileResolver.kt`
- `car/presentation/src/test/kotlin/io/github/julystar/musicapp/car/presentation/layout/CarLayoutProfileResolverTest.kt`
- `carApp/build.gradle.kts`
- `carApp/proguard-rules.pro`
- `carApp/src/main/AndroidManifest.xml`
- `carApp/src/main/java/io/github/julystar/musicapp/car/CarApplication.kt`
- `carApp/src/main/java/io/github/julystar/musicapp/car/MainActivity.kt`
- `carApp/src/main/res/values/strings.xml`
- `carApp/src/main/res/values/themes.xml`
- required launcher icon resources under `carApp/src/main/res/mipmap-*` and `drawable*` (reuse/copy branding assets only; no Mobile screen code)

### Modify

- `settings.gradle.kts`: include `:core:runtime`, `:car:presentation`, and `:carApp`.
- `shared/build.gradle.kts`: depend on `:core:runtime`; stop compiling files moved to runtime. Preserve its existing presentation dependencies.
- `androidApp/build.gradle.kts`: depend on `:core:runtime` explicitly if needed by manifest/entry-point code; preserve application ID.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/Koin.kt`: accept an explicit module list/additional modules instead of always loading mixed `appModule`.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/AppModule.kt`: become Mobile presentation composition only, or be replaced by `mobilePresentationModules`.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/PlaybackModule.kt`: remove `includes(playbackPresentationModule)`; runtime construction and Mobile presentation bindings must be separate.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/LibraryFeatureModule.kt`: split repository bindings from feature presentation includes and `ArtworkImageLoader` binding.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/SearchFeatureModule.kt`: split `RoomSearchRepository`/source-account bindings from `searchFeatureDiModule`.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/SettingsFeatureModule.kt`: keep `corePresentationModule` and settings ViewModel bindings on the Mobile side.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/di/CoreDataModule.kt`, `SourceDataModule.kt`, `DownloadModule.kt`, `LibrarySyncModule.kt`, and Android `PlatformModule.kt`: move their runtime bindings to `core:runtime` without changing provider behavior.
- `shared/src/androidMain/kotlin/io/github/julystar/musicapp/core/PlaybackService.kt`: move to runtime and replace the reflected Mobile `MainActivity` with `packageManager.getLaunchIntentForPackage(packageName)` or an injected app launch-intent factory. Do not change queue/session semantics.
- `androidApp/src/main/java/io/github/julystar/musicapp/AppApplication.kt`: compose runtime + Mobile presentation modules.
- `androidApp/src/main/AndroidManifest.xml`: reference the runtime-owned service FQN if the package changes; do not otherwise turn the Mobile APK into `carApp`.

### Move, preserving package/API and behavior

Move the minimum runtime transitive closure from `shared` into `core:runtime`. Use these exact source roots, with the listed exclusions:

- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/database/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/migration/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/core/audio/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/core/data/**`, excluding `media/RepositoryArtworkImageLoader.kt`; remove Compose-only bitmap methods from `media/AssetRepository.kt` if it enters runtime, leaving byte loading intact.
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/service/playback/data/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/source/storage/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/source/server/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/singleton/Bridge.kt` and `RoomLibraryStore.kt`
- runtime DI/initializer files listed in the Modify section
- Android counterparts for database, DataStore, credential store, network state, playback data/service/session/queue, download scheduler, `PlayerControllerRepository`, and runtime platform functions.

Do not blindly move all of `shared`. Diagnostics/plugin/import code enters `core:runtime` only when required by the retained startup graph. Compose files found under `Root.kt`, `navigation/**`, `widgets/**`, `diagnostics/SafeModeScreen.kt`, `plugin/management/*Root.kt`, dialogs, locale composition, back handlers, and image loaders remain in `shared`.

If a moved runtime file imports a type from a feature module that packages presentation (current examples: Home statistics contracts, Search repository contracts, playlist edit contracts), move that small UI-neutral contract to `core:domain` and update imports. Do not add the whole feature module as a runtime dependency and do not rewrite the implementation.

## Files Not To Modify

Phase 4 must not edit or copy:

- `feature/*/src/**/presentation/**`
- `service/playback/presentation/src/**/miniplayer/**`
- `service/playback/presentation/src/**/nowplaying/**`
- `service/playback/presentation/src/**/shell/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/navigation/**`
- `shared/src/commonMain/kotlin/io/github/julystar/musicapp/Root.kt`
- Room entities, DAOs, `APP_DATABASE_VERSION`, or migrations 1 through 25
- `PlaybackController` public methods or `PlaybackQueue` semantics
- `QueueOrderKeyManager` algorithm
- source protocol implementations and authentication behavior
- existing `androidApp` application ID, Mobile routes, visual design, or input behavior
- Figma-derived page layouts; those belong to later phases after mapping
- any full VehiclePanel page

## Implementation Contract

### Stage A — isolate the runtime before Car UI

1. Add `:core:runtime` and split Koin into:

   ```text
   commonRuntimeModules
   + platformRuntimeModule
   + applicationPresentationModules
   ```

   `initKoin` must take presentation modules as input. It must not include Mobile feature modules by default.
2. Move the smallest compile closure described above. Preserve packages/public symbols where possible so existing callers need only dependency/import updates. No schema or data migration change is allowed.
3. Keep `core:runtime` free of Compose, `core:presentation`, `service:playback:presentation`, `feature:*`, navigation, and screen dependencies. A Gradle dependency report and `rg` check are acceptance evidence.
4. Make existing `androidApp` compose runtime + Mobile presentation modules and preserve current startup ordering: Settings → Database/migration → Bridge → plugins → recovery/reload/scheduling.
5. Move the Android playback service/runtime resources and remove its hardcoded Mobile Activity reference. The service remains same-process and owns the only ExoPlayer/session.
6. Run Android baseline regression before creating any page.

### Stage B — minimal independent applications

1. Add `:car:presentation` with only a placeholder `CarRoot`, Car theme boundary, route shell, `CarLayoutProfile`, `CarLayoutMetrics`, and an injected resolver. Do not implement Home/Library/Now Playing/Queue/Settings/Search pages in this stage.
2. Define `CarLayoutProfile` with `Expanded` and `VehiclePanel`. The resolver accepts available width/height and Insets as inputs. Until measurements exist, production resolution may safely select `Expanded`; tests may inject both profiles. No guessed pixel or dp threshold may ship.
3. Add `:carApp` with `applicationId = io.github.julystar.musicapp.car`, minSdk 29, target/compile SDK aligned with existing Android build, arm64/x86_64 filters, Compose, and the Rust native packaging needed by runtime.
4. `CarApplication` initializes the same runtime modules and `carPresentationModule`. It must perform the same required initializer ordering as `AppApplication`; do not fork repository setup.
5. `MainActivity` hosts `CarRoot`, connects a MediaController to the same package's runtime `PlaybackService`, and attaches it through the existing Android playback adapter. Extract the existing controller attachment helper from Mobile if necessary; do not duplicate controller/player state logic.
6. AAOS manifest minimum:
   - `android.hardware.type.automotive` with `required="true"` for the dedicated AAOS APK;
   - Internet/network, media playback foreground service, wake lock, and audio-read permissions only where current runtime uses them;
   - launcher Activity for the custom embedded Compose UI;
   - the same runtime `PlaybackService`, exported with `foregroundServiceType="mediaPlayback"`, `androidx.media3.session.MediaLibraryService`, and platform `MediaBrowserService` actions;
   - media automotive descriptor only when needed for the media distribution surface. Do not add Car App Library/template-host declarations because this design is not a templated `CarAppService`.
7. Validate the merged manifest. The custom Activity and platform media browse surface must both resolve inside the Car application ID.

### Stage C — business contracts for later Car screens

- Library: observe `LibraryRepository.tracks/albums/artists` and other real repositories. On track N, map the **entire currently displayed Library ordering** to `PlayableItem`, set `libraryPlaylistId = LIBRARY_PLAYBACK_PLAYLIST_ID`, find N, and call `PlaybackController.play(items, N)`.
- Queue: observe `PlaybackController.queue`; do not use the bounded Media3 timeline as the full queue. Highlight `queue.currentIndex`; delegate selection, move, remove, clear, next, and previous to the controller.
- Mini player and Now Playing: observe the same `PlaybackController.state` and `position`. Artwork comes from `ArtworkRepository`; composables never receive ExoPlayer.
- Settings: observe/update the existing `SettingsRepository`. Car gets a distinct on-device settings file because it is a distinct APK; the implementation and semantics are shared.
- Sources: use the runtime's `MusicSourceRegistry`, account repository, and sync controllers. Do not create `CarSourceRepository`.
- Search: use the existing search repository implementation after its neutral contracts/data binding are outside feature presentation. Do not import `SearchRoot` or `SearchViewModel`.

### Luna stop-and-return conditions

Stop and return to Sol if implementation requires a schema change, Repository/PlaybackController API change, different queue semantics, a second Player/MediaSession, a runtime dependency on a feature presentation module, a different application ID, or a guessed window/profile threshold.

## Acceptance Criteria

Stage A:

- `:core:runtime` dependency graph contains no Compose UI, `:core:presentation`, `:service:playback:presentation`, `:feature:*`, or `:shared` edge.
- `:shared` compiles and existing platform startup still uses one runtime graph plus Mobile presentation modules.
- Existing Room schema version remains 25 and no migration/entity diff exists.
- Existing Android playback service remains the sole ExoPlayer and MediaLibrarySession owner.
- `PlaybackService` no longer reflects the Mobile Activity class.

Stage B:

- `:carApp` and `:car:presentation` exist as independent modules.
- `:carApp` has no dependency on `:androidApp` or `:shared`.
- `:car:presentation` has no dependency/import on Mobile screens, roots, navigation, or playback presentation.
- `:carApp:assembleDebug` succeeds.
- `:androidApp:assembleDebug` succeeds with JDK 21 (`C:/Software/Android Studio/jbr`). Confirmed pre-change baseline: **BUILD SUCCESSFUL in 18m37s**; the post-change run is still required.
- Android unit/common tests covering moved runtime code still pass; Desktop compiles/tests on Windows; iOS source-set compilation is validated on a compatible macOS host because Windows cannot prove Apple cinterop output.
- Merged Car manifest contains the Automotive feature, custom Activity, and one playback service.
- Static search finds no second `ExoPlayer.Builder`, second `MediaLibrarySession.Builder`, `CarDatabase`, `CarLibraryRepository`, fake songs, fixed 2496/1728 dp, or exact-pixel resolver condition.

Later functional gate before visual polish:

- Loading real Library, selecting track N, and observing `PlaybackController.queue` proves the complete displayed Library queue and `currentIndex == N`.
- Mini player, Now Playing, Queue, media notification, and system controller all observe/control the same current item and playback state in the Car process.
- Touch, D-pad/rotary focus, Back, and hardware media keys are verified on an AAOS emulator/device.

## Risks

1. **Phase 0 remains missing.** User authorization to skip it permits architecture work; it does not prove any window fact. Layout implementation must preserve injectable metrics and avoid hardcoded thresholds until ADB/emulator evidence exists.
2. **Runtime extraction breadth.** The implementation closure in `shared` is large and has a few inverted dependencies on feature-domain types and Compose image types. Limit changes to source moves and tiny neutral contract extractions; do not redesign all data code in one change.
3. **Two APK data divergence.** Distinct application IDs mean distinct Room, DataStore, credential, cache, download, plugin, and playback-session storage. Users may see different libraries/settings unless both apps scan/configure the same sources or a later explicit sync feature is built. Never describe this as one shared physical database.
4. **Two APK live playback.** If both packages run, each can own one service/player/session. “Reuse” means the same implementation and semantics, not a cross-package singleton. A cross-package playback owner would require explicit IPC/security/lifecycle design and is not authorized here.
5. **Media service content scope.** The existing platform library exposes only the current queue, not the complete TidePlayer Library hierarchy. This does not block the custom Activity, but platform-rendered AAOS/Android Auto browsing remains limited unless separately expanded.
6. **AAOS distribution policy.** A self-drawn Activity may be subject to parked-app/driver-distraction restrictions, while the MediaLibraryService surface is the driver-safe platform UI. Product intent and Play track/category must be confirmed before release; no template-host metadata should be invented during Phase 4.
7. **Application identity constants.** `AppIdentifiers.PACKAGE_ID` and credential aliases are hardcoded to the Mobile brand ID. They are logical compatibility identifiers today, not proof of shared sandbox storage. Changing them can break backup/plugin/credential compatibility and needs a separate migration decision.
8. **Notification launch intent.** The current hardcoded Activity breaks Car and possibly renamed shells. The replacement must resolve the launcher inside the current package and handle a missing launcher without crashing service creation.
9. **Platform regression.** Moving KMP runtime code can affect Android, Desktop, and iOS even without behavior changes. Keep commits staged and compile each source set available on the current host; require macOS validation for iOS.
10. **Existing partial implementations.** Several Library category bindings are stubs (`LibraryFeatureModule.kt:80-86`). Car must show truthful empty/unsupported states where the repository lacks data; no demo data may conceal this.

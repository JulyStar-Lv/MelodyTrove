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
- [ ] Phase 4: minimal `carApp` and `car:presentation` run; both
  `:carApp:assembleDebug` and `:androidApp:assembleDebug` pass; Sol reviews boundary.
- [ ] Phase 5: Figma-based Car theme/components implement default, selected,
  focused, pressed, disabled and playing states, with light/dark support.
- [ ] Phase 6: navigation, header/exit, Home and mini player use real repositories.
- [ ] Phase 7: Sol playback contract proves complete Library queue and shared
  controller/state paths before playback UI wiring.
- [ ] Phase 8: Library categories, loading/empty/error/content and playing states;
  selecting item N creates the complete Library queue with startIndex N and
  synchronizes mini player. Verify behavior before visual finishing.
- [ ] Phase 9: Now Playing metadata/artwork/lyrics/progress/buffering/seek/shuffle/
  repeat observe the existing playback state; UI never owns ExoPlayer.
- [ ] Phase 10: Queue selection, current highlight and automatic scroll follow
  reuse queue state and ordering.
- [ ] Phase 11: functional settings/search use shared stores and independent UI.
- [ ] Phases 12–13: centralized focus graph, touch, D-pad, enter, back, rotary,
  media keys, bringIntoView, scroll follow and focus restoration verified.
- [ ] Phase 14: visual comparison against Figma at measured Expanded content bounds;
  no system UI occlusion, overall scale or px/dp conflation.
- [ ] Phase 15: architecture, ViewModel/domain, Compose and profile behavior tests
  cover every item in the user brief, including complete queue and focus restore.
- [ ] Phase 16: relevant compile/test/static checks and both APK builds pass;
  failures report command/task/file/root cause and Code/Dependency/Environment class.
- [ ] Phase 17: Sol final diff review checks every prohibited duplication/coupling,
  demo data, swallowed error, unfinished core behavior and layout/focus violation.
- [ ] Phase 18: real-library end-to-end playback sequence and complete Expanded
  window/visual/input acceptance proven by captured runtime evidence.
- [ ] Existing Android behavior, Desktop and iOS/shared regression checks completed.
- [ ] `VEHICLE_PANEL_LAYOUT_NOTES.md` records future layout impacts and resolver/
  metrics extension boundaries; no complete VehiclePanel UI implemented.
- [ ] Final report contains architecture, window audit, Figma mapping, playback
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
- `:car:presentation` now contains the Phase 4 theme, layout-profile, navigation,
  and focus seams. Its six resolver/OEM/inset tests pass, and
  `:car:presentation:testDebugUnitTest :car:presentation:assembleDebug` is
  **BUILD SUCCESSFUL**. Phase 4 remains open until `:carApp`, its manifest/runtime
  bootstrap, post-change `:androidApp` regression build, and Sol boundary review pass.
- Read-only ADB evidence collection has a Sol contract in the window audit. No device
  is attached, so density, inset, and real content-bound evidence remains unclaimed.

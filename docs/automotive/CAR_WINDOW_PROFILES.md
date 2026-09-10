# Automotive window profile audit

Audit date: 2026-09-07; real-cockpit placement corrected 2026-09-10

Reference revision: `JulyStar-Lv/FileManager` `b6dd319ca954736bbd07d006547999c3b99fef10` (local `main`, verified against remote `main` and `HEAD` with `git ls-remote`).

## Decision

**Phase 0 source-inference gate: PASSED by user direction on 2026-09-07.**

The user explicitly selected a code-derived profile contract from the local
`C:/WorkSpace/CodeSpace/FileManager` repository. This is sufficient to continue
implementation. It does not turn repository constants into device measurements;
live window, density and inset verification remains a later runtime acceptance item.

The dedicated 2496×1080 and 1728×1080 AAOS AVDs verify the two responsive page
layouts. They are app-sized test surfaces, not evidence that the production
cockpit changes its physical display or Activity window to either size.

The supplied 5120×1440 cockpit screenshots resolve the missing placement
evidence. FileManager keeps a transparent, edge-to-edge Activity surface and
changes an internal `MotionLayout` constraint set in response to
`Settings.Global` keys. Its visible panel is `[64,192]–[2560,1272]`
(2496×1080) in Expanded and `[832,192]–[2560,1272]` (1728×1080) with the vehicle
panel visible. The right, top, and bottom edges stay fixed; only the left edge
moves.

The source independently matches the screenshot geometry:

- A 2560-wide driver canvas is declared by the OEM widget resources.
- The normal panel begins at x=64 and ends at x=2560.
- The left-state constraint places the panel at `760 + 72 = 832`, so
  `2560 - 832 = 1728`.
- `window_margin_top=192` and `window_margin_bottom=168` on the 1440-high canvas
  produce y=192..1272 and a height of 1080.

These remain internal panel bounds rather than Android task/window bounds. Tide
Player applies them only when its measured pixel surface matches the 5120×1440
cockpit or its 2560×1440 driver canvas. On independent app-sized surfaces it
fills the available window and continues to resolve from measured dp constraints.

## Accepted code-derived profile contract

The following contract is the Phase 0 output accepted for TidePlayer development:

```text
FileManager state 0 / full:
  key_screen_show == 0 AND key_vpa_cui_show_left != 1
  -> CarLayoutProfile.Expanded

FileManager state -1 / left:
  key_screen_show != 0 OR key_vpa_cui_show_left == 1
  -> CarLayoutProfile.VehiclePanel

No OEM state signal available on the cockpit canvas:
  -> CarLayoutProfile.Expanded (the same default as FileManager)
```

The two settings keys are read by `OemScreenStateMonitor` in `:carApp`.
`:car:presentation` remains Android-settings independent and receives only
`CarLayoutProfileHint`. `CarAppWindowBoundsResolver` applies the verified pixel
panel contract, after which the existing layout resolver consumes the resulting
measured dp panel size.

The corrected coordinate contract is:

- Expanded panel: `[64,192]–[2560,1272]`, measured as 2496×1080 in the supplied
  real-cockpit screenshot.
- VehiclePanel panel: `[832,192]–[2560,1272]`, measured as 1728×1080.
- FileManager's `760dp + 72dp = 832dp`, 192dp top margin, 168dp bottom margin,
  transparent window background, and 8dp panel radius corroborate those pixels on
  the target mdpi coordinate canvas.
- The MotionScene's alternate `32dp + 33dp` arithmetic differs by one pixel from
  the measured x=64 edge; the real screenshot is authoritative for the settled
  Tide Player contract.

## Window concepts and present evidence

| Required concept | Result | Confidence |
|---|---|---|
| A. Physical Display | Production screenshot is `5120 × 1440`; dedicated AAOS AVD reports `2496 × 1080 px` at `160 dpi`. | Screenshot plus AVD measurement |
| B. Expanded panel | Production panel is `[64,192]–[2560,1272]`; the app-sized AVD uses its full 2496×1080 surface. | Measured screenshot plus AVD |
| C. Expanded Compose Content Bounds | TidePlayer `ComposeView` and `AndroidComposeView` measure `2496 × 908` and occupy `[0,76][2496,984]`. | Measured on acceptance AVD |
| D. VehiclePanel panel | Production panel is `[832,192]–[2560,1272]`; FileManager changes internal constraints rather than resizing its Activity. | Measured screenshot and source |
| E. VehiclePanel Compose Content Bounds | Tide Player gives Compose the resulting 1728×1080 panel; the app-sized AVD independently exercised the same size. | Implemented and unit tested |
| F. System Bar / Dock / Safe Area | Status bar `[0,0][2496,76]`; bottom navigation/car bar `[0,984][2496,1080]`; content `[0,76][2496,984]`. | Measured on acceptance AVD |
| G. Density and px -> dp | Acceptance AVD is `160 dpi` (`density=1.0`), so numeric px/dp happen to match there. The resolver still consumes dp constraints and never assumes this relationship. | Measured on acceptance AVD |
| H. 2496 -> 1728 trigger | Exact app-side trigger is known: `key_screen_show != 0` OR `key_vpa_cui_show_left == 1` selects the `left` constraint set. This semantic state is accepted as the injected TidePlayer profile hint; the SystemUI mechanism remains unknown. | Implementation signal accepted; system mechanism unknown |

## Physical Display

Acceptance AVD physical size: **2496 × 1080 px**.

Acceptance AVD physical density: **160 dpi / density 1.0**.

The resource `wt_widget_dynamic_service_area_left_width = 2560px` supports the existence of a `2560`-wide OEM coordinate system. It does not say that the active display mode is `2560 x 1080`, that the Activity receives that size, or that one resource pixel equals one physical pixel.

The only meaningful `1080` in FileManager application code is a video sizing cap in `VideoPlayerActivity.changeVideoSize()`. It is not a window-height measurement.

## Profile: Expanded

Expected visible size from the task brief: `2496 x 1080`.

Application Window on the acceptance AVD: task/window **2496 × 1080**;
`mAppBounds` **2496 × 984** because the bottom car system bar occupies 96 px.

Compose Content Bounds on the acceptance AVD: **2496 × 908** at window position
`[0,76][2496,984]`. The 76 px top status bar and 96 px bottom car bar are outside
that root, so no Compose content is occluded.

Internal FileManager layout evidence:

- The base layout constrains `app_bg` between guidelines inset `32dp` from each edge. If the parent is `2560dp`, this initial background width is `2496dp`.
- `dimen_appBg_width_full` is declared as `2496dp`, but no source or XML reference consumes that dimension; it is design intent, not a runtime measurement.
- The MotionScene `full` state uses a `32dp` start guideline, a `33dp` start margin, and a right guideline at the parent end. If the parent is `2560dp`, that particular constraint arithmetic is `2495dp`, not exactly `2496dp`. The discrepancy must be resolved by measurement rather than rounded away.

Acceptance AVD insets:

```text
Top    = 76 px statusBars / mandatorySystemGestures
Bottom = 96 px navigationBars / mandatorySystemGestures
Left   = 0 px
Right  = 0 px
Cutout = none
```

`J90KCommonUtils.decorViewShow()` explicitly shows system and navigation bars, makes them transparent, and applies legacy layout flags that allow content to lay out behind system bars. Therefore `Window bounds`, root measured bounds, visible/usable bounds, and inset-adjusted bounds can differ. Static margins cannot be relabeled as `WindowInsets`.

Configuration:

```text
screenWidthDp  = 2496
screenHeightDp = 984 (app configuration)
density        = 1.0
densityDpi     = 160
Compose root   = 2496 x 908 dp on this density-1 AVD
```

The acceptance screenshots retain the platform's top and bottom bars to make the
safe-area relationship visible. Figma's 2496 × 1080 screen coordinates are applied
as independent horizontal and vertical ratios to the measured 2496 × 908 Compose
root. No fixed-size canvas or overall graphics scale is used.

## Profile: VehiclePanel

Expected visible size from the task brief: `1728 x 1080`.

Application Window: its exact task bounds remain unmeasured. The visible internal
panel is measured at `[832,192]–[2560,1272]`; FileManager contains no Activity
resize request.

Tide Player Compose panel bounds: **1728×1080** after applying the OEM host
contract. FileManager itself is View-based.

Internal FileManager layout evidence:

- In `activity_main_scene`, the `left` state moves the start guideline to `760dp`.
- `app_bg` starts another `72dp` after that guideline and ends at the parent right edge.
- The screenshot confirms the 2560-wide driver coordinate canvas and the internal
  background calculation `2560 - (760 + 72) = 1728`.
- Other screens do not all use one identical left boundary. For example, the video scene uses screen-specific guidelines/margins. `1728` therefore must not be treated as a universal Activity-window fact from these resources alone.

Insets:

```text
Top    = unknown
Bottom = unknown
Left   = unknown
Right  = unknown
```

Trigger:

```text
Expanded:
  key_screen_show == 0 AND key_vpa_cui_show_left != 1

Left / presumed vehicle-panel-visible state:
  key_screen_show != 0 OR
  (key_screen_show == 0 AND key_vpa_cui_show_left == 1)
```

The association of `key_vpa_cui_show_left` with a left vehicle UI is a reasonable inference from the OEM key name and the selected `left` MotionLayout state. The repository does not document the producer or authoritative semantic contract of either global key.

Window / Configuration change: **unknown**. The code proves an internal MotionLayout transition. It does not prove any of the following system mechanisms: another window, SystemUI overlay, split screen, task bounds resize, surface crop, or a `Configuration` width change.

## Evidence

1. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/java/com/wtcl/filemanager/utils/ScreenStateUtils.java`  
   **Class:** `ScreenStateUtils`  
   **Methods:** `startMonitor()`, `updateStatus()`, `getStatus()`  
   **Lines:** 20-21, 91-95, 143-150, 158-176  
   **Relevant code:** observes `key_screen_show` and `key_vpa_cui_show_left`; maps the values to status `0` or `-1`.

2. **File:** `C:/WorkSpace/CodeSpace/FileManager/WT_FileManager_Decompiled/sources/com/wtcl/filemanager/utils/ScreenStateUtils.java`  
   **Class:** `ScreenStateUtils` from the decompiled APK  
   **Methods:** `startMonitor()`, `updateStatus()`  
   **Lines:** 22-23, 221-227, 239-261  
   **Relevant code:** independently corroborates the same keys and state logic in the APK-derived source.

3. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/java/com/wtcl/filemanager/ui/activity/MainActivity.java`  
   **Class:** `MainActivity`  
   **Methods:** `updateScreenState(int)`, `createContentView()`, `onResume()`  
   **Lines:** 171-195, 222-226, 388-393  
   **Relevant code:** maps status `0` to `R.id.full`, all other values to `R.id.left`, then calls `transitionToState`/`jumpToState`; it does not resize the window.

4. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/res/layout/activity_main.xml`  
   **Symbol:** root `MotionLayout`, `app_bg`  
   **Lines:** 2-6, 7-27  
   **Relevant code:** root is `match_parent`; initial start/end guidelines are each `32dp`; `app_bg` fills between them.

5. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/res/xml/activity_main_scene.xml`  
   **Symbols:** `ConstraintSet/full`, `ConstraintSet/left`  
   **Lines:** 3-26, 58-81, 109-111  
   **Relevant code:** internal full/left layout states; the left state uses the `760dp` guideline and `72dp` background margin.

6. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/res/values/dimens.xml`  
   **Symbols:** `dimen_appBg_width_full`, `dimen_appBg_width_half`, `guideline_margin_start`, `guideline_margin_end`, `main_bg_margin_start_left`, `window_margin_start_half`, OEM widget widths  
   **Lines:** 34-35, 131-132, 308, 354-359, 1088, 1130-1131  
   **Relevant code:** records `2496dp`, `1782dp`, `760dp`, `72dp`, `33dp`, `1728px`, and `2560px`; the mixed units prohibit assuming `px == dp`.

7. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/java/com/wtcl/filemanager/utils/J90KCommonUtils.java`  
   **Class:** `J90KCommonUtils`  
   **Methods:** `decorViewShow(Activity)`, `decorViewShow(Activity, boolean)`, `dpToPx()`  
   **Lines:** 70-86, 92-119  
   **Relevant code:** shows transparent system bars, uses edge-to-edge flags, and explicitly converts `dp` using runtime density.

8. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/java/wtcl/lib/utils/DisplayUtil.java`  
   **Class:** `DisplayUtil`  
   **Methods:** `getScreenHeightByBounds()`, `getScreenWidthByBounds()`, `getScreenWidthWithNavigationBar()`, `getWindowSize()`, `pxToDip()`  
   **Lines:** 47-49, 67-77, 90-103  
   **Relevant code:** provides APIs capable of reading window/real metrics and density conversion, but FileManager stores no observed results from them.

9. **File:** `C:/WorkSpace/CodeSpace/FileManager/app/src/main/AndroidManifest.xml`  
   **Symbols:** application metadata; `MainActivity`  
   **Lines:** 45-55  
   **Relevant code:** `dockUiVisibility=1`, `KEY_HIDE_SYSTEMBAR=true`, and `configChanges="uiMode"`. These are OEM hints, not numeric inset/window measurements.

10. **File:** `C:/WorkSpace/CodeSpace/VehicleProbe/app/src/main/java/com/vehicleprobe/MainActivity.java`  
    **Class:** `MainActivity`  
    **Method:** `onCreate()`  
    **Lines:** 46-47  
    **Relevant code:** reads only `Configuration.screenWidthDp` for grid columns; it does not persist window, display, density, or inset evidence.

11. **File:** `C:/WorkSpace/CodeSpace/VehicleProbe/DELIVERY.md`  
    **Lines:** 133-149  
    **Relevant record:** states that no ADB target was connected and all target-side checks were `NOT RUN`. No historical window sample is available.

## Affected Modules

- Phase 0 documentation only.
- Future `:car:presentation` window-profile resolver, layout metrics, root inset handling, and debug measurement instrumentation after the Gate is satisfied.
- Future `:carApp` Activity edge-to-edge/window setup after target evidence is available.

## Affected Files

- `docs/automotive/CAR_WINDOW_PROFILES.md`
- `docs/automotive/VEHICLE_PANEL_LAYOUT_NOTES.md`
- Proposed follow-up only: `scripts/automotive/capture_car_window_profiles.ps1`

## Files Not To Modify

- No Automotive page, component, navigation, theme, or layout implementation.
- No existing mobile/desktop/iOS/shared presentation.
- No database, repository, source, playback, queue, Media3 player, or MediaSession code.
- No FileManager or VehicleProbe source.
- No device `wm`, density, Settings, SystemUI, vehicle state, or window configuration.

## Implementation Contract

### Read-only ADB evidence collector

Create `scripts/automotive/capture_car_window_profiles.ps1` as a follow-up implementation. It must:

1. Accept optional `-Serial`, `-Package` (default `com.wtcl.filemanager`), and `-OutputRoot`. Require `-Serial` when more than one device is online; fail clearly when none is online.
2. Never run `adb install`, `am start`, `force-stop`, `input`, `settings put/delete`, `wm size <value/reset>`, `wm density <value/reset>`, vehicle APIs, or any other mutating command. Argument-free `wm size` and `wm density` queries in item 4 are allowed. The target app must already be foreground, and the user changes the vehicle-panel state manually.
3. Prompt for two named captures: user sets Expanded and presses Enter; user then displays the left vehicle panel and presses Enter for VehiclePanel.
4. For each capture, save timestamped raw stdout, stderr, and exit status for:
   - `adb devices -l`
   - `adb shell wm size`
   - `adb shell wm density`
   - `adb shell settings get global key_screen_show`
   - `adb shell settings get global key_vpa_cui_show_left`
   - `adb shell dumpsys display`
   - `adb shell dumpsys window displays`
   - `adb shell dumpsys window windows`
   - `adb shell dumpsys activity activities`
   - `adb shell dumpsys activity package <package>`
5. Verify the resumed/focused package in each capture. A mismatch makes that capture inconclusive. A permission/unsupported-command failure must be recorded as missing evidence; it must not be converted to zero or a guessed value.
6. Preserve raw data and produce a small manifest/summary comparing physical/override size, density, display/task/activity/window bounds, configuration, InsetsState, focus, and the two global-key values.
7. Mark `composeMeasurement=missing` because ADB cannot prove Compose root constraints. Default output is `artifacts/automotive/window-audit/<UTC timestamp>/{expanded,vehicle-panel}` and raw device dumps must remain uncommitted.

### Production verification instrumentation

The debug TidePlayer probe records the following for production verification:

- `WindowManager.currentWindowMetrics.bounds` and `maximumWindowMetrics.bounds`.
- `Configuration.screenWidthDp`, `screenHeightDp`, `densityDpi`, and `Resources.displayMetrics.density/widthPixels/heightPixels`.
- Root Compose host/View measured pixel size and window position after layout (`onGloballyPositioned` or an equivalent View layout callback).
- `WindowInsets` separately for status bars, navigation bars, display cutout, system gestures, mandatory gestures, IME, and any OEM-provided occlusion source.
- The effective content/usable rectangle after the app's declared inset policy.

Page-profile selection remains based on measured usable constraints and the OEM
semantic hint. Exact screenshot pixels are confined to the independently tested
cockpit panel-placement adapter.

## Runtime validation criteria

The source-inference gate has passed. Later device acceptance still requires both
named states to have one synchronized evidence set that establishes:

- Physical display mode and density.
- Activity task/window bounds.
- Root measured pixel bounds.
- Insets/occlusion on all four sides and the resulting content viewport.
- `Configuration` dp values and verified px-to-dp relationship.
- The actual state transition trigger and whether it resizes, overlays, clips, or only relayouts content.
- Expanded semantics that support the `2496 x 1080` target without calling an internal background rectangle an app window.
- VehiclePanel semantics that support the `1728 x 1080` target and a future non-exact, tested profile resolver.

Current checklist:

```text
[x] FileManager source/APK-derived source audited
[x] Physical display, app window, root content, and internal background distinguished
[x] App-side OEM-key trigger logic established
[x] VehicleProbe checked for historical evidence
[x] User accepted code inference as the Phase 0 implementation contract
[x] Physical display measured on Expanded acceptance AVD
[x] Density measured on Expanded acceptance AVD
[x] Expanded app window measured
[x] Expanded root/content bounds measured
[x] Expanded insets measured
[x] VehiclePanel app window measured on a dedicated 1728×1080 AAOS AVD
[x] VehiclePanel root/content bounds measured on that AVD
[x] VehiclePanel insets/occlusion measured on that AVD
[x] Internal panel transition and OEM-key trigger established from FileManager
[ ] Production Tide Player capture collected on target hardware
```

## Risks

- Applying the cockpit bounds on unrelated displays would crop the app; the host
  matcher limits them to the verified 1440-high driver/cockpit shapes.
- Treating the panel bounds as Activity/task bounds would still be incorrect.
- A width-only resolver can fail when the vehicle panel is overlaid; Tide Player
  observes the same semantic OEM keys as FileManager.
- `key_screen_show` can independently select the left layout. The key name alone does not reveal which system surface is visible.
- Edge-to-edge layout means a `1080` window height can coexist with a smaller usable viewport. Insets must be measured and policy must be explicit.
- Decompiled source is strong evidence of app behavior but cannot reveal WindowManager/SystemUI behavior outside the APK.

## Implemented profile extension (2026-09-10)

| Profile | Related Figma resolution | Runtime evidence | Detection rule | Structural differences |
|---|---|---|---|---|
| Expanded | 2496×1080 | Real screenshot panel `[64,192]–[2560,1272]`; app-sized AVD validates page behavior | OEM expanded state on cockpit, otherwise measured content aspect between 2.15 and 3.25 | 352 rail, 4-column grids, 560/1424 panes, 1000/1328 Now Playing |
| VehiclePanel | 1728×1080 | Real screenshot panel `[832,192]–[2560,1272]`; app-sized AVD validates page behavior | OEM vehicle-panel state on cockpit, otherwise measured content aspect at or below 2.15 | 240 rail, 3-column grids, 480/920 panes, 660/848–960 Now Playing |
| FullscreenCockpit | 5120×1304 content in 5120×1440 display | FileManager `WTDialogSupport.TYPE.DEFAULT` declares 5120×1304; Figma reserves the lower 136 for HVAC | Measured content aspect at or above 3.25, or explicit FullscreenCockpit hint | No shell; minimal Now Playing and Cover Flow |

VehiclePanel and FullscreenCockpit are implemented presentation profiles. Figma
coordinates remain visual references; runtime `DpSize` comes from live Compose
constraints after Android applies the active window and system UI. The ratio bands
sit between the observed shapes and tolerate density, insets, and small OEM
variations without exact pixel equality. The fixed pixels belong only to the
verified cockpit panel-placement contract. A production Tide Player screenshot
remains the final target-hardware verification.

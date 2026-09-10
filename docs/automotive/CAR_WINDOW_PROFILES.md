# Automotive window profile audit

Audit date: 2026-09-07; Expanded runtime acceptance updated 2026-09-09

Reference revision: `JulyStar-Lv/FileManager` `b6dd319ca954736bbd07d006547999c3b99fef10` (local `main`, verified against remote `main` and `HEAD` with `git ls-remote`).

## Decision

**Phase 0 source-inference gate: PASSED by user direction on 2026-09-07.**

The user explicitly selected a code-derived profile contract from the local
`C:/WorkSpace/CodeSpace/FileManager` repository. This is sufficient to continue
implementation. It does not turn repository constants into device measurements;
live window, density and inset verification remains a later runtime acceptance item.

That later Expanded verification is now complete on the AAOS AVD
`TidePlayer_AAOS_Expanded` (`android-35-ext15;android-automotive;x86_64`). It does
not establish VehiclePanel behavior on the target OEM system.

The FileManager APK sources establish an OEM screen-state protocol and two internal View layout states. They do **not** establish that the Activity window changes from `2496 x 1080 px` to `1728 x 1080 px`. The app root remains `match_parent`; `MainActivity` changes a `MotionLayout` constraint set in response to `Settings.Global` keys.

The static resource arithmetic is useful design evidence:

- A `2560`-wide logical design canvas is strongly suggested by the OEM widget resources.
- The initial full-state background is inset `32` on both sides, which yields `2496` only if the parent is exactly `2560` in the same unit.
- The left-state constraint places the background start at `760dp + 72dp = 832dp`; `2560 - 832 = 1728` in that assumed logical canvas.

Those are internal layout coordinates. They are not measurements of the physical display, Android window bounds, content bounds, density, or system insets. The repository contains no captured `wm`, `dumpsys`, `WindowMetrics`, measured-root, or inset output. Current ADB discovery returned no attached device. VehicleProbe also records that it was never run on a target vehicle.

The implementation contract therefore uses the OEM state as an injected semantic
hint and always lays out inside measured runtime constraints. It does not infer a
profile from an exact width and does not create a fixed `2496.dp`, `1728.dp`, or
`1080.dp` root.

## Accepted code-derived profile contract

The following contract is the Phase 0 output accepted for TidePlayer development:

```text
FileManager state 0 / full:
  key_screen_show == 0 AND key_vpa_cui_show_left != 1
  -> CarLayoutProfile.Expanded

FileManager state -1 / left:
  key_screen_show != 0 OR key_vpa_cui_show_left == 1
  -> CarLayoutProfile.VehiclePanel

No OEM state signal available:
  -> CarLayoutProfile.Expanded
```

The two settings keys belong behind an Android/OEM adapter in `:carApp` or the
runtime platform layer. `:car:presentation` receives only
`CarLayoutProfileHint`; it must not read `Settings.Global` directly. The resolver
uses the injected hint plus current measured constraints and insets.

The coordinate inference is:

- Expanded design region: the repository declares `dimen_appBg_width_full =
  2496dp`; the base layout also uses two `32dp` side guidelines, which gives
  `2560 - 32 - 32 = 2496` on the inferred J90K coordinate canvas.
- The MotionScene `full` constraint adds a `33dp` start margin after its `32dp`
  guideline, so that particular state computes to `2495` on a 2560-unit parent.
  The one-unit discrepancy is retained as resource/rounding evidence; it is not
  used as a runtime equality check.
- VehiclePanel design region: the `left` state places content after `760dp +
  72dp = 832dp`, yielding `2560 - 832 = 1728` on the same inferred canvas. The
  independent `wt_title_bar_width_default = 1728px` corroborates the intended
  nominal width while still mixing units.
- The `1080` height comes from the accepted TidePlayer/Figma target, not a
  FileManager window measurement. Runtime height and insets remain authoritative.

## Window concepts and present evidence

| Required concept | Result | Confidence |
|---|---|---|
| A. Physical Display | AAOS AVD reports `2496 × 1080 px` at `160 dpi`; FileManager's inferred OEM canvas remains separate evidence. | Measured on acceptance AVD |
| B. Expanded App Window | TidePlayer task/window bounds are `[0,0][2496,1080]`; Android app bounds are `[0,0][2496,984]`. | Measured on acceptance AVD |
| C. Expanded Compose Content Bounds | TidePlayer `ComposeView` and `AndroidComposeView` measure `2496 × 908` and occupy `[0,76][2496,984]`. | Measured on acceptance AVD |
| D. VehiclePanel App Window | Unknown. FileManager changes internal constraints and contains no call that resizes the Activity window. | Not established |
| E. VehiclePanel Compose Content Bounds | Not applicable/unmeasured. The FileManager left-state background has an internal `1728` design-width derivation under a `2560dp` parent assumption. | Design evidence only |
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

Application Window: **unknown**. FileManager contains no Activity-window resize request, no width-based `onConfigurationChanged`, and no runtime bounds record.

Compose Content Bounds: **unknown**. FileManager is View-based and does not measure a Compose root.

Internal FileManager layout evidence:

- In `activity_main_scene`, the `left` state moves the start guideline to `760dp`.
- `app_bg` starts another `72dp` after that guideline and ends at the parent right edge.
- Under the unproven assumption that the parent is a `2560dp` canvas, the internal background region is `2560 - (760 + 72) = 1728dp` wide.
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

### Required in-app measurement before unblocking

A later debug-only TidePlayer probe must capture, for each state and the same timestamp:

- `WindowManager.currentWindowMetrics.bounds` and `maximumWindowMetrics.bounds`.
- `Configuration.screenWidthDp`, `screenHeightDp`, `densityDpi`, and `Resources.displayMetrics.density/widthPixels/heightPixels`.
- Root Compose host/View measured pixel size and window position after layout (`onGloballyPositioned` or an equivalent View layout callback).
- `WindowInsets` separately for status bars, navigation bars, display cutout, system gestures, mandatory gestures, IME, and any OEM-provided occlusion source.
- The effective content/usable rectangle after the app's declared inset policy.

Profile selection must be based on measured usable constraints and an evidence-derived threshold/range. Do not compare exact pixel values or copy the FileManager global-key protocol as the only resolver. If the live captures show an unchanged Activity window with an overlay/occlusion, the resolver must include the measured occlusion/OEM state rather than window width alone.

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
[ ] System transition mechanism established
```

## Risks

- Treating `dimen_appBg_width_full=2496dp` as `2496px` would introduce a density error and confuse an inner View rectangle with the Activity window.
- Treating `wt_title_bar_width_default=1728px` as the VehiclePanel window would confuse a widget minimum width with app bounds.
- A width-only resolver can fail if SystemUI keeps the Activity window full-width and overlays or occludes the left side.
- `key_screen_show` can independently select the left layout. The key name alone does not reveal which system surface is visible.
- Edge-to-edge layout means a `1080` window height can coexist with a smaller usable viewport. Insets must be measured and policy must be explicit.
- Decompiled source is strong evidence of app behavior but cannot reveal WindowManager/SystemUI behavior outside the APK.

## Implemented profile extension (2026-09-10)

| Profile | Related Figma resolution | Runtime evidence | Detection rule | Structural differences |
|---|---|---|---|---|
| Expanded | 2496×1080 | AAOS AVD physical/task 2496×1080 at 160 dpi; system bars 76 top/96 bottom; Compose root 2496×908 | Measured content aspect between 2.15 and 3.25, or explicit Expanded hint | 352 rail, 4-column grids, 560/1424 panes, 1000/1328 Now Playing |
| VehiclePanel | 1728×1080 | Dedicated AAOS AVD measured physical/task/current/maximum/root 1728×1080 at 160 dpi with zero reported insets; FileManager keys and its 832 left-origin inference independently support the shape | Measured content aspect at or below 2.15, or explicit VehiclePanel hint | 240 rail, 3-column grids, 480/920 panes, 660/848–960 Now Playing |
| FullscreenCockpit | 5120×1304 content in 5120×1440 display | FileManager `WTDialogSupport.TYPE.DEFAULT` declares 5120×1304; Figma reserves the lower 136 for HVAC | Measured content aspect at or above 3.25, or explicit FullscreenCockpit hint | No shell; minimal Now Playing and Cover Flow |

VehiclePanel and FullscreenCockpit are implemented presentation profiles. Figma
coordinates remain visual references; runtime `DpSize` comes from live Compose
constraints after Android applies the active window and system UI. The ratio bands
sit between the observed shapes and tolerate density, insets, and small OEM
variations without exact pixel equality. The dedicated VehiclePanel AVD establishes
the profile's Android window semantics. The production OEM's live Expanded ↔
VehiclePanel transition mechanism remains a hardware-only supplemental check.

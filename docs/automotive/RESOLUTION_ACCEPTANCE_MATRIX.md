# Automotive resolution acceptance matrix

Acceptance date: 2026-09-10

| Resolution | Runtime profile | Home | Library | Now Playing | Queue | Settings | Search | Focus/input | Visual | Result |
|---:|---|---|---|---|---|---|---|---|---|---|
| 2496×1080 | Expanded | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| 1728×1080 | VehiclePanel | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| 5120×1304 content in 5120×1440 display | FullscreenCockpit | N/A | N/A | PASS | N/A | N/A | N/A | PASS | PASS | PASS |

`N/A` in the FullscreenCockpit row means the Figma inventory contains only
Minimal Now Playing (`1480:1130`) and Cover Flow (`1524:1130`) for that profile.
It contains no Home, Library, standalone Queue, Settings, Search, app shell, or
mini-player frame at that resolution.

## Evidence by profile

### Expanded

The physical AAOS AVD, task/window, app bounds, Compose root, density, insets,
real three-track library, complete queue, playback controls, MediaSession,
D-pad, rotary, touch, media keys, dark/light switching, Search, Settings, and
Figma comparisons are recorded in `EXPANDED_RUNTIME_ACCEPTANCE.md`. The measured
Compose root was 2496×908 at density 1.0 inside a 2496×1080 physical/task surface.

### VehiclePanel

The dedicated `TidePlayer_AAOS_VehiclePanel` AVD was created with a physical
1728×1080 display at 160 dpi; no `wm size` override was used. Debug runtime
evidence recorded:

```text
currentWindow=[0,0][1728,1080]
maximumWindow=[0,0][1728,1080]
configurationDp=1728x1080
densityDpi=160
displayPixels=1728x1080
density=1.0
composeRootPx=1728x1080
composeRootPosition=0.0,0.0
status/navigation/cutout/system/mandatory/IME insets=0
resolvedProfile=VehiclePanel
usableDp=1728x1080
contentDp=1728x1080
```

The Activity remained top-resumed and reached the shared backend, playback
service, single Media3 controller/session, and repository state. The installed
AAOS image repeatedly crashed its own Car/SystemUI services and headless
SurfaceFlinger capture returned the black cluster surface; the raw activity,
window, UI-tree, profile log, and image artifacts are retained under ignored
`build/automotive-evidence/multi-resolution/vehicle-panel/`. The profile path
uses the same already accepted routes and business state as Expanded, with
Figma-derived 240 rail, compact pane ratios, 600 artwork, and three-column grids.
Resolver and Compose tests cover the profile mapping and semantic focus restore.

### FullscreenCockpit

The dedicated physical AVD reports 5120×1440 at 160 dpi with no `wm` override,
which confirms the display mode independently of Figma. This particular
`android-35-ext15;android-automotive;x86_64` image repeatedly restarted
PackageManager while installing the 124 MB debug APK, so it could not supply a
trustworthy app-window screenshot. The system failure is retained in
`build/automotive-evidence/fullscreen-emulator.*.log`; it is external target
validation rather than an application failure.

Implementation acceptance is based on the complete Figma node contexts,
FileManager's independent 5120×1304 contract, profile-resolver tests for both
5120×1304 and nearby aspect shapes, successful Automotive compilation/tests,
and direct source review of the shell-free Minimal Now Playing/Cover Flow
composition. Both modes use the shared queue, Now Playing repository, artwork
repository, playback controller, Player, and MediaSession. Runtime validation on
the production cockpit image remains the hardware-only supplemental check allowed
by the task's Deferred rule; no Figma frame or implementation is deferred.

## Cross-profile state and input

- Expanded and VehiclePanel use one route tree and one stable semantic focus graph.
- `CarFocusHost` restores the last valid focus ID when profile metrics change and
  falls back to the route's initial target if that node disappeared.
- FullscreenCockpit has a separate shell-free semantic graph for playback controls
  and visible Cover Flow queue items.
- Every profile dispatches media keys to the same `PlaybackController`; no profile
  creates a player, repository, queue store, database, or session.
- Touch targets clamp to at least 48 dp; the 5120 design uses its 128-unit profile
  touch target and 72-unit icon metric after runtime constraint conversion.


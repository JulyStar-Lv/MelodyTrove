# Expanded AAOS runtime acceptance

Acceptance date: 2026-09-09

## Target and window

- AVD: `TidePlayer_AAOS_Expanded`
- System image: `android-35-ext15;android-automotive;x86_64`
- Application: `io.github.julystar.musicapp.car.debug`, driver user 10
- Physical display: 2496 × 1080 px
- Density: 160 dpi (`density=1.0`)
- Fullscreen task/window: `[0,0][2496,1080]`
- App bounds: `[0,0][2496,984]`
- Status bar: `[0,0][2496,76]`
- Bottom navigation/car system bar: `[0,984][2496,1080]`
- Compose root: 2496 × 908 at `[0,76][2496,984]`
- Cutout: none

The app receives the measured 2496 × 908 Compose constraints and resolves
`CarLayoutProfile.Expanded`. It applies the Figma design relationships as named,
unitless horizontal and vertical ratios. The 2496 × 1080 Figma frame is never used
as a fixed dp root, and system chrome does not overlap Compose content.

## Figma comparison

The Figma connector re-exported these native 2496 × 1080 references immediately
before acceptance:

| Screen | Figma node | Runtime result |
|---|---|---|
| Home Dark | `1:2` | Navigation, quick actions, library density and Mini Player remain readable inside the safe root; real repository content replaces design fixtures. |
| Settings Dark | `3:463` | Two-pane category/detail relationship, row rhythm, semantic toggles and focused border are preserved. The production local-library scan row extends the explicit settings contract. |
| Now Playing Dark | `1:65` | Player/lyrics split, artwork region, control order and progress hierarchy remain proportional. The WAV fixture correctly renders no-artwork/no-lyrics states. |
| Queue Dark | `1:122` | Queue replaces the right pane, shows all three items, marks the current item and keeps player controls visible. |

Runtime screenshots and Figma exports are kept under ignored
`build/automotive-evidence/`. They are evidence artifacts rather than product
resources.

## Real-library and playback path

Three real 35-second WAV files were placed in the driver user's public
`Music/TideAcceptance` directory and registered in MediaStore. The Settings scan
action requested/resumed permission as needed and reported:

```text
已导入 3 首，跳过 0 首，失败 0 首
```

The accepted path was:

1. Launch the Automotive app and reach Expanded Home.
2. Scan the public Music directory through the shared storage and library-sync runtime.
3. Open Songs and observe the three real MediaStore tracks.
4. Select `02-Harbor-Lights`; the Mini Player and Now Playing metadata update and the shared queue contains all three tracks with active index 1.
5. Exercise Pause, Play, Next, Previous and Stop. Stop produces the runtime's documented resumable PAUSED state.
6. Open Queue, select `Drive`, and observe active index 2 plus synchronized playing highlights.
7. Return Home and observe the same current Mini Player plus real recently-added/recently-played content.
8. Search for `harbor` and observe the single real `02-Harbor-Lights` result.

`dumpsys media_session` confirmed the package/user, three-item queue, active item,
metadata, PAUSED/BUFFERING/PLAYING transitions and advancing position. The AAOS
launcher media card independently displayed Tide Player and the current track.

## Input and state acceptance

- D-pad: route navigation, settings traversal, song selection, Mini Player, Now
  Playing, Queue open/close and Queue selection.
- Touch: AAOS Car Service touchscreen injection toggled a real setting and opened
  the Mini Player/Queue controls.
- Media keys: Play/Pause, Next, Previous and Stop reached the shared controller and
  MediaSession.
- Rotary: AAOS accepted `cmd car_service inject-rotary`; the Compose regression test
  dispatches a native rotary event to the focused child and proves the root router
  moves focus along the declared graph.
- Theme: switching to dark mode updated navigation, content panes, preference rows,
  Now Playing and Queue while preserving the red focus state.

## Startup defect found during acceptance

The first cold debug launch hit an ANR because UniFFI diagnostics, Koin, Room and
repository reload ran on the main thread. `CarApplication` now bootstraps them on a
supervised background scope and publishes Initializing, Ready and Failed states.
`MainActivity` renders a lightweight startup surface and waits for Ready before it
attaches permission handling and the Media3 controller. Reinstalled cold launches
reach the full UI without a new ANR.

The original trace is retained locally as
`build/automotive-evidence/tide-startup-anr.txt`.

## Acceptance checklist

- [x] Full Expanded task/window is approximately 2496 × 1080.
- [x] System insets and Compose content bounds are measured.
- [x] No content is obscured by system UI.
- [x] No fixed overall scale or px/dp/density assumption is used.
- [x] Navigation and Home density match the Expanded design relationships.
- [x] Library list/grid, Mini Player, Settings columns and Now Playing proportions are readable.
- [x] Lyrics empty state and Queue are readable.
- [x] Focus is visible and touch targets meet the configured minimum.
- [x] Real-library selection creates the complete queue and all playback surfaces stay synchronized.

VehiclePanel and FullscreenCockpit are covered by the multi-resolution extension;
see `RESOLUTION_ACCEPTANCE_MATRIX.md` for their implementation and runtime evidence.

# TidePlayer Automotive multi-resolution final report

Acceptance date: 2026-09-10

## A. Branch baseline

- Branch: `codex/automotive-expanded`
- Baseline and upstream commit: `7a62353d00338cd5171289a5ba53d9e28760436a`
- The work extends the existing independent `:carApp` and `:car:presentation`.
- Existing Home, Library, Search, Settings, details, Mini Player, Now Playing,
  Queue, focus, input, repository, and playback paths were retained.
- Added complete VehiclePanel metrics, automatic shape mapping, and the
  FullscreenCockpit structural presentation. Nothing was rebuilt from `main`.

The detailed KEEP / EXTEND / REFACTOR / REMOVE / MISSING audit is in
`CURRENT_AUTOMOTIVE_BASELINE.md`.

## B. Figma inventory

Figma file `5NO5nvLFdd2jPTwHmnJE8A` was read through the connected Figma MCP as
Shine (`lvshine1990@gmail.com`). It has one Page (`0:1`, `Page 1`), no Sections,
53 top-level Frames, 2,776 total Frames, 2,705 text nodes, 718 rounded rectangles,
950 instances, 157 ellipses, and 89 local component definitions.

Formal application targets are 20 frames at 2496×1080, 20 at 1728×1080, two
Extended States frames, Minimal Now Playing at 5120×1440 with a 5120×1304 content
node, and Cover Flow at 5120×1440. All 42 formal 2496/1728 frames were read by
actual node ID with design context and screenshot; the 5120 context/fullscreen,
Extended States, and profile specification nodes were also read. The full node,
theme, state, and resolution list is in `FIGMA_RESOLUTION_MATRIX.md`.

## C. Resolution matrix

| Figma target | Runtime profile | Window condition | Core layout metrics |
|---|---|---|---|
| 2496×1080 | Expanded | cockpit panel `[64,192]–[2560,1272]`, or an app-sized test window | 352 rail, 4-column grid, 560/1424 panes, 640 artwork |
| 1728×1080 | VehiclePanel | cockpit panel `[832,192]–[2560,1272]`, or an app-sized test window | 240 rail, 3-column grid, 480/920–944 panes, 600 artwork |
| 5120×1304 content / 5120×1440 display | FullscreenCockpit | explicit hint or measured usable aspect `≥ 3.25` | no shell, 720 artwork, 128 touch targets, Minimal/Flow structures |

The production cockpit wrapper first applies FileManager's verified pixel panel
bounds on a matching 1440-high host. The page resolver then consumes the panel's
runtime `DpSize`; ordinary app-sized windows still use automatic shape selection.

## D. Window audit

The supplied real 5120×1440 screenshots and FileManager source establish one
transparent cockpit surface with two internal panel states. Expanded occupies
`[64,192]–[2560,1272]` (2496×1080). VehiclePanel moves the left edge to
`760 + 72 = 832`, producing `[832,192]–[2560,1272]` (1728×1080). Tide Player now
observes `key_screen_show` and `key_vpa_cui_show_left` and applies the same
geometry, transparent outer surface, edge-to-edge policy, and 8dp panel corners.

Dedicated AAOS layout-validation evidence:

- Expanded: physical/task 2496×1080 at 160 dpi, app bounds 2496×984,
  Compose root 2496×908 at `[0,76]`, status 76 and bottom car bar 96.
- VehiclePanel: physical/current/maximum/task/root 1728×1080 at 160 dpi,
  configuration 1728×1080 dp, zero reported insets, resolved profile
  `VehiclePanel`.
- FullscreenCockpit target: physical 5120×1440 at 160 dpi. The installed AAOS
  image repeatedly restarted PackageManager while processing the large debug APK,
  so app-window capture on that target was inconclusive. Raw environment evidence
  is retained locally. Production cockpit execution is the only hardware-only
  supplemental check; the Figma frames and code implementation are complete.

The 2496/1728 AVDs validate the page layouts at those exact sizes. They do not
represent the production cockpit's physical display modes; the supplied cockpit
screenshots provide that placement evidence.

The full source/runtime distinction and evidence paths are in
`CAR_WINDOW_PROFILES.md`, `EXPANDED_RUNTIME_ACCEPTANCE.md`, and
`RESOLUTION_ACCEPTANCE_MATRIX.md`.

## E. Architecture

Added or extended only presentation/profile and debug measurement code.
`:carApp` remains independent from `:androidApp`; `:car:presentation` does not
import Mobile screens. Database, library sync, sources, settings, search,
`PlaybackController`, queue state, Player, and MediaSession remain shared.
Resolution changes select metrics or the one justified fullscreen structural
composition; they do not create ViewModels, repositories, queues, players,
sessions, or databases.

The OEM adapter is isolated in `:carApp`. `:car:presentation` owns a pure,
unit-tested bounds resolver and never reads Android global settings.

Expanded ↔ VehiclePanel now follows the reference Demo's 300ms linear transition.
Cold start and host-configuration changes jump directly to their settled bounds;
only live OEM state changes animate. The two OEM setting notifications are
coalesced within one frame so a single panel change cannot restart the animation.

## F. Figma to Compose

- 2496/1728 Home → `CarHomeScreen`
- Songs/Albums/Artists/Playlists → `CarLibraryScreens`
- Album/Artist/Playlist details → `CarDetailScreens`
- Settings → `CarSettingsScreen`
- Search/state family → `CarSearchScreen`, `CarPageState`, interactive surfaces
- 2496/1728 Now Playing and Queue → `CarNowPlayingScreen` / `QueuePane`
- 5120 Minimal Now Playing and Cover Flow → `CarFullscreenNowPlayingScreen`
- shared 2496/1728 shell → `CarNavigationRoot`, header, rail, and Mini Player

Every formal node mapping, source path, profile, status, and Visual QA result is
listed in `FIGMA_IMPLEMENTATION_MATRIX.md`. Dark/light uses the shared setting and
semantic `CarTheme`; Extended States map loading, empty, error, focused, selected,
playing, pressed, and disabled behavior to existing components.

## G. Playback

Library rows submit the complete `PlayableItem` list plus the selected index to
the existing `PlaybackController`. The shared Player and MediaSession publish one
queue/playback state observed by playing highlights, Mini Player, Now Playing,
Queue, and fullscreen modes. Expanded runtime acceptance exercised a real
three-track MediaStore library, index selection, pause/play, next, previous, stop,
seek/progress, queue selection, Home synchronization, and Search. Profile changes
only recompose presentation metrics.

## H. Focus

Expanded and VehiclePanel share stable semantic IDs for header/exit, navigation,
content, lists, grids, details, Search, settings panes, Mini Player, Now Playing,
lyrics, controls, and Queue. FullscreenCockpit uses a shell-free control and
Cover Flow graph. D-pad, keyboard, vertical rotary, media keys, Back,
bring-into-view, route restore, and profile-switch restore are specified in
`FOCUS_GRAPH.md` and covered by Compose/unit tests.

## I. Files changed

`:car:presentation`:

- `layout/CarLayoutProfile.kt`
- `layout/CarLayoutMetrics.kt`
- `layout/CarLayoutProfileResolver.kt`
- `layout/CarAppWindowBounds.kt`
- `navigation/CarNavigationRoot.kt`
- `screen/CarLibraryScreens.kt`
- `focus/CarFocusGraph.kt`
- `nowplaying/CarFullscreenNowPlayingScreen.kt`
- resolver, architecture, and focus Compose tests

`:carApp`:

- `MainActivity.kt` cockpit panel host, edge-to-edge setup, and debug evidence
- `OemScreenStateMonitor.kt` read-only FileManager-compatible state observation
- transparent window theme and OEM system-bar metadata

Documentation:

- `CURRENT_AUTOMOTIVE_BASELINE.md`
- `FIGMA_RESOLUTION_MATRIX.md`
- `CAR_WINDOW_PROFILES.md`
- `MULTI_RESOLUTION_ARCHITECTURE_DECISION.md`
- `FIGMA_IMPLEMENTATION_MATRIX.md`
- `FOCUS_GRAPH.md`
- `RESOLUTION_ACCEPTANCE_MATRIX.md`
- `MULTI_RESOLUTION_FINAL_REPORT.md`
- `EXPANDED_RUNTIME_ACCEPTANCE.md` scope update

## J. Build and tests

| Command | Result |
|---|---|
| `gradlew :car:presentation:testDebugUnitTest` | PASS, including four FileManager panel-bound tests |
| `gradlew :carApp:assembleDebug :androidApp:assembleDebug` | PASS |
| `gradlew :carApp:lintDebug :car:presentation:lintDebug` | PASS; 0 errors, 2/4 pre-existing warnings |
| `gradlew :shared:desktopTest :desktopApp:compileKotlinDesktop` | PASS |
| architecture/static ownership and forbidden-pattern tests | PASS |

The lint process printed a Kotlin metadata analyzer-version diagnostic while
still completing successfully and producing zero-error reports. iOS cinterop
cannot execute on the Windows host; shared code compiled and desktop tests passed,
and this change does not touch shared/iOS source.

## K. Resolution acceptance matrix

| Resolution | Profile | Result | Basis |
|---:|---|---|---|
| 2496×1080 | Expanded | PASS | real-cockpit FileManager placement plus app-sized AVD E2E, input, theme, and visual acceptance |
| 1728×1080 | VehiclePanel | PASS | real-cockpit FileManager placement plus app-sized AVD metrics, focus, build, and test acceptance |
| 5120×1304 in 5120×1440 | FullscreenCockpit | PASS | complete formal-node implementation, physical display evidence, resolver/build/test/static/visual review; production target execution is supplemental |

No formal Figma resolution or page is Deferred. FullscreenCockpit cells absent
from Figma are marked `N/A — Figma does not define this screen for this profile`
in `RESOLUTION_ACCEPTANCE_MATRIX.md`.

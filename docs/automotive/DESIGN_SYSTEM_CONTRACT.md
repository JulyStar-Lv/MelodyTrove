# Automotive design system and focus contract

Status: **implementation contract for Expanded; VehiclePanel values and final px-to-dp calibration remain deferred**

## Decision

Create a car-local design system in `:car:presentation`. It may reuse Compose primitives and repository font/icon assets, but it must not inherit mobile layout tokens or mobile screen structure. The public entry point is `CarTheme`; components consume semantic colors, type roles, shapes, spacing, touch targets, and `CarLayoutMetrics`.

Use an enum for the stable profile identity and an immutable data class for metrics:

```kotlin
enum class CarLayoutProfile {
    Expanded,
    VehiclePanel,
}

@Immutable
data class CarLayoutMetrics(
    val profile: CarLayoutProfile,
    val usableSize: DpSize,
    // Named shell, pane, spacing, component and touch metrics.
)
```

`VehiclePanel` is a resolver/result boundary only in Phase 4/5. Its page metrics are not populated from the hidden 1728 frames in this phase.

## Evidence

- Design System `3:139` declares spacing `4,8,12,16,24,32,40,48,64,80`, type roles `64 Bold`, `56 Bold`, `34 SemiBold`, `26 Regular`, and focus as a `3-4px semantic border, no glow`.
- Color System `95:1010` defines named Dark/Light semantic swatches but contains selected-color label/fill conflicts. Actual target screen nodes show neutral selected surfaces.
- Components `3:40` and `90:818` cover NavigationItem, PlaybackButton, AlbumCard, SongRow, NowPlayingSummary, BasicComponent, preferences, and icon inventory.
- States `3:523` and `90:1097` cover Navigation Default/Selected/Focused/Pressed/Disabled; SongRow Default/Playing/Focused/Pressed/Disabled; BasicComponent Default/Pressed/Focused/Disabled; and preference states.
- Existing repository style is `AppTheme` + Miuix + immutable token data classes and composition locals in `core/presentation/.../theme/Theme.kt`. Car presentation should follow that Kotlin/Compose pattern without adding its tokens to the shared mobile theme.

## Affected Modules

- `:car:presentation`: owns theme, layout metrics, components, focus graph, and car screen presentation.
- `:carApp`: owns Android window/inset measurement and passes usable bounds into the resolver.
- `:core:presentation`: resource reuse only where a font or exact glyph is proven compatible; no car layout tokens added here.

## Affected Files

Proposed files under `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/`:

```text
theme/CarTheme.kt
theme/CarColors.kt
theme/CarTypography.kt
theme/CarShapes.kt
theme/CarSpacing.kt
theme/CarDimensions.kt
layout/CarLayoutProfile.kt
layout/CarLayoutMetrics.kt
layout/CarWindowProfileResolver.kt
focus/CarFocusGraph.kt
focus/CarFocusRegistry.kt
focus/CarFocusIndicator.kt
icon/CarIcons.kt
component/CarAppWindowHeader.kt
component/CarNavigationRail.kt
component/CarMiniPlayer.kt
component/CarSongRow.kt
component/CarAlbumCard.kt
component/CarArtistCard.kt
component/CarPreference.kt
```

Tests belong under the matching `commonTest` or Android test source set. The exact package root may follow the final module scaffold, but responsibilities must remain car-local.

## Files Not To Modify

- `core/presentation/.../theme/Theme.kt`, `Color.kt`, and `Type.kt` to force Automotive values onto mobile/desktop/iOS.
- Mobile Navigation, Home, Library, Now Playing, Queue, Settings, Search, or Dialog composables.
- Playback, Queue, database, repository, source, or Settings domain APIs for visual needs.
- Figma or hidden 1728 frames.

## Implementation Contract

### Theme surface

`CarTheme` must provide:

```text
CarColors
CarTypography
CarShapes
CarSpacing
CarDimensions
CarTouchTargets
CarLayoutMetrics
CarFocusVisuals
```

Use immutable value objects and composition locals, following the repository's existing `DesignTokens` pattern. Color mode comes from the shared user setting/system decision, then selects an explicit Automotive Dark or Light palette. Dynamic artwork color must not replace the fixed brand/focus colors on safety-critical controls.

### Color contract

The semantic token set must include at least:

```text
backgroundBase
backgroundSubtle
panel
surface
surfaceContainerHigh
surfaceContainerHighest
surfaceSelected
surfacePressed
textPrimary
textSecondary
textSummary
textActions
textDisabled
accentPrimary
accentSubtle
controlTrack
controlTrackDisabled
borderDefault
borderSubtle
focusBorder
error
```

Reference values printed by the Miuix-aligned board are:

| Token | Dark | Light |
|---|---|---|
| backgroundBase | `#000000` | `#FFFFFF` |
| backgroundSubtle | `#181818` | `#F7F7F7` |
| panel / surface | `#242424` | `#FFFFFF` |
| containerHigh / Highest | `#242424` / `#2D2D2D` | `#E8E8E8` / `#E8E8E8` |
| surfacePressed | `#393939` | `#E8E8E8` |
| textPrimary | `#F2F2F2` | `#000000` |
| textSecondary | white 80% | black 80% |
| textSummary | white 50% | black 60% |
| textActions | white 40% | black 40% |
| textDisabled | `#666666` | `#B2B2B2` |
| accentPrimary | `#FA2D48` | `#FA2D48` |
| accentSubtle | `#4B0E16` | `#FEE8EA` |
| controlTrack | `#505050` | `#E6E6E6` |
| controlTrackDisabled | `#3F3F3F` | `#F0F0F0` |
| borderDefault / Subtle | `#404040` / `#393939` | `#D9D9D9` / `#E0E0E0` |
| error | `#F12522` | `#E94634` |

Resolve the board conflicts as follows:

- `surfaceSelected` uses the actual neutral swatch/screen fill: Dark `#303030`, Light `#EDF0F2` or the exact paired screen-node value.
- `accentSubtle` keeps the printed red tints `#4B0E16`/`#FEE8EA` for accent-selected controls.
- Target Light screen nodes use `#FA233B` for several primary actions. Define a Light screen accent override only once in `CarColors`; do not scatter both reds through components. Visual regression decides whether the final Light value is `#FA233B` or the board's `#FA2D48`.
- When an older board and the target screen disagree, the target screen node wins.

### Typography contract

Define semantic roles rather than exposing raw sizes:

| Role | Reference design style |
|---|---|
| display | 64 Bold |
| headlineLarge | 56 Bold |
| pageTitle | 40 or 48 SemiBold according to mapped screen |
| titleLarge | 34 SemiBold |
| title | 28-32 SemiBold |
| bodyLarge | 26 Regular |
| body | 20-22 Regular |
| label | 16-18 Medium/SemiBold |
| supporting | 14-16 Regular |

Figma context reports Roboto Regular/SemiBold/Bold. The repository's shared Sans is Noto Sans SC. Phase 5 must render representative Chinese and Latin strings in both candidates and choose one explicit car family based on line breaks and glyph metrics. Until that check, typography values are reference design pixels, not `sp` literals.

### Spacing, shape, icon, and touch contracts

- Base spacing roles map to the design sequence `4,8,12,16,24,32,40,48,64,80`.
- Shape roles cover radii `8,14,16,18,20,22,24,26,28,32` plus full/circle. Components request a role such as `navigationItem`, `panel`, `card`, or `control`; they do not embed a radius.
- Icon assets use semantic names. The inventory includes Home, Library, Settings, Search, Refresh, Back, Shuffle, Previous, Play, Pause, Next, Repeat, Heart, Queue, Output, More, Close, Drag, ChevronRight, MusicNote, Source, Appearance, and Playing.
- Shuffle, RepeatOne, and RepeatAll must use the Figma-described Halcyon/Xiaomi HyperOS glyphs. Export the exact SVG/vector bytes before committing. Never redraw them from memory.
- Touch targets are component roles: exit `72`, navigation row `84` high, mini controls `56`, previous/next/queue `112`, and play/pause `128` design pixels. Runtime `Dp` targets are derived through metrics and must also satisfy platform accessibility minimums.

### Responsive Expanded metrics

The Expanded reference grid is a 2496 x 1080 design canvas. Derive a fit from the usable Compose box:

```text
referenceWidth  = 2496 design px
referenceHeight = 1080 design px
usableWidth     = measured Compose constraint width
usableHeight    = measured Compose constraint height
```

Do not apply a single transform scale. Instead, use the reference ratios to produce named `Dp` metrics, then let text, lazy grids, and panes lay out normally. Prefer width-derived horizontal metrics and height-derived vertical metrics with bounded min/max values. Preserve fixed relationships such as rail/pane proportions and accessibility touch targets.

The resolver API must accept usable constraints and inset policy as inputs. `Expanded` selection may initially be the only fully populated result. Do not encode exact `2496`/`1728` pixel equality. `VehiclePanel` can return an unsupported/deferred page metric state until its later contract is approved.

### Component state contract

State is orthogonal:

```text
enabled
selected
focused
pressed
playing
checked/on
```

Resolve visuals in this priority: Disabled, Pressed, Focused overlay/border, Selected or Playing semantic content, Default. Focus must remain visible when the item is also Selected, Playing, or On. Figma lacks several combined examples, so combine semantic layers instead of inventing new colors for every Cartesian state.

Focus uses a 3-4 design-pixel semantic border and no glow. Do not communicate focus by color alone; preserve shape/border contrast in both themes. Touch interaction must not permanently leave a keyboard/rotary focus ring visible unless the platform input mode requires it.

### Focus graph

Use explicit semantic focus IDs and a route-level registry. Index-based IDs are forbidden for songs/queue because sorting and playback changes reorder items.

Shared shell:

```text
Exit
  Down -> Home navigation item
  Right -> route content entry

Navigation vertical order
  Home -> Playlists -> Settings -> Songs -> Albums -> Artists -> MiniPlayer

Navigation item Right
  -> route's remembered content target, otherwise route content entry

Leftmost content Left
  -> selected navigation item

MiniPlayer
  summary/open -> Previous -> PlayPause -> Next (spatial row)
```

Initial focus is the selected route's navigation item on first entry. Returning from a child/detail route restores the semantic opener. Switching routes restores the last valid focus ID for that route; if the item disappeared, fall back to the route content entry, then selected navigation item.

Lists and grids use spatial traversal without wraparound. Moving to an off-screen target calls `bringIntoView` before or with focus transfer. At the left edge, Left returns to navigation. At the right or vertical end, focus stays on the current valid target unless an explicit adjacent pane exists.

Now Playing:

```text
Collapse/Back control
  Down -> Favorite or first player action

Secondary row
  Favorite <-> Quality <-> More

Playback row
  Repeat <-> Previous <-> PlayPause <-> Next <-> Queue

Player pane Right
  -> current lyric line or queue row

Lyrics/Queue pane Left
  -> last focused player control
```

Opening Queue keeps focus on the Queue control until the pane is composed, then moves to the playing queue row, otherwise the first row. Closing Queue restores the Queue control. A queue-track activation moves playback and keeps/restores focus by stable track/queue ID.

Settings uses pane traversal: selected category Right goes to the remembered preference in the detail pane; the first-column boundary returns Left to its category. Switch activation does not move focus.

Dialogs, once designed, must trap focus inside the modal, use an explicit initial action, dismiss on Back when allowed, and restore the exact opener. No background component may receive rotary/D-pad events while a dialog is active.

Back priority is Dialog -> Queue pane -> Detail page -> root route exit policy. Media Play/Pause/Next/Previous/Stop are handled centrally through the shared playback controller and do not move focus.

## Acceptance Criteria

- All car components read semantic tokens and named metrics; repeated raw dimensions/colors are absent from pages.
- Dark/Light mode changes colors without changing reference geometry.
- Reference-size screenshot comparison covers Home, Songs, Playlists, Albums, Artists, both detail pages, Settings, Now Playing, and Queue.
- Navigation, SongRow, BasicComponent, and preferences demonstrate every Figma-provided state plus meaningful combined Selected+Focused and Playing+Focused states.
- Focus is visible, has no glow, survives route/queue/dialog transitions, and never becomes trapped or disappears.
- Every focusable off-screen item scrolls into view; stable IDs restore focus after list changes.
- Touch, D-pad, rotary, keyboard, and hardware media keys share actions without creating duplicate playback state.
- No design pixel is assumed equal to dp or sp, and no exact display-pixel comparison selects a profile.

## Risks

- The user-authorized Phase 0 skip leaves runtime density, content bounds, and inset calibration unresolved. Metrics must remain replaceable after device capture.
- Figma token boards conflict internally; regression screenshots must settle the documented provisional overrides.
- Roboto versus Noto Sans SC can materially change Chinese layout.
- Figma lacks combined focus states and complete Search/Dialog/page-state designs. Those gaps need explicit design decisions before they can pass acceptance.
- Spatial focus defaults are often unstable in multi-pane lazy layouts. Omitting explicit `focusProperties`, stable IDs, and restoration will create cross-column jumps.


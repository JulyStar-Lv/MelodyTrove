# Automotive multi-resolution architecture decision

## Decision

Keep one repository, one runtime graph, one independent `:carApp`, and one isolated `:car:presentation`. Resolve the measured Compose content rectangle to one of three presentation profiles, then supply centralized `CarLayoutMetrics` to shared screen composables. Use a structural fullscreen composable only where Figma changes the information architecture.

```text
Window/Compose bounds + insets
        -> CarLayoutProfileResolver
        -> CarLayoutProfile
        -> CarLayoutMetrics
        -> shared state/actions + profile-aware Compose
```

## Runtime profiles and resolver contract

- `Expanded`: the wide app-window shape represented by 2496×1080.
- `VehiclePanel`: the narrower app-window shape represented by 1728×1080 when the vehicle panel is visible.
- `FullscreenCockpit`: the panoramic 5120×1304 app content surface represented by the 5120×1440 fullscreen designs.

The automatic resolver uses aspect-ratio bands from measured content constraints. It never checks exact width or height, never treats Figma px as dp, and never scales an entire root. An OEM state hint can still override automatic selection through the existing pure `carLayoutHintFromOemState` contract.

The verified FileManager cockpit placement is a separate host-window contract.
On a measured 1440-high 2560 driver canvas or 5120 cockpit canvas,
`CarAppWindowBoundsResolver` uses the supplied real-screen pixel bounds
`[64,192]–[2560,1272]` and `[832,192]–[2560,1272]`. Those constants do not select
page layout by width; they place the app panel. App-sized and other windows bypass
this contract and continue through automatic constraint-based resolution.

## Expanded ↔ VehiclePanel motion

The reference Demo uses a MotionLayout transition with a 300ms linear
interpolator. It jumps directly to the initial state on resume and animates only
subsequent state changes. Tide Player mirrors that policy with one synchronized
Compose transition over the panel's left, top, right, and bottom pixel edges.
The fixed right edge prevents a one-pixel seam while width changes.

The OEM monitor coalesces changes to the two global keys within one 16ms frame
and `StateFlow` suppresses identical states. This prevents duplicate system
notifications from restarting the transition. Changes between cockpit and
ordinary window hosts snap directly because those are configuration changes, not
the user-visible vehicle-panel motion.

## Metric variants

Expanded and VehiclePanel share navigation, Home, Library, Mini Player, Settings, Search, details, Now Playing, Queue, state holders, and actions. Profile metrics select rail/content widths, pane gaps, artwork, row heights, padding, touch targets, and 4-versus-3-column media grids.

## Structural variants

`FullscreenCockpit` omits app header/navigation/mini-player because Figma omits them. It renders the shared playback state as either Minimal Now Playing (`1480:1130`) or Cover Flow (`1524:1130`). Both modes use the existing `PlaybackController`, `NowPlayingRepository`, queue, artwork repository, Media3 Player, and MediaSession.

## Shared and profile-specific components

Shared: semantic theme/tokens, artwork, interactive surfaces, navigation items, preference rows, song rows, player state, playback commands, repository state, stable keys, and focus IDs.

Profile-specific: numeric `LayoutReference` data and `CarFullscreenNowPlayingScreen`. No profile-specific ViewModel, repository, player, queue engine, database, or session is introduced.

## Existing code disposition

Keep the module graph, Koin runtime composition, all domain/runtime ownership, route state, and focus coordinator. Refactor only the layout resolver's Expanded-only construction. Extend media-grid metrics and add the fullscreen presentation. Mobile/Desktop presentation files and playback runtime ownership are outside this change.

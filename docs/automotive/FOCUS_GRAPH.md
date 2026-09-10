# Automotive focus graph

## Input contract

`CarFocusHost` owns one `CarFocusCoordinator` for the active Automotive
presentation. Every focusable surface registers a stable semantic `CarFocusId`.
Focused nodes are remembered by navigation route, brought into view, and restored
after route or `CarLayoutProfile` changes. `carInputRouter` maps D-pad directions to
Compose focus traversal, vertical rotary motion to up/down traversal, and media
play/pause, next, previous, and stop keys to the shared `PlaybackController`.
Touch and keyboard activation use the same interactive surfaces.

## Expanded and VehiclePanel

These profiles share the same semantic graph. Their rail widths, pane ratios, row
sizes, grid columns, and spacing come from profile metrics, so resizing does not
replace route or playback state.

```text
Exit
  ↓
Home ↔ Home content
  ↓
Playlists ↔ Playlist content / detail
  ↓
Settings ↔ Settings main pane ↔ Settings detail pane
  ↓
Songs ↔ Song list
  ↓
Albums ↔ Album grid / detail
  ↓
Artists ↔ Artist grid / detail
  ↓
Mini Player ↔ active route content
```

Search opens from Home and starts at `search.field`; results use stable item IDs.
Detail screens start at `detail.back`, then move through play-all and stable rows.
Now Playing starts at `now_playing.collapse`; shuffle, more, repeat, previous,
play/pause, next, queue, lyric, and queue-item targets remain reachable through
explicit links or Compose spatial traversal. Back returns details to their parent,
Now Playing to its previous root route, and other routes to Home.

## FullscreenCockpit

This structural profile has no app shell or mini player. Minimal Now Playing starts
at `fullscreen.toggle`; previous, next, and cover-flow mode share the same playback
state. Cover flow registers `fullscreen_cover.<queue-index>` for every visible
item. Back exits cover flow and returns to minimal Now Playing.

```text
Previous ↔ Play/Pause ↔ Next ↔ Cover Flow
                              ↓
                Queue item -2 … current … +2
```

## Profile-switch behavior

The profile is a `CarFocusHost` effect key. After a constraints change, the host
waits for the replacement nodes to lay out, restores the last registered semantic
target for the route, and falls back to that route's initial focus when the old
node is absent. The same coordinator survives Expanded ↔ VehiclePanel changes.
Fullscreen uses its own route names because its structure intentionally replaces
the shell.

## Verification

- `CarNavigationFocusComposeTest` covers D-pad movement, rotary movement, and
  focus restoration across Expanded → VehiclePanel.
- `CarFocusGraphTest` covers semantic registration, route memory, removal, and
  fallback.
- Expanded AAOS acceptance exercised play/pause, next, previous, and stop media
  keys through the shared controller and MediaSession.

# Automotive Expanded focus and input contract

## Evidence and ownership

The local FileManager reference uses the classic Android View focus engine and
Activity/widget key handlers; it does not contain a reusable global focus graph.
Its OEM setting logic remains the accepted Phase 0 source for window-profile
inference only. TidePlayer therefore owns one Compose focus coordinator in
`:car:presentation` and does not copy FileManager input code.

`CarFocusCoordinator` owns stable focus requesters, route-level focus memory and
restoration. Every interactive target exposes a semantic `CarFocusId`; list targets
derive IDs from domain IDs or stable queue keys. Focused lazy-list items call
`bringIntoView`, so D-pad and rotary traversal follows scrolling content.

## Expanded graph

- Initial root focus is the selected navigation item. Search starts at its field,
  a detail starts at Back, and Now Playing starts at Collapse.
- Exit moves down to Home. Navigation moves vertically in the visible rail order;
  Artists moves down to the mini player.
- The selected navigation item moves right to the first interactive target for its
  page. Content targets move left to the owning navigation item.
- Content lists and rows use stable IDs and geometric traversal internally. Their
  focus is remembered per root/detail route.
- Opening a detail retains the opener in its parent route. Back restores the opener
  when it still exists and otherwise falls back to the root navigation item.
- The mini player is reachable from the rail and moves right into the active page.
  Activating it opens Now Playing.
- Now Playing explicitly links Collapse, Shuffle, More, Repeat, Previous,
  Play/Pause, Next and Queue. Opening Queue moves focus to the current item, or the
  first item when no current index exists. Closing Queue restores its button.
- Page Back is handled by the navigation root. Queue Back closes only the queue;
  detail Back closes only the detail; Now Playing Back returns to its source page.
- A future dialog must introduce its own focus scope, initial action and opener
  restore target before it is accepted. Phase 1 contains no modal dialog.

## Input mapping

The root key router converts D-pad directions to Compose focus moves. Enter/Center
and keyboard activation continue through Compose click semantics so touch and key
activation call the same action. Automotive rotary controllers that emit focus
forward/back or D-pad events use the same focusable graph.

Media Play/Pause, Next and Previous call the shared `PlaybackController`. Media Stop
uses the repository's resumable-pause behavior, matching `TidePlayerSessionPlayer`,
which intentionally defines external STOP as pause. The runtime-owned Media3
session remains the system/headset/Bluetooth media-key owner while the Activity is
not foreground.

Runtime direction, rotary, focus-ring and restoration acceptance still requires an
AAOS device or emulator. Build and unit evidence proves the graph policy and wiring,
not physical-controller behavior.

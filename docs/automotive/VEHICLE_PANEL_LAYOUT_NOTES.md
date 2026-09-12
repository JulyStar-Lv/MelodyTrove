# VehiclePanel layout notes

Status: profile selection contract accepted from FileManager code; secondary page implementation remains deferred.

## Evidence boundary

FileManager proves that two OEM global settings select an internal `left` MotionLayout state. It does not prove an Activity window resize to `1728 x 1080`.

Within `activity_main_scene`, the left-state background begins at `760dp + 72dp = 832dp` and ends at the parent right edge. On an assumed `2560dp` logical parent this yields `1728dp`. The assumption and runtime unit conversion remain unverified. See `CAR_WINDOW_PROFILES.md` for evidence and line references.

The actual system behavior could be a resized task, another window, SystemUI overlay, occlusion inset, or surface crop. For current development, an Android/OEM adapter may translate the FileManager state to an injected `VehiclePanel` hint while Compose still uses its measured constraints and insets. Target measurements remain required for final runtime acceptance.

## Later profile impact

When VehiclePanel page work begins, `CarLayoutProfile.VehiclePanel` may require different metrics or structure for:

- Navigation width and focus entry/exit paths.
- Content gutters and safe left edge.
- Grid column count and card width.
- Mini-player width and control density.
- Now Playing artwork, metadata, lyrics, and queue proportions.
- Settings column ratio.
- Dialog anchor/maximum width.
- Scroll restoration and rotary/D-pad traversal.

The business state, repositories, ViewModels, database, source stack, playback controller, player, MediaSession, and queue engine remain shared.

## Resolver boundary

Do not implement exact checks such as `widthPx == 1728` or fixed `1728.dp` roots.

The accepted resolver contract is:

- An injected OEM hint selects `Expanded` or `VehiclePanel`; no exact pixel check or guessed width threshold selects the profile.
- If no OEM hint is available, production defaults to `Expanded`.
- Every profile consumes the current measured usable Compose constraints and insets.
- `Settings.Global` access stays outside `:car:presentation` behind a platform adapter.

## Deferred acceptance

VehiclePanel page work may begin only after synchronized Expanded/VehiclePanel captures prove:

- Window and task bounds before and after the change.
- Root measured size and `Configuration` dp before and after the change.
- Insets/occlusion before and after the change.
- Density and px-to-dp conversion.
- The producer/meaning of the OEM state signal or an observable public equivalent.

Until then, preserve only the profile and metrics extension boundary; do not create duplicate pages or business layers.

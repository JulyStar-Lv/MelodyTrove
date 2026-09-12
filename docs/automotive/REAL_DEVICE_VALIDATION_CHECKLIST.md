# Automotive real-device validation checklist

This checklist records work that must be verified on the connected 5120×1440
cockpit after the next install. A checked source/build item is not a substitute
for real-device visual acceptance; every runtime item below remains pending until
new screenshots and task/window evidence are captured.

## Test setup and evidence

- [ ] Install the latest `carApp-arm64-v8a-debug.apk` after setting the required
  device model property, then restore the property immediately after installation.
- [ ] Launch Tide Player by tapping its real AppList entry, not with `am start`.
- [ ] Before and after every profile change, record `wm size`, `wm density`,
  `dumpsys activity activities`, app/window bounds, Compose root bounds, and the
  OEM `key_screen_show` / `key_vpa_cui_show_left` values.
- [ ] Capture lossless PNG screenshots for every checkpoint. Compare them at
  native size with the corresponding Figma frame; record overlay/difference images.
- [ ] Confirm app data, playback queue, artwork and settings survive the test path
  unless a clean-data scenario is being tested explicitly.

## AppList and window/task behavior

- [ ] AppList shows the same red Tide Player music-note artwork as the phone/desktop
  product and does not display it as a circular icon.
- [ ] AppList opens the normal application in the OEM-assigned panel without a
  duplicate Tide Player task or stale previous-version window.
- [ ] Entering fullscreen from a Tide Player instance opened by AppList creates the
  dedicated fullscreen playback task; it must not inherit the AppList half-screen
  TaskDisplayArea.
- [ ] Fullscreen Now Playing fills the 5120-wide cockpit content area and leaves only
  the OEM-reserved bottom vehicle area described by the Figma frame.
- [ ] Cover Flow remains in that same fullscreen task and never falls back to the
  2496/1728 panel.
- [ ] Exiting Cover Flow returns to fullscreen Now Playing; exiting fullscreen
  returns to the same normal Tide Player page without relaunch or state loss.

## Expanded ↔ VehiclePanel

- [ ] With `key_screen_show == 0` and `key_vpa_cui_show_left != 1`, Expanded is
  selected and the visible app target is the Figma 2496×1080 profile.
- [ ] With `key_screen_show != 0` or `key_vpa_cui_show_left == 1`, VehiclePanel is
  selected and the visible app target is 1728×1080.
- [ ] Change both OEM keys independently and in combination. Each observer update
  must produce one stable profile change without missed events or duplicate loops.
- [ ] During both transition directions, layout follows the configured animation,
  with no black/white frame, stretched artwork, clipped navigation, overlapping
  content, focus loss, or unnecessary whole-screen recomposition.
- [ ] Playback, queue position, current route, scroll position and selected settings
  remain unchanged across the profile transition.

## Now Playing at 2496×1080 and 1728×1080

- [ ] Expanded dark/light matches Figma `1:65` / `89:566`; VehiclePanel dark/light
  matches `1886:2376` / `1886:2808`.
- [ ] Expanded collapse button is at design `(56, 48)`, and enter-fullscreen is
  vertically below it at `(56, 136)`.
- [ ] VehiclePanel collapse button is at design `(40, 48)`, and enter-fullscreen is
  vertically below it at `(40, 136)`.
- [ ] Both controls are 72×72 with 56×56 glyphs, correct icons, corner radius,
  border, background, focus order and click target.
- [ ] The down-arrow control exits the Now Playing page in both panel profiles;
  it does not only change display mode or leave a stale Now Playing route behind.
- [ ] The right pane shows title, `artist · album`, then the 2dp divider before
  lyrics. Non-current lyric lines have the Figma 3dp blur while the current line
  remains sharp.
- [ ] Artwork, favorite/more controls, lossless badge, progress, timestamps,
  repeat/previous/play/next/queue controls, metadata, lyrics and queue state match
  their profile-specific Figma geometry.

## Fullscreen Now Playing and Cover Flow

- [ ] Minimal fullscreen Now Playing matches Figma `1480:1130`, including controls,
  track metadata, 720 artwork, lyric baselines and background treatment.
- [ ] The upper-left down-arrow and exit-fullscreen controls are both present at
  `(96, 216)` and `(184, 216)`, with the title and full artist name beginning at
  `(280, 228.5)`.
- [ ] The down-arrow exits the complete playback UI and returns the underlying
  normal page; the adjacent control exits fullscreen but keeps normal Now Playing.
- [ ] Minimal fullscreen artwork is clipped to the Figma 36dp corner radius.
- [ ] Cover Flow matches Figma `1524:1130` at 5120×1440.
- [ ] Every Cover Flow artwork is visibly clipped to a consistent 28dp rounded
  rectangle throughout its scale and 3D rotation; no square corner flashes appear.
- [ ] Drag slowly in both directions, fling repeatedly, reverse direction mid-drag,
  and cross enough items to recycle the visible range. No cover or full frame may
  turn white, disappear, show the wrong bitmap, or reload all visible artwork.
- [ ] Drag is finger-following; release settles in 220ms without snapping backward.
  Side-cover scale, spacing, opacity, z-order and perspective remain continuous.
- [ ] Left and right side covers retain the Figma safe distance and are not clipped
  by the physical display edge or OEM overlay.
- [ ] Dragging only browses. Tapping a cover starts that queue item and updates title,
  artist, artwork and playback state exactly once.
- [ ] Cover title and artist blocks remain centered beneath every scaled cover;
  neither line is vertically clipped, and long artist names use the full designed
  width before ellipsis.
- [ ] Test queues with 0, 1, 2, 3 and many items, plus missing/corrupt artwork and
  rapid next/previous changes while Cover Flow is open.

## Card surfaces and visual consistency

- [ ] No content card has a floating drop shadow/elevation in Home, Search, Library,
  Songs, Albums, Artists, Playlists, Settings, Album detail, Artist detail, Playlist
  detail, Now Playing, Queue, or fullscreen modes.
- [ ] Cards retain their intended solid/translucent background, radius, border,
  selected/pressed/focused feedback, spacing and clipping after shadow removal.
- [ ] Home Recent Added feature and compact cards use background only and do not
  show shadow halos or clipped shadow corners.
- [ ] Settings groups remain visually merged: Appearance/Language/Lyrics and the
  four Library/Data rows each form a continuous background without missing corners.
- [ ] Album and Artist detail hero/content panels use background separation only;
  their fade masks and artwork clipping remain intact.

## Page coverage

- [ ] Check dark and light themes for Home, Search, Library overview, Songs, Albums,
  Artists, Playlists, Settings and every settings detail page in both panel profiles.
- [ ] Check album, artist and playlist detail pages, including empty/loading/error,
  long Chinese/English metadata, scrolling, selection and playback entry.
- [ ] Confirm Library navigation contains only the Figma-specified entries and does
  not add Songs/Albums/Artists as an extra left-side submenu.
- [ ] Confirm the default “我的收藏” playlist exists and opens correctly.
- [ ] In Playlists, “我的收藏” uses the 136px selected-card treatment (4×104
  accent marker, 104px rounded artwork, blurred cover background and dark
  readability wash); ordinary playlist cards are 240×292 with 216px artwork.
- [ ] Library Songs/Albums/Artists use one 496×56 segmented container with 4px
  padding/gaps and 48px segments. The upper-right Search shortcut is absent.
- [ ] The current song row uses the four-bar 22×16 Figma playing indicator, with
  staggered 800ms reverse animation, rather than a music-note character.
- [ ] Check Mini Player, normal Now Playing, Queue, fullscreen Now Playing and Cover
  Flow as one continuous playback path, including Back and hardware/rotary focus.

## Current implementation status before device reconnection

- [x] Cover Flow uses rounded artwork clipping, stable virtual-item composition keys,
  and a camera distance larger than the rotated artwork depth.
- [x] Now Playing collapse/fullscreen controls use the Figma vertical arrangement
  for both panel profiles.
- [x] Now Playing metadata/divider/lyric blur, fullscreen dual controls and rounded
  artwork are implemented from the referenced Figma nodes.
- [x] Playlist selected/thumbnail cards, Library segmented tabs, and the four-bar
  playing indicator use the measured Figma geometry and motion parameters.
- [x] The Library upper-right Search shortcut has been removed.
- [x] Explicit Compose card shadows/elevations were removed from the car presentation
  module while retaining card backgrounds and shapes.
- [x] `:car:presentation:testDebugUnitTest` and `:carApp:assembleDebug` pass.
- [ ] All runtime and visual checks above await a connected real device.

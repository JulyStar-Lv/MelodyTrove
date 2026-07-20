# Design QA

- Source: Figma Make, `Design System for TideTunes`, Version 31.
- Mobile: Home layout, Daily Picks, Recently Played, Continue Playing, mini player, and bottom navigation visually checked against the source design; mini player and bottom navigation verified in both light and dark themes.
- Desktop: sidebar, responsive content layout, and bottom player visually checked at 1440 × 900; the removed App Cover page and removed top toolbar do not reappear.
- Interaction: Home, Settings, and Appearance & Home navigation verified in the browser.
- Collapsing sticky headers: page titles move upward and scale from the expanded state into a compact pinned bar; Home, Settings, and Design System verified after scrolling on mobile and desktop viewports.
- Daily Picks: animated four-point gradient verified in light and dark themes; motion changes over time and the now-playing label updates with the selected song.
- Home sections: Recently Added, Recommended Artists, and Pinned Playlists verified on mobile with the same data and cards as the landscape layout.
- Recently Played: ranked tracks verified in three swipeable pages with recency, duration, page indicators, and artwork playback animation without row highlighting; no outer card, progress bar, or persistent overflow menu remains.
- Continue Playing: six cards verified using the PlaylistCard presentation without track count or duration; Pinned Playlists retains its metadata.
- Pinned Playlists: Favorite verified as the first card on mobile and desktop; it links to a liked song while the remaining playlist order stays unchanged.
- Hover behavior: card, genre, song, and slider hover-scale effects removed; a desktop album card remains at `transform: none` while hovered.
- Click consistency: pointer clicks no longer leave a persistent song-row focus ring; section headers open the matching Library category; card components use native button semantics; player icon controls use `0.92`, content cards `0.97`, rows/headers `0.985`, and primary actions `0.95`; keyboard focus remains `focus-visible` only.
- Mini player liquid glass: mobile and desktop variants verified in light and dark themes with shared translucent material, 30px backdrop blur, dynamic artwork-colored refraction, highlight rim, glass border, and theme-aware shadow/controls.
- Mini player dark mode: the light top-rim highlight is disabled and the remaining glass border/inset highlight is reduced to prevent a white line along the upper edge.
- Runtime: production build passes and Mini Player controls remain functional; an existing `ListeningSummaryCard` nested-button warning remains outside this change.

Final result: passed

## Library & sources optimization — 2026-07-20

- Product grounding: the current Settings scope is limited to Local and WebDAV management, scanning, and metadata backfill. OneDrive, Jellyfin, Plex, and Navidrome are not exposed on this screen.
- Before/after: `audits/library-sources/mobile-before.png` and `audits/library-sources/mobile-overview.png` at 390 × 844. The six tall standalone cards were replaced by a compact unified-library summary and one manageable source list.
- Responsive implementation: `audits/library-sources/desktop-overview.png` at 1440 × 900 verifies the same hierarchy without unnecessary multi-column card repetition.
- Scanning state: `audits/library-sources/mobile-scanning-menu.png` verifies Automatic scan, the content-fit HyperOS choice menu, metadata scan guidance, maintenance actions, and mini-player clearance.
- Functional fidelity: the screen exposes only Local and WebDAV, presents WebDAV as the only addable source, adds Standard-mode behavior text matching repository documentation, and includes both missing-artwork and missing-lyrics backfill actions.
- Add source flow: `audits/add-source/mobile-form.png`, `audits/add-source/mobile-added.png`, and `audits/add-source/desktop-form.png` verify responsive WebDAV configuration, required-field errors, anonymous mode, password visibility, connection testing, disabled-before-test submission, and immediate list/summary updates after adding.
- WebDAV management: `audits/manage-webdav/mobile-editor.png`, `audits/manage-webdav/mobile-directories.png`, `audits/manage-webdav/mobile-delete-confirm.png`, and `audits/manage-webdav/desktop-editor.png` verify the clickable WebDAV management entry, editable connection details, connection retesting, selectable import directories, immediate source-summary updates, and guarded deletion. Local Storage remains read-only.
- Runtime: production build passes, the browser error log is empty, and the menu opens and closes without leaving an overlay behind.

Final result: passed

## HyperOS settings choice menu — 2026-07-19

- Source: `/var/folders/jc/z_g_5hld77g5zmm6bxv83_5c0000gn/T/codex-clipboard-9d9bde29-ee6a-43f5-8489-5e7aafab1ecb.jpg`.
- Implementation screenshots: `audits/hyperos-settings-choice/mobile-theme-menu.png` at 390 × 844 and `audits/hyperos-settings-choice/desktop-theme-menu.png` at 1440 × 900, both in the dark theme with the choice menu open.
- Comparison: the source and mobile implementation were inspected together. The implementation matches the reference pattern with a dimmed background, large 28px panel radius, vertically spaced full-row choices, a blue selected value, a right-aligned checkmark, and a compact up/down affordance on the setting row.
- Iteration history: the first pass exposed a light menu over the app's dark theme because the portal rendered outside the themed root; the menu now reads the triggering setting's theme and renders the correct material. A scroll-close listener that dismissed the menu during focus was removed. Lower-page menus now prefer opening upward when the trigger is in the lower half of the viewport.
- Interaction: Theme selection updates the displayed value and selected marker; Escape closes the menu and returns to the settings state. The same component covers language, playback, lyrics, scan, cache, network, and backup choices.
- Responsive and runtime: mobile and desktop placement verified, browser error log is empty, `git diff --check` passes, and the production Vite build succeeds.

Final result: passed

## Now Playing mobile and landscape refinement — 2026-07-20

- Source visual truth: `audits/reference-player-immersive/mobile-now-playing-final.png` plus the user's portrait and landscape layout constraints.
- Implementation screenshot: unavailable; the Codex in-app browser is open but no controllable browser capture surface is exposed in this task.
- Viewports: portrait `390 × 844`; landscape `844 × 390`.
- State: full player, player view, Midnight Cascade, playing.
- Full-view comparison evidence: blocked until a browser-rendered portrait and landscape capture can be made.
- Focused-region comparison evidence: blocked for the lyric scale, progress area, and five-control alignment for the same reason.

**Findings**

- [P1] Landscape composition has not been visually verified.
  Location: full player landscape branch.
  Evidence: the production build passes, but no browser-rendered `844 × 390` screenshot is available.
  Impact: overflow, crop, or optical-alignment issues could remain in a short landscape viewport.
  Fix: capture the player, lyrics, and queue states at `844 × 390`, then compare and correct any visible drift.

- [P2] Portrait spacing changes have not been visually compared.
  Location: portrait lyric preview, full lyrics, and five-button transport.
  Evidence: source values were reduced and control seats normalized in code, but no new `390 × 844` screenshot is available.
  Impact: the intended extra bottom breathing room and smaller lyric hierarchy are not yet visually proven.
  Fix: capture `390 × 844` and compare against the reference and user-requested adjustments.

**Implementation Checklist**

- Capture portrait and landscape player states in a browser.
- Verify all five transport controls share one optical center line and stay clear of the safe area.
- Verify portrait active lyrics at `28px`, surrounding lyrics at `23px`, and landscape lyrics at `24px` / `20px` do not clip.
- Exercise player-to-lyrics and player-to-queue transitions in both orientations.
- Check browser console errors, then update this report with the comparison evidence.

**Follow-up Polish**

- None classified until visual capture is available.

Final result: blocked

## Now Playing mobile queue transition refinement — 2026-07-20

- Source visual truth: the current TideTunes mobile full-player and queue states, with the queue treated as a secondary surface above the player.
- Implementation screenshot: unavailable; this task does not expose a controllable in-app browser capture surface.
- Viewport: portrait `390 × 844`.
- State: full player switching between player, lyrics, and queue views.

**Scoped Follow-up Changes**

- The queue now enters from the bottom with a short spring transition and exits along the same path.
- Player and lyrics remain stacked beneath the queue and use a subtle scale/opacity response, eliminating the opposing horizontal motion and layout jump.
- Full-screen mobile panels are absolutely stacked so simultaneous exit/enter animations no longer reflow the page.
- Reduced-motion users receive a brief opacity transition instead of spatial movement.
- The existing horizontal player-to-lyrics transition remains unchanged.
- The three-button lyrics/device/queue footer navigation was removed from both the portrait lyrics and queue screens, plus their landscape counterparts.
- Lyrics and queue now use one top-level back control so the player remains reachable without restoring the removed footer.
- Queue previous, play/pause, and next controls now match the main player's button seats, icon scale, translucent play surface, shadow, and pressed motion.
- Queue playback controls now reserve bottom safe-area spacing after the footer removal.
- Production build passes; browser screenshot and interaction comparison remain unavailable.

**Implementation Checklist**

- Exercise player-to-queue, queue-to-player, lyrics-to-queue, and queue-to-lyrics transitions at `390 × 844`.
- Confirm there is no background flash, header jump, or bottom-control misalignment during the spring settle.
- Check the browser console and repeat the interaction with reduced motion enabled.

Final result: blocked

## Now Playing desktop refinement — 2026-07-20

- Source visual truth: the existing TideTunes full-player desktop implementation and current product design system.
- Implementation screenshot: unavailable; this task does not expose a controllable in-app browser capture surface.
- Viewports: primary `1440 × 900`; compact desktop `860 × 520`; ultrawide width capped by a `1600px` content stage.
- State: full player, lyrics tab, Midnight Cascade, playing.
- Full-view comparison evidence: blocked until browser-rendered desktop captures can be made.
- Focused-region comparison evidence: blocked for the artwork/control column, lyrics panel, and compact-height layout.

**Findings**

- [P1] Desktop composition has not been visually verified.
  Location: full-player wide layout.
  Evidence: the production build succeeds, but no browser-rendered `1440 × 900` or `860 × 520` screenshot is available.
  Impact: optical balance, text wrapping, or compact-height overflow could still need refinement.
  Fix: capture both viewports, compare the artwork/control proportions and lyric panel density, then correct any visible drift.

**Implementation Checklist**

- Capture the player at `1440 × 900` and `860 × 520`.
- Verify artwork remains square and the full control stack stays visible at the compact height.
- Verify the centered header label has no trailing action, the lyrics/queue pill tabs remain balanced, and all keyboard focus states are visible.
- Check the queue tab, seek control, transport controls, and browser console.

**Scoped Follow-up Changes**

- Desktop progress and transport now reuse the mobile interaction set: repeat, previous, play/pause, next, and queue.
- The queue control switches the desktop content pane to the queue tab.
- The top-right playback-device control was removed.
- The lyrics/queue surface no longer has a card background, border, shadow, or backdrop blur.
- The desktop header now contains only the centered `Now Playing` label; the song title was removed from the header.
- The top-left overflow icon was removed and a vertical overflow control now sits beside Favorite using the same `48px` / `40px` mobile control seats and icon sizing.
- The desktop artwork height now reserves `360px` for metadata, progress, margins, and transport, keeping the full bottom control stack visible down to the `860 × 520` desktop threshold.
- The queue header no longer shows Shuffle, Repeat, or Autoplay shortcuts; the equivalent landscape queue shortcuts were removed for cross-orientation consistency.
- The desktop lyrics/queue pill switcher is hidden; the transport queue control now toggles between queue and lyrics so the removed switcher does not create a dead end.
- Production build passes after the scoped changes; browser screenshot comparison remains unavailable.

**Follow-up Polish**

- None classified until visual capture is available.

Final result: blocked

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

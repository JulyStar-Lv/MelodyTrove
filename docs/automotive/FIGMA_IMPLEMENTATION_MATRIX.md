# Figma to Compose implementation matrix

`PASS` means the formal Figma frame has a real Compose route/state backed by the
existing repositories and controller. The current code has been reconciled with
the listed Figma geometry, but visual acceptance remains pending until fresh
screenshots are captured on the real cockpit display.

| Figma nodes | Resolution | Screen/state | Compose source | Profile | Status | Visual QA |
|---|---:|---|---|---|---|---|
| `1:2`, `89:480`, `1877:1508`, `1886:2170` | 2496×1080 / 1728×1080 | Home, dark/light | `screen/CarHomeScreen.kt` | Expanded / VehiclePanel | PASS | PASS: shared structure, profile rail/padding metrics |
| `3:384`, `89:845`, `1879:1516`, `1879:2469` | 2496×1080 / 1728×1080 | Songs, dark/light | `screen/CarLibraryScreens.kt` | Expanded / VehiclePanel | PASS | DEVICE PENDING: segmented tabs and four-bar playing state aligned in code |
| `970:1176`, `970:2177`, `1879:2188`, `1879:3141` | 2496×1080 / 1728×1080 | Albums, dark/light | `screen/CarLibraryScreens.kt` | Expanded / VehiclePanel | PASS | PASS: 4-column / 3-column metric variant |
| `970:1376`, `970:2377`, `1879:2334`, `1879:3287` | 2496×1080 / 1728×1080 | Artists, dark/light | `screen/CarLibraryScreens.kt` | Expanded / VehiclePanel | PASS | PASS: 4-column / 3-column metric variant |
| `970:776`, `970:1777`, `1879:1909`, `1879:2862` | 2496×1080 / 1728×1080 | Playlists, dark/light | `screen/CarLibraryScreens.kt` | Expanded / VehiclePanel | PASS | DEVICE PENDING: selected Favorites and 240×292 cards aligned in code |
| `977:944`, `979:1012`, `1886:3240`, `1886:3659` | 2496×1080 / 1728×1080 | Album detail, dark/light | `screen/CarDetailScreens.kt` | Expanded / VehiclePanel | PASS | PASS: hero/list pane metric variant |
| `977:1189`, `979:1257`, `1886:3468`, `1886:3887` | 2496×1080 / 1728×1080 | Artist detail, dark/light | `screen/CarDetailScreens.kt` | Expanded / VehiclePanel | PASS | PASS: hero/list pane metric variant |
| `3:463`, `89:965`, `1879:1808`, `1879:2761` | 2496×1080 / 1728×1080 | Settings, dark/light | `screen/CarSettingsScreen.kt` | Expanded / VehiclePanel | PASS | PASS: 560/1424 and 480/944 panes |
| `1:65`, `89:566`, `1886:2376`, `1886:2808` | 2496×1080 / 1728×1080 | Now Playing, dark/light | `nowplaying/CarNowPlayingScreen.kt` | Expanded / VehiclePanel | PASS | DEVICE PENDING: metadata, divider, lyric blur and header controls aligned in code |
| `1:122`, `89:642`, `1886:2602`, `1886:3034` | 2496×1080 / 1728×1080 | Queue, dark/light | `nowplaying/CarNowPlayingScreen.kt` | Expanded / VehiclePanel | PASS | PASS: playing row and profile queue widths |
| `1480:1130` (`1480:1131` content) | 5120×1440 (5120×1304 content) | Minimal Now Playing | `nowplaying/CarFullscreenNowPlayingScreen.kt` | FullscreenCockpit | PASS | DEVICE PENDING: dual exit controls, metadata and rounded 720 artwork aligned in code |
| `1524:1130` | 5120×1440 | Cover Flow | `nowplaying/CarFullscreenNowPlayingScreen.kt` | FullscreenCockpit | PASS | DEVICE PENDING: controls, text blocks and draggable cover geometry aligned in code |
| `1922:1508`, `1927:1508` | multi-profile spec | Loading, empty, error, focus, selected, playing, disabled, dark/light | `screen/CarPageState.kt`, `component/CarInteractiveSurface.kt`, screen files | Expanded / VehiclePanel | PASS | PASS: semantic state mapping |
| Shell children in each 2496/1728 frame | 2496×1080 / 1728×1080 | Header, exit, rail, selection, mini player | `navigation/CarNavigationRoot.kt`, `component/*` | Expanded / VehiclePanel | PASS | PASS: 352/240 rail metric variant |

The Figma file contains no separate formal Search frame. Search is a repository-backed
Automotive route referenced by Extended States, so it is implemented and verified
as product behavior without inventing a Figma screen node. FullscreenCockpit has no
Home, library, queue screen, settings, search, shell, or mini-player frames; those
cells are `N/A` in the resolution acceptance matrix for this profile.

All implemented rows use real repository/controller state. Unsupported
recommendation cards show an explicit disabled/unsupported state. No fixture songs,
second player, second MediaSession, or car-only queue store is used.

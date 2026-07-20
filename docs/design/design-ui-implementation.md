# TideTunes Design UI implementation

This document maps the production Compose Multiplatform UI to the source-of-truth prototype in `Design/`.

## Navigation and adaptive shell

| Design surface | Production implementation |
| --- | --- |
| Mobile root navigation | `BottomBar.kt` + `TideBottomNavigationBar` |
| Mobile mini player | `PlaybackMiniPlayerHost` + `TideMiniPlayerBar` |
| Tablet navigation | `NavigationRailBar.kt` |
| Desktop navigation | `SidebarBar.kt` |
| Desktop player placement | Bottom of `HomeMainPane` |
| Root destinations | Home, Search, Library, Settings |
| Now Playing entry | Persistent mini player only |

The obsolete desktop top toolbar has been removed. Compact, medium, expanded, large, and XL layouts all use the same root destination model.

## Root pages

| Page | Compose implementation | Design coverage |
| --- | --- | --- |
| Home | `HomeDesignScreen.kt` | Daily Picks, Pinned Playlists, Your Listening, Continue Playing, Recently Played, Recently Added, Recommended Artists |
| Search | `SearchDesignScreen.kt` | Search field, history, genre grid, trending, loading/error/empty/results states, source-aware rows |
| Library | `LibraryDesignScreen.kt` | Playlists, songs, albums, artists, genres, folders, favorites, downloads, history, recently added/played, lossless, Hi-Res, sources |
| Settings | `SettingsScreen.kt` + shared settings components | Personalization, Playback, Library & Data, App Info |

## Secondary and detail pages

Album, Artist, Playlist, Now Playing, Lyrics, Queue, Downloads, Sources, Import, Plugin management, and all Settings subpages retain their existing business state and navigation. Their visual alignment is supplied by the shared components updated in this implementation:

- `TidePageHeader`
- `TideCardSurface`
- `TideSettingsGroup`
- `TidePreferenceRow`
- `TideMiniPlayerBar`
- `TideBottomNavigationBar`
- shared spacing, radius, elevation, blur, motion, and adaptive tokens

## Data rules

- UI screens must render repository/view-model state rather than demo music.
- An empty library displays actionable empty states and source/folder guidance.
- Library playback remains driven by `PlaybackController` and the full-library queue.
- Unsupported source providers must not be shown as active production integrations.
- Now Playing is not a fifth root tab.

## Validation checklist

- Mobile: 390 × 844 and narrow Android widths
- Tablet: 840–1279 dp with navigation rail
- Desktop: 1280+ dp with sidebar and bottom player
- Light and dark themes
- Long titles and translated strings
- Empty, loading, error, disabled, playing, and selected states
- Minimum interactive target: 48 dp where the component surface permits it

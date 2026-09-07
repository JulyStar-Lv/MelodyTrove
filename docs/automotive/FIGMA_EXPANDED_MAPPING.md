# Automotive Expanded Figma mapping

Status: **contract ready for Phase 4/5 implementation; visual acceptance remains pending runtime evidence and screenshots**

Figma file: `5NO5nvLFdd2jPTwHmnJE8A`, page `0:1`

Scope: the 2496 x 1080 Expanded design frames only. Every number below is a Figma design-pixel measurement. It is neither a dp value nor permission to fix a Compose root to 2496 x 1080.

## Decision

Use a dedicated `:car:presentation` component tree and map the 2496 x 1080 Figma composition through `CarLayoutMetrics`. Compose receives usable constraints, resolves a profile, and derives runtime dimensions from the profile's design proportions. It must not call `scale()` on a fixed canvas, compare an exact pixel width, or convert a design pixel to `1.dp`.

The user explicitly authorized work beyond the unresolved Phase 0 measurement gate. That authorization allows contract and implementation work; it does not establish density, insets, or a px-to-dp ratio. Runtime profile selection and final visual acceptance must remain measurable and replaceable.

The hidden 1728 x 1080 frames are evidence of a future VehiclePanel profile. They are excluded from this mapping and must not be copied into Phase 4/5.

## Evidence

### Read coverage

Every formal Expanded screen family received a targeted `get_design_context` read in both modes:

| Screen | Dark node | Light node |
|---|---|---|
| Home | `1:2` | `89:480` |
| Now Playing | `1:65` | `89:566` |
| Queue state | `1:122` | `89:642` |
| Songs | `3:384` | `89:845` |
| Settings | `3:463` | `89:965` |
| Playlists | `970:776` | `970:1777` |
| Albums | `970:1176` | `970:2177` |
| Artists | `970:1376` | `970:2377` |
| Album detail | `977:944` | `979:1012` |
| Artist detail | `977:1189` | `979:1257` |

Supporting boards read with design context: Design System `3:139`, Color System `95:1010`, Components `3:40`/`90:818`, States `3:523`/`90:1097`. `get_variable_defs` returned `{}` for the design system, color system, and Home nodes.

The bounded read log is in `build/automotive/figma/EXPANDED_READ_SUMMARY.md`.

### Shared shell mapping

| Figma node | Compose component | Proposed source path under `car/presentation/src/main/kotlin/io/github/julystar/musicapp/car/presentation/` | Expanded design metric |
|---|---|---|---|
| AppWindowHeader/Tide Player (`480:876`, mode peers) | `CarAppWindowHeader` | `component/CarAppWindowHeader.kt` | `(24,24) 352x72` |
| ExitButton/HitArea (`480:877`, peers) | `CarExitButton` | `component/CarAppWindowHeader.kt` | `72x72`; 56 icon centered with 8 inset |
| AppName (`480:881`, peers) | header title | `component/CarAppWindowHeader.kt` | local x `92`, y `12.5`; 40 type in screen contexts |
| NavigationRail (`3:196`, peers) | `CarNavigationRail` | `component/CarNavigationRail.kt` | `(40,112) 352x920`; inner width `320` |
| NavigationItem/* | `CarNavigationItem` | `component/CarNavigationRail.kt` | `320x84`; local x `16`; icon `56`; label begins x `94` |
| NavigationSectionTitle/音乐库 | `CarNavigationSectionLabel` | `component/CarNavigationRail.kt` | local `(32,356)`, 20 type |
| NowPlayingSummary (`3:208`, peers) | `CarMiniPlayer` | `component/CarMiniPlayer.kt` | local `(16,732) 320x164`; open region `320x88`; controls `56` |
| Icon/Glyph (`81:197`) | `CarIcon` semantic registry | `icon/CarIcons.kt` | 32 base grid; rendered sizes 24/32/36/40/56/72 by component |

Rail visual order is Home, Playlists, Settings, library label, Songs, Albums, Artists, then mini player. Route selection must change the selected state without changing this geometry.

### Home mapping

| Figma node | Compose component | Proposed source path | Expanded design metric |
|---|---|---|---|
| Home/ScrollableContent (`921:776`, `921:777`) | `CarHomeScreen` lazy vertical content | `home/CarHomeScreen.kt` | `(416,0) 2080x1080`; content extends to y `1660` |
| QuickCard/Guess You Like (`637:792`) | `CarHomeQuickCard` | `home/CarHomeQuickCard.kt` | local `(24,112) 576x160` |
| QuickCard/Daily Recommendation (`541:862`) | same component | `home/CarHomeQuickCard.kt` | `(624,112) 576x160` |
| QuickCard/Playback History (`541:871`) | same component | `home/CarHomeQuickCard.kt` | `(1224,112) 576x160` |
| ActionTile/Search (`715:784`, `715:789`) | `CarSearchEntryTile` | `home/CarSearchEntryTile.kt` | `(1824,112) 164x160`; icon 56 |
| Recent-added feature card (`3:238`) | `CarRecentAlbumFeatureCard` | `home/CarRecentAlbumSection.kt` | `(24,384) 640x248` |
| Recent-added compact cards | `CarRecentAlbumRowCard` | `home/CarRecentAlbumSection.kt` | widths `417/418`, height `112`, 24 design-pixel gutters |
| AlbumCard/Recommended/* | `CarAlbumCard` | `component/CarAlbumCard.kt` | `332x400`; five-column design composition at x `24+400n` |
| ArtistCard/* | `CarArtistCard` | `component/CarArtistCard.kt` | `332x336`; artwork `248` circle |
| ScrollSafeArea/Bottom | content end spacer | `home/CarHomeScreen.kt` | 48 design pixels after last visible section |

Use lazy containers and `bringIntoView`; do not reproduce the Figma content as one oversized absolute layer.

### Library mapping

| Figma node | Compose component | Proposed source path | Expanded design metric |
|---|---|---|---|
| Library/UtilityCard (`1375:*`) | `CarLibraryUtilityPane` | `library/CarLibraryUtilityPane.kt` | `(432,112) 560x920`; inner width `496`, padding `32` |
| Library/BasicComponentGroup (`14:31`, `89:874`) | `CarSongList` | `library/CarSongsScreen.kt` | `(1032,112) 1424x920` |
| SongRow/*/LibraryExpanded | `CarSongRow` | `component/CarSongRow.kt` | `1424x84`; step `85`; content starts x `152` |
| Playlist/ListPanel | `CarPlaylistListPane` | `playlist/CarPlaylistsScreen.kt` | `(432,112) 560x920`; header 76; scroll 844 |
| Playlist/TrackPanel | `CarPlaylistTrackPane` | `playlist/CarPlaylistsScreen.kt` | `(1032,112) 1424x920`; inner list `1328x780` |
| PlaylistHeader/Play | `CarPrimaryActionButton` | `playlist/CarPlaylistsScreen.kt` | `176x72` |
| Library/AlbumGrid (`970:1630`, `970:2631`) | `CarAlbumGrid` | `album/CarAlbumsScreen.kt` | `(1032,112) 1424x920`; 4 columns |
| AlbumCard/Library/* | `CarAlbumCard` | `component/CarAlbumCard.kt` | `332x400`; horizontal gap 16; vertical gap 24 |
| Library/ArtistGrid (`970:1674`, `970:2675`) | `CarArtistGrid` | `artist/CarArtistsScreen.kt` | `(1032,112) 1424x920`; 4 columns |
| ArtistCard/Library/* | `CarArtistCard` | `component/CarArtistCard.kt` | `332x360`; horizontal gap 16; vertical gap 24 |
| Icon/Search on Library headers | `CarSearchButton` | `component/CarLibraryHeaderActions.kt` | absolute design position `(2388,66)`, icon 36; runtime align to top/end |
| HeaderAction/Sort, Filter | deferred hidden actions | `component/CarLibraryHeaderActions.kt` | `88x56` each; nodes hidden in every read screen |

The exact 4-column composition is an Expanded metric. Runtime implementation should calculate columns from the resolved profile and available content constraints while preserving `332:16` card-to-gutter proportions at the reference size.

### Detail mapping

| Figma node | Compose component | Proposed source path | Expanded design metric |
|---|---|---|---|
| AlbumDetail/ScrollableContent (`977:1042`, `979:1110`) | `CarAlbumDetailScreen` | `album/CarAlbumDetailScreen.kt` | `(432,16) 2024x1016` |
| AlbumDetail/Hero | `CarAlbumHeroPane` | `album/CarAlbumHeroPane.kt` | local `(0,96) 560x920` |
| Detail/TrackListBackground | track surface | `album/CarAlbumDetailScreen.kt` | local `(600,96) 1424x920` |
| Detail/TrackList | `CarSongList` | `component/CarSongList.kt` | local `(648,166) 1328x769` |
| ArtistDetail/ScrollableContent (`977:1277`, `979:1345`) | `CarArtistDetailScreen` | `artist/CarArtistDetailScreen.kt` | `(432,16) 2024x1016` |
| ArtistDetail/Hero | `CarArtistHeroPane` | `artist/CarArtistHeroPane.kt` | local `(0,96) 560x920` |
| ArtistDetail/ContentBackground | content surface | `artist/CarArtistDetailScreen.kt` | local `(600,96) 1424x920` |
| Detail/TrackList + ArtistDetail/Albums | song list plus album row | `artist/CarArtistDetailScreen.kt` | x `648`, width `1328`; songs y `212`, albums y `676` |

### Now Playing and Queue mapping

| Figma node | Compose component | Proposed source path | Expanded design metric |
|---|---|---|---|
| NowPlaying/PlayerColumn (`3:260`, peers) | `CarNowPlayingControlsPane` | `nowplaying/CarNowPlayingScreen.kt` | `(56,48) 1000x984`; radius 32 |
| artwork area | `CarNowPlayingArtwork` | `nowplaying/CarNowPlayingArtwork.kt` | design context exposes a 640-wide art region within player column |
| CurrentTime / Duration | progress labels | `nowplaying/CarPlaybackProgress.kt` | y `826`; x `180` and `773`; 22 type |
| Progress track/thumb | `CarPlaybackProgress` | `nowplaying/CarPlaybackProgress.kt` | track width 640, height 14; thumb 38 |
| Favorite / Lossless / More | secondary actions | `nowplaying/CarNowPlayingActions.kt` | each `72`; y `712` |
| Previous / PlayPause / Next hit areas | playback actions | `nowplaying/CarPlaybackControls.kt` | `112`, `128`, `112`; y `862/854/862` |
| RepeatAll / Queue | edge playback actions | `nowplaying/CarPlaybackControls.kt` | visual icon 72; hit area for queue 112 |
| NowPlaying/ContentColumn (`3:287`, `89:595`) | `CarLyricsPane` | `nowplaying/CarLyricsPane.kt` | `(1112,48) 1328x984`; inner x 48; lyric width 1240 |
| QueuePanel (`3:328`, `89:671`) | `CarQueuePane` | `queue/CarQueuePane.kt` | same `(1112,48) 1328x984` slot |
| QueueList/ScrollViewport | lazy queue | `queue/CarQueuePane.kt` | local `(0,160) 1328x824`; inner x 48 |
| QueueRow/* | `CarQueueRow` | `queue/CarQueueRow.kt` | `1232x104`; vertical step 112 |

Queue is an alternate right-column state, not a second player screen. Opening it must preserve player controls and replace lyrics in the 1328-wide slot. The queue button uses a separate selected icon layer in Figma.

### Settings mapping

| Figma node | Compose component | Proposed source path | Expanded design metric |
|---|---|---|---|
| Settings/MainSettings (`15:19`, `89:983`) | `CarSettingsCategoryPane` | `settings/CarSettingsScreen.kt` | `(432,112) 560x920`; inner width 496 |
| Preference/MainItem (`65:81`) | `CarSettingsCategoryItem` | `component/CarPreference.kt` | runtime instance `496x88` |
| Settings/SecondarySettings (`15:42`, `89:990`) | `CarSettingsDetailPane` | `settings/CarSettingsScreen.kt` | `(1032,112) 1424x920`; inner width 1360 |
| Preference/ArrowPreference (`65:62`) | `CarArrowPreference` | `component/CarPreference.kt` | runtime row `1360x96` |
| Preference/SwitchPreference (`65:66`) | `CarSwitchPreference` | `component/CarPreference.kt` | runtime row `1360x96` |

Do not embed the mobile Settings root. Reuse repository/state only and build this two-pane car presentation.

### Search, dialogs, and state gaps

No dedicated 2496 Search page was found. The only Search nodes are `ActionTile/Search` on Home and `Icon/Search` on Songs/Albums/Artists. No query, typing, searching, result, no-result, or error composition is specified.

No Dialog board or dialog frame was found. No page-level Loading, Empty, or Error frame was found. The States boards cover component interaction states only. These missing designs must be resolved through a Figma update or an explicit Sol contract before Luna creates those visuals. Reusing an existing domain state model does not authorize inventing an Automotive layout.

## Affected Modules

- `:car:presentation`: all mapped Compose presentation and car-local design tokens.
- `:carApp`: hosts the presentation and supplies measured usable constraints/insets.
- Existing feature/service/core modules: state and actions are consumed through contracts; their existing mobile screens are not reused.

## Affected Files

This document proposes the source paths shown in the mapping tables. Phase 4 may adjust package roots to the actual module layout, but it must preserve component ownership and update this mapping if a path changes.

Documentation/evidence files in this phase:

- `docs/automotive/FIGMA_EXPANDED_MAPPING.md`
- `docs/automotive/DESIGN_SYSTEM_CONTRACT.md`
- `build/automotive/figma/EXPANDED_READ_SUMMARY.md`

## Files Not To Modify

- Existing mobile/desktop/iOS screen implementations as a shortcut for car UI.
- Database schema, repositories, source adapters, playback engine, queue engine, MediaSession, or player ownership.
- Hidden 1728 frames and 5120 fullscreen reference layouts.
- Figma nodes, styles, or components.

## Implementation Contract

1. Build shared shell and primitive components first, then one screen family at a time.
2. Resolve `CarLayoutProfile` from usable constraints. Pass a `CarLayoutMetrics` value down through a composition local or explicit parameter. Page composables must not read raw display pixels.
3. Store the reference metrics as unitless design proportions or named reference values. Convert them into runtime `Dp` only from measured Compose constraints and the selected profile.
4. Preserve the two major Expanded compositions: shell pages use a 352-wide rail plus 560/1424 content panes; Now Playing uses 1000/1328 panes with 56 outside margins and a 56 inter-pane gap.
5. Render lists/grids with lazy Compose containers, stable domain keys, focus restoration, and `bringIntoView`.
6. Export exact Figma icon/vector assets during implementation or reuse a repository icon only after glyph comparison. Temporary MCP asset URLs must not ship. Do not hand-author substitute paths.
7. Implement Dark and Light from semantic car tokens. Keep selection, focus, playing, pressed, and disabled independent.
8. Search, Dialogs, Loading, Empty, and Error remain explicit design gaps. Implementation of their page visuals requires an added contract.

## Acceptance Criteria

- Every implemented screen can be traced from Figma node to Compose component and current source path.
- At the measured Expanded reference viewport, pane positions, card/list density, touch targets, type hierarchy, and radii visually match the corresponding Dark and Light frames.
- Resizing does not scale text/artwork as one bitmap and does not clip the shell.
- No `2496.dp`, `1080.dp`, `1728.dp`, `screenWidth == 2496`, or `screenWidth == 1728` exists.
- Dark and Light use the same geometry unless the paired frames prove a difference.
- Selected, focused, pressed, playing, and disabled remain visually distinguishable in both modes.
- Search/dialog/page-state work is not reported complete until the missing design states are supplied and mapped.

## Risks

- Figma exposes no bound variables through `get_variable_defs`; named swatches and actual node styles can drift.
- The Color System contains rendered-fill versus printed-label conflicts for `surface/selected`, and light accent values differ across boards/screens.
- Component showcase boards use sample sizes that differ from runtime screen instances. Screen frames are the geometry source of truth.
- Roboto is reported by design context while the repository currently provides Noto Sans SC and Plus Jakarta Sans. Font substitution can change Chinese glyph width and line breaking.
- The authorized Phase 0 skip does not resolve density/inset uncertainty; final screenshots may require metric calibration without changing component structure.

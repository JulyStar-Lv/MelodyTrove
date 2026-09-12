# Current Automotive baseline

## Current branch and commit

- Branch: `codex/automotive-expanded`
- Audited baseline: `7a62353d00338cd5171289a5ba53d9e28760436a`
- Worktree at audit start: clean and synchronized with `origin/codex/automotive-expanded`

## Existing modules and screens

The independent `:carApp` application and isolated `:car:presentation` UI module already existed. Shared persistence, library sync, sources, search, settings, playback, queue, ExoPlayer, and Media3 session ownership live in `:core:runtime` and domain modules. Mobile remains in `:androidApp`/`:shared`.

Existing Automotive routes were Home, Songs, Albums, Artists, Playlists, Search, Settings, Album detail, Artist detail, Playlist detail, Now Playing, and Queue. Dark/light themes, semantic car components, a focus registry, D-pad, rotary, touch, and media-key routing were present.

## Existing resolution and runtime support

- `Expanded` existed and had complete metrics derived from the 2496×1080 Figma reference.
- `VehiclePanel` existed only as a profile name. Its resolver returned `metricsAvailable=false`, and the UI displayed “当前窗口布局尚未完成适配”. No 1728 UI was usable.
- There was no 5120×1304 full-cockpit runtime profile despite Figma fullscreen frames and FileManager's full-window contract.
- The Activity passed real Compose constraints to the resolver. The resolver did not yet observe a useful automatic width-shape rule; its default strategy always chose Expanded.

## Existing data and playback integration

Home and Library read real repositories. Search used the real search repository. Settings used the shared settings store. Library playback created the complete `PlayableItem` list with the selected `startIndex`. Mini Player, Now Playing, Queue, Media3 Player, and MediaSession observed one shared playback state. No car-specific player, queue database, or repository copy existed.

## Existing tests

The branch contained architecture ownership tests, layout resolver tests, ViewModel/domain tests, playback-request and queue-key tests, Compose tests for Mini Player, Song Row, Navigation focus, rotary routing, and Now Playing/Queue interaction, plus shared Android/Desktop regression coverage.

## Known placeholders and technical debt

- VehiclePanel metrics and UI were a deliberate placeholder.
- Figma 5120 fullscreen Now Playing and Cover Flow were not represented at runtime.
- Media grids used a hard-coded four-column count.
- Recommendation-only Home content correctly exposed unsupported states because no recommendation domain contract exists.
- Automatic profile selection and focus restoration had no profile-switch regression.

## Change classification

### KEEP

`:carApp`, `:car:presentation`, `:core:runtime`, shared repositories/controllers, playback/session ownership, car theme, semantic components, routes, state holders, and the existing focus coordinator.

### EXTEND

`CarLayoutProfile`, `CarLayoutMetrics`, `CarLayoutProfileResolver`, grid metrics, full-cockpit presentation, profile-aware focus restoration, resolver/Compose tests, and Automotive documentation.

### REFACTOR

The resolver's Expanded-only construction into profile reference contracts. Page composables continue sharing state and business actions.

### REMOVE

The VehiclePanel deferred boundary and its user-visible placeholder.

### MISSING

Complete 1728 metrics, 3-column media grids, 5120 fullscreen Now Playing/Cover Flow, all-resolution matrices, and profile-switch acceptance.

## Safe extension and review boundaries

Safe to extend: files under `car/presentation`, `carApp`, and `docs/automotive`. Files requiring focused review: `core/runtime` playback ownership, shared Room/DataStore composition, Android manifest/service declarations, and any Mobile presentation module. This change does not need to modify the latter boundaries.

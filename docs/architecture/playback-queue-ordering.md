# Playback queue ordering

The current playback queue is session state. Its order is held in the in-memory
`PlayerRepository` queue and is not written back to Room playlists or
`playlist_track` rows when a listener reorders songs.

Each queue entry stores an order key in `MusicMeta.order`. The ordering
algorithm lives exclusively in Rust's `order-key` crate and is exposed to
Kotlin through `app-backend` UniFFI functions. Kotlin selects the neighbouring
keys and writes the returned key to the moved entry; it does not implement key
comparison or key generation.

A normal move updates one `MusicMeta.order` value and replaces the real
playback queue once. The playback controller does not call `play()` for this
operation, so the active media item and its position are retained while its
queue index is recomputed from the updated queue.

The whole session queue is rebalanced only when its keys are missing,
non-canonical, duplicate, non-increasing, too long, or cannot provide a strict
insertion interval. Rebalancing creates evenly-spaced Rust order keys, retries
the requested move once, and leaves the real queue unchanged if the retry also
fails.

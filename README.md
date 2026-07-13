# TideTunes
TideTunes is a Kotlin Multiplatform / Compose Multiplatform music app for Android, iOS, and Desktop.

Features
----

- WebDAV and OneDrive support
- Fast, Standard, and Full WebDAV metadata scan modes
- Playlist-based, music cover and lyric support
- Unified Room KMP library on Android, iOS, and Desktop
- Source-agnostic canonical tracks with multi-source playback references

Architecture
----

TideTunes uses one shared Room database as the app-facing library source of
truth. Source accounts, library roots, source items, and track-source
references are stored separately from canonical tracks, albums, artists, genres,
lyrics, artwork metadata, playlists, and downloads.

Source adapters handle auth, browse, scan, and playback-resource resolution.
The import coordinator is the write boundary into Room, and transient playback
URLs, headers, tokens, and cookies are not persisted.

WebDAV metadata scanning defaults to Standard for new users: core tags and
embedded lyrics are read, while artwork and raw tags are skipped. Fast reads
only core tags; Full also reads artwork and raw tags. Existing installations
with a WebDAV account migrate once to Full to preserve their previous behavior.
Skipped optional metadata is left unchanged, and Settings can backfill missing
artwork or lyrics later without requiring the remote file fingerprint to change.

Roadmap
----

- v0.4: Plugin support
- v0.5: Ubuntu Linux support
- v0.6: UI/UX Improvements

Motivation
----

TideTunes is a Kotlin Multiplatform / Compose Multiplatform music app targeting
Android, iOS, and Desktop. Rust provides remote storage access, metadata parsing,
and desktop audio decoding.


License
----

- The majority of this project is licensed under the GPL-3.0 license.
- The [tidetunes-order-key](./rust-libs/order-key) is under either the Apache-2.0 or the MIT license.

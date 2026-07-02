# TideTunes
TideTunes is a Kotlin Multiplatform / Compose Multiplatform music app for Android, iOS, and Desktop.

Features
----

- WebDAV and OneDrive support
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

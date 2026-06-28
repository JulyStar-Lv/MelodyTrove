# WebDAV live test — 2026-06-25

Target: AList WebDAV over HTTPS at `alist.kbhy.online:5001`, using the
user-supplied account. Credentials were entered interactively and were not
written to the repository, command history, report, Room database, or logs.

## Results

| Check | Result |
| --- | --- |
| WebDAV authentication | Passed |
| Root `PROPFIND Depth: 1` | Passed; 896 entries returned |
| Unicode directory URL | Passed using the equivalent UTF-8 percent-encoded path |
| iOS directory browser | Passed; remote FLAC files displayed |
| iOS Keychain persistence | Passed; app restart restored access without re-entering credentials |
| Finite HTTP Range | Passed; requested and received the first 32 bytes of a 33,731,890-byte FLAC |
| Lofty remote metadata | Passed |
| First 50 file metadata scan | Passed; 50 full successes, 0 partial/failure/timeout; 20.183 s with concurrency 4 |
| Incremental fingerprint scan | Passed; 50 unchanged files skipped, 0 Range requests, 0 bytes, 0.824 s |
| Playlist import | Passed; one selected remote file appeared in the playlist |
| iOS Range playback | Passed; playback position advanced without a full-file cache |
| iOS seek | Passed; seeking from 5 seconds to 2:11 caused a non-contiguous remote Range |
| Test-data cleanup | Passed; temporary playlist, storage record, and Keychain credential removed |

Parsed metadata for the selected fixture:

- title: `倔强`
- artist: `五月天`
- album: `神的孩子都在跳舞`
- duration: 263,373 ms
- sample rate: 44,100 Hz
- bit depth: 16
- channels: 2

All first-50 entries supplied ETag, Last-Modified, and MIME properties. The
base metadata parser does not read embedded artwork; bounded artwork extraction
is a separate on-demand operation.

## Finding and fix

The first import exposed a Room mirror race: the playlist summary was written
before its track relations, so the list temporarily displayed zero tracks until
the detail screen refreshed the complete playlist.

This finding was later superseded by the Room-only playlist path:
`PlaylistRepository.createPlaylist` writes through `RoomLibraryStore`, and Room
Flow refreshes summaries from `playlist` and `playlist_track`. The same live
workflow was repeated after rebuilding; the list immediately displayed one
track.

## Range playback finding and fix

The previous iOS path called `ctGetAsset`, downloaded the complete remote file
to `Library/Caches/playback-*.flac`, and then opened that local file with
`AVPlayer`. The legacy `get(path, byte_offset)` API also requested an unbounded
`bytes=start-` range and could silently accept `200 OK`, so it did not provide
bounded random access.

Playback now uses a Rust loopback media gateway:

- binds only to `127.0.0.1` on an ephemeral port;
- protects each session with a random URL token;
- exposes `HEAD` and single-range `GET` with `Accept-Ranges`, `Content-Length`,
  `Content-Range`, `206`, and `416` behavior;
- converts AVPlayer requests into finite 256 KiB WebDAV ranges;
- keeps an in-memory 32-block LRU cache instead of writing the complete media
  file to disk;
- shuts down with the owning iOS playback session.

The first simulator run exposed a startup panic in
`hyper::Server::from_tcp`: UniFFI was polling the async controller on a Kotlin
coroutine thread with no active Tokio reactor. Listener and Hyper server
creation were moved into TideTunes's Rust Tokio runtime.

AList returned a generic media MIME type. The gateway now derives
`audio/flac` from the source extension and preserves a `.flac` suffix on the
loopback URL so AVFoundation can identify the container.

Live evidence from the 33,731,890-byte fixture:

- AVPlayer probe: `bytes=0-1`;
- initial stream: `bytes=0-33731889`, served incrementally in 256 KiB blocks;
- seek performed while only about 2.6 MiB of the initial stream had been read;
- seek requests: `bytes=12910592-12976127`, then
  `bytes=12976128-33731889`;
- corresponding remote block started at byte `12845056`;
- stopped-session statistics: 39 remote requests and 9,961,473 bytes fetched;
- no `Library/Caches/playback-*` file existed after playback and seek.

Tracks without persisted duration metadata now use the platform player's
reported duration, so the progress control remains seekable after AVPlayer has
parsed the stream.

## Reusable probe

`rust-libs/tidetunes-metadata/examples/webdav_probe.rs` performs the live
directory, finite-range, and Lofty checks. It reads these environment variables:

```text
TIDETUNES_WEBDAV_ADDRESS
TIDETUNES_WEBDAV_USERNAME
TIDETUNES_WEBDAV_PASSWORD
```

No defaults or credentials are embedded in source.

# Lyrico Plugin API v3 completion status

The production JavaScript metadata path is implemented and covered by focused Rust, Kotlin, ZIP
integration, and production-assembly tests.

Completed scope:

- QuickJS string/object/array/null/undefined normalization without JSON double encoding.
- Timed and cancellable script loading and calls, poisoned-runtime rejection, and lazy rebuild.
- Lyrico v3 song, lyric, and cover request/result contracts.
- Plugin permissions, bounded private context isolation, cache cleanup, and lifecycle invalidation.
- Dynamic Room-backed `MetaSourceRegistry` production assembly and resilient lookup use case.
- Settings navigation and minimum cross-platform plugin management UI.
- Official v3 manifest config types, dropdown options, dependency visibility, optional descriptive
  fields, and empty-capability `searchSongs` defaulting, with legacy boolean/select aliases.
- Strict Lyrico v3 ZIP tests covering `JSON.stringify(...)` execution and official manifest
  configuration end to end.
- Lyrico Host API modules, including XML mutation, HTTP redirect revalidation, cache, compression,
  base64/bytes, and crypto helpers.
- Registry shutdown wired to Desktop, Android's emulated/test-process termination callback, and
  iOS application termination, including private context clearing when a poisoned runtime is
  rebuilt. Normal Android process death relies on OS resource reclamation.

Current validation status and platform limitations are recorded in `docs/testing/test-report.md`.

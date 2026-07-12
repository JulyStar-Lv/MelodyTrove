# TideTunes Plugin Runtime

This document records the scope and validation path for the Lyrico Plugin API v3 compatible runtime.

## Scope

- User plugins are imported manually from local ZIP files. TideTunes does not ship, download, recommend, or auto-enable real third-party plugins.
- The Rust runtime owns QuickJS, Host API execution, HTTP/XML/cache/crypto helpers, timeout/cancel handling, and per-plugin cache isolation.
- Kotlin owns ZIP import metadata, script bundling, runtime lifecycle, and MetaSource parsing. Business objects are not exposed to Rust.
- Each enabled plugin gets a lazy, isolated runtime. Calls are serialized for the same plugin; different plugin managers can create independent runtimes.

## Import Behavior

- Single-plugin ZIPs and aggregate ZIPs are extracted through the Rust `extract_plugin_zip` UniFFI function.
- Extraction rejects path traversal, symlinks, excessive file count, excessive depth, and excessive total uncompressed size.
- The installer recursively discovers `manifest.json` files, validates API version, entry file, include directories, icon path, duplicate plugin IDs, and downgrade conflicts.
- Installed plugins are stored disabled by default. Manual lookup remains allowed by the runtime model; automatic and batch lookup default to disabled.

## Runtime Behavior

- Scripts are loaded in this order: host bootstrap, include bootstrap, sorted include-directory `.js` files, then entry script.
- `include(path)` is a compatibility no-op. The runtime does not read plugin files after bundling.
- Host API calls go through `__lyricoHostCall(name, JSON.stringify(payload))` and return `{ "value": ... }`.
- Default limits are 4 MiB QuickJS heap, 2 MiB QuickJS stack, 5 seconds per JS call, and 16 MiB per HTTP response.
- Timeout, OOM, poisoned, and internal runtime failures invalidate the Kotlin runtime cache. The Rust worker exits so QuickJS is destroyed before the next call rebuilds the runtime.

## Validation Commands

Run these before declaring this feature complete:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
cargo test -p tidetunes-plugin-runtime --manifest-path rust-libs/Cargo.toml
.\gradlew.bat :shared:compileKotlinDesktop :shared:desktopTest --tests "com.github.tidetunes.plugin.*" --no-daemon --console=plain
.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --console=plain
pnpm run audit:release
```

## Current Limitations

- The repository currently contains runtime and repository plumbing, but no TideTunes Compose plugin management screen or navigation entry.
- MetaSource adapter classes exist, but they still need to be wired into TideTunes' active metadata lookup flows.
- Real third-party plugin ZIPs must only be tested locally through user-provided paths and must not be committed.

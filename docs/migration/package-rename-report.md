# TideTune package and identity migration

Date: 2026-06-24  
Branch: `codex/tidetune-migration`

## Product identity

| Item | TideTune value |
| --- | --- |
| Product and project name | `TideTune` |
| Android application ID | `com.github.tidetune` |
| Android namespace | `com.github.tidetune` |
| Shared Android namespace | `com.github.tidetune.shared` |
| Kotlin root package | `com.github.tidetune` |
| Desktop main class | `com.github.tidetune.MainKt` |
| Desktop package name | `TideTune` |
| OAuth custom scheme | `tidetune://` |
| Desktop data directory | `~/.tidetune` |
| Transitional redb filename | `tidetune-legacy.redb` |
| Rust core crate/library | `tidetune-core` / `tidetune_core` |

The final Room database will be named `tidetune.db`. The existing redb database
uses `tidetune-legacy.redb` to keep the migration source distinct from the Room
destination.

## Kotlin source migration

All active Kotlin source roots were moved from
`com/kutedev/easemusicplayer` to `com/github/tidetune`:

- `androidApp/src/main/java`
- `desktopApp/src/desktopMain/kotlin`
- `shared/src/commonMain/kotlin`
- `shared/src/androidMain/kotlin`
- `shared/src/desktopMain/kotlin`

Package declarations, imports, manifest components, application class, theme,
Compose generated-resource imports, services, and Desktop entry point were
updated together.

The old independent `android/` build was removed. It duplicated the application
that is now maintained by the root `androidApp` and `shared` modules and was not
included by the root Gradle build.

## Rust crate migration

| Previous crate role | TideTune crate |
| --- | --- |
| backend/core plus current UniFFI exports | `tidetune-core` |
| schema and persisted redb models | `tidetune-schema` |
| async runtime | `tidetune-runtime` |
| ordering keys | `tidetune-order-key` |
| remote storage | `tidetune-remote-storage` |
| UniFFI binding CLI | `tidetune-ffi-builder` |

Cargo package names, paths, Rust imports, generated FFI symbols, native library
lookup names, and Kotlin UniFFI packages were migrated. Kotlin bindings were
regenerated from `libtidetune_core.dylib`, not only text-replaced, so UniFFI
checksums match the renamed Rust exports.

`tidetune-core` still contains the current UniFFI exports as an intermediate
buildable state. A dedicated `tidetune-ffi` wrapper crate will be introduced
during the Gobley/UniFFI boundary phase.

## License handling

The upstream repository tracks both a root `LICENSE` symlink and a lowercase
`license/` directory. They conflict on the current case-insensitive macOS
filesystem. The symlink is replaced by `LICENSE.md`, copied verbatim from
`license/LICENSE-GPL`; the original license directory and component licenses
remain unchanged.

## Compatibility identifiers intentionally retained

`rust-libs/tidetune-schema/src/v2/repositories.rs` retains five historical
`ease_client_shared::...` redb type-name strings. They are serialized database
type identifiers required to read and migrate existing v2 data. Changing them
would break compatibility. They are not package names, runtime namespaces, app
branding, or public APIs.

The architecture baseline document also names the old project identifiers to
describe the reviewed upstream state.

## Verification

Commands executed after the migration:

```text
./gradlew :androidApp:assembleDebug --no-daemon --no-configuration-cache --console plain
./gradlew :desktopApp:compileKotlinDesktop --no-daemon --no-configuration-cache --console plain
$HOME/.cargo/bin/cargo test --workspace
```

Results:

- Android debug APK: passed.
- Desktop Kotlin compilation: passed.
- Rust: 29 tests passed, including WebDAV and redb v2-to-v3 migration tests.
- old-identifier scan: no active old package/product/crate identifiers remain
  outside the documented redb compatibility strings and architecture history.


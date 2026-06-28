# TideTunes package and identity migration

Date: 2026-06-24  
Branch: `codex/tidetunes-migration`

## Product identity

| Item | TideTunes value |
| --- | --- |
| Product and project name | `TideTunes` |
| Android application ID | `com.github.tidetunes` |
| Android namespace | `com.github.tidetunes` |
| Shared Android namespace | `com.github.tidetunes.shared` |
| Kotlin root package | `com.github.tidetunes` |
| Desktop main class | `com.github.tidetunes.MainKt` |
| Desktop package name | `TideTunes` |
| OAuth custom scheme | `tidetunes://` |
| Desktop data directory | `~/.tidetunes` |
| Transitional redb filename | `tidetunes-legacy.redb` |
| Rust core crate/library | `tidetunes-core` / `tidetunes_core` |

The final Room database will be named `tidetunes.db`. The existing redb database
uses `tidetunes-legacy.redb` to keep the migration source distinct from the Room
destination.

## Kotlin source migration

All active Kotlin source roots were moved from
`com/kutedev/easemusicplayer` to `com/github/tidetunes`:

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

| Previous crate role | TideTunes crate |
| --- | --- |
| backend/core plus current UniFFI exports | `tidetunes-core` |
| schema and persisted redb models | `tidetunes-schema` |
| async runtime | `tidetunes-runtime` |
| ordering keys | `tidetunes-order-key` |
| remote storage | `tidetunes-remote-storage` |
| UniFFI binding CLI | `tidetunes-ffi-builder` |

Cargo package names, paths, Rust imports, generated FFI symbols, native library
lookup names, and Kotlin UniFFI packages were migrated. Kotlin bindings were
regenerated from `libtidetunes_core.dylib`, not only text-replaced, so UniFFI
checksums match the renamed Rust exports.

`tidetunes-core` still contains the current UniFFI exports as an intermediate
buildable state. A dedicated `tidetunes-ffi` wrapper crate will be introduced
during the Gobley/UniFFI boundary phase.

## License handling

The upstream repository tracks both a root `LICENSE` symlink and a lowercase
`license/` directory. They conflict on the current case-insensitive macOS
filesystem. The symlink is replaced by `LICENSE.md`, copied verbatim from
`license/LICENSE-GPL`; the original license directory and component licenses
remain unchanged.

## Compatibility identifiers intentionally retained

`rust-libs/tidetunes-schema/src/v2/repositories.rs` retains five historical
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


# TideTunes build logic

This included build is the first measured step toward convention plugins.

Current status:

- `com.github.tidetunes.convention.project` is intentionally minimal and only
  sets shared project metadata.
- Existing module build scripts are not migrated yet.
- Future plugin slices should be introduced one at a time and verified with the
  full Android/iOS/Desktop gate before being applied broadly.

Planned slices:

1. `convention.kmp.base` - Kotlin target defaults and compiler options.
2. `convention.cmp.shared` - Compose Multiplatform shared-module defaults.
3. `convention.android.application` - Android application defaults.
4. `convention.room` - Room/KSP schema configuration.
5. `convention.rust-interop` - Gobley/UniFFI/Cargo wiring.

Do not move existing `shared`, `androidApp`, or `desktopApp` build logic into a
plugin until the equivalent plugin has a narrow verification command and keeps
the full gate green.

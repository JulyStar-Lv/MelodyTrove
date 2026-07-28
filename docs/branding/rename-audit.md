# MelodyTrove rename audit

Date: 2026-07-28

## Scope

This audit covers tracked source paths, Kotlin/Java packages, Gradle build logic,
Compose resources, Android, iOS/Xcode, Desktop storage, Room, Rust crates,
UniFFI bindings, scripts, workflows, the design prototype, and documentation.

## Canonical result

| Area | Result |
| --- | --- |
| Product name | `MelodyTrove` / `旋律珍藏` |
| Repository slug | `MelodyTrove` |
| Kotlin/Java package | `io.github.julystar.musicapp` |
| Android | `AppApplication`, `Theme.App`, new application ID and both transition URL schemes |
| iOS | `App.xcodeproj`, `App` target/scheme, `AppMain`, `SharedKit`, `MelodyTrove.app` |
| Desktop | standard platform data root under `MelodyTrove` with idempotent legacy migration |
| Persistence | `AppDatabase`, Room schema version 19, `library.db`, `settings.preferences_pb` |
| Rust/UniFFI | `app-backend`, `app_backend`, `uniffi.app_backend` |
| Internal UI/build names | brand-neutral names such as `AppTheme`, `Design*`, and `*ConventionPlugin` |

The Room version remains 19 because the rename does not change the relational
schema. Existing schema JSON files were moved to the new generated database
identity without modifying their identity hashes.

## Compatibility

Legacy paths, filenames, service IDs, preference keys, URL schemes, and
developer environment variables are cataloged in
[`legacy-identifiers.md`](legacy-identifiers.md). Desktop migration uses
restart-safe markers, atomic moves where supported, verified copy fallback, and
SQLite header validation. Mobile filename and credential fallbacks are
implemented, subject to the OS sandbox limitation documented in
[`external-migration-checklist.md`](external-migration-checklist.md).

## Automated checks

The merge gate is:

```bash
node scripts/audit-release.js
node scripts/audit-branding.js
git diff --check
./gradlew --no-daemon --stacktrace \
  :androidApp:assembleDebug \
  :desktopApp:compileKotlinDesktop \
  :shared:desktopTest \
  :shared:compileKotlinIosSimulatorArm64
cargo fmt --manifest-path rust-libs/Cargo.toml --all -- --check
cargo clippy --manifest-path rust-libs/Cargo.toml \
  --workspace --all-targets -- -D warnings
cargo test --manifest-path rust-libs/Cargo.toml --workspace
xcodebuild -project iosApp/App.xcodeproj -scheme App \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build
```

macOS package versions must start with a positive integer. The application
version line now begins at `1.0.0`; development packages use the compatible
numeric form `1.0.<build-number>` while the in-app version retains its
development suffix and commit SHA.

The final pull request records the exact command outcomes and any external
operator actions that remain.

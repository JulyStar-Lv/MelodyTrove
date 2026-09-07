# Android pre-implementation build baseline

Date: 2026-09-07. Source revision: `45bba3c510ac4ca3425891ab750ae5ad838b85ef`.
No application/module changes had been made when this command ran.

## Command and environment

```powershell
$env:JAVA_HOME = 'C:/Software/Android Studio/jbr'
.\gradlew.bat :androidApp:assembleDebug --console=plain
```

- JDK: JetBrains OpenJDK 21.0.10.
- Gradle: repository wrapper 8.14.5.
- Android SDK: `C:/Users/Shine/AppData/Local/Android/Sdk`.
- Local full log: `build/automotive-baseline-build.log` (ignored build output).

## Result

**Exit code 0; BUILD SUCCESSFUL in 18m 37s.**

1044 actionable tasks: 592 executed, 452 up-to-date.

Both APK files exist:

- `androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk`
- `androidApp/build/outputs/apk/debug/androidApp-x86_64-debug.apk`

## Scope and limitations

This proves the unmodified Android application builds in this environment. It
does not prove automotive UI, window profiles, playback end-to-end behavior,
tests, device compatibility, or regression safety after future code changes.

No `carApp` module exists yet; `:carApp:assembleDebug` has not run. Phase 0's
window evidence gate prevents proceeding to application/page implementation.

The build reports existing deprecated Compose dependency accessors. It also
packages several native libraries without stripping debug symbols. Neither
warning caused this baseline build to fail.

Existing iOS `audioProcessingTap` Cinterop cannot be compiled on this Windows
host. Android build success is not iOS validation. Existing macOS CI remains the
appropriate place for those checks once implementation changes are ready.

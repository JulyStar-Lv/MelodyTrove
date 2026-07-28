# Theme color design and implementation audit

Date: 2026-07-28

## Decision

MelodyTrove now uses one cross-platform theme-seed model:

- the current artwork supplies the active seed when artwork color is enabled and
  extraction succeeds;
- the persisted Manual Theme Seed is the fallback for missing or failed artwork;
- loading keeps the previous valid artwork seed when one exists;
- disabling artwork color always uses the Manual Theme Seed.

The user-facing system-wallpaper/Android dynamic-color entry has been removed.
`Monet` remains only as an internal Miuix library enum name.

## Design-to-Compose mapping

| Design contract | Repository source | Result |
| --- | --- | --- |
| Appearance IA: Theme → Color → Language | `AppearanceSettingsSection.kt` | Matched |
| Artwork color on/off and artwork status copy | Compose Resources plus `AppearanceSettingsSection.kt` | Matched |
| Artwork/manual/fallback seed matrix | `ThemeSeed.kt` and `Root.kt` | Matched |
| Default Manual Theme Seed `#FF5B8A` | `SettingsModels.kt` and `Color.kt` | Matched |
| Six presets and a 12-color saved palette | `ThemeColorPickerDialog.kt` and DataStore settings | Matched |
| Local HSV/Hex preview; Apply persists | `ThemeColorPickerDialog.kt` | Matched |
| Invalid Hex has icon/message and disables Apply | `ThemeColorPickerDialog.kt` | Matched and exercised |
| Light and dark generated previews | `ThemeSeedPreviewTheme` | Matched |
| Semantic error colors are stable | `Color.kt` plus preview note | Matched |
| Swatch 48 dp; minimum target 48 dp | `DesignColorPicker` and swatch semantics | Matched |
| Dialog max 760 dp; content max 720 dp | `DesignDialog.kt` and `DesignColorPicker` | Matched |
| HSV area 180 dp; indicator 20 dp; Hue visual 32 dp | `DesignColorPicker` | Matched |
| Grid gap 12 dp; section gap 20 dp | `DesignColorPicker` | Matched |
| Theme transition 400 ms | `DesignMotion.themeMillis` | Matched |
| Compact one-column/vertical actions; wider horizontal actions | `ThemeColorPickerDialog.kt` | Matched |
| Pointer, touch, keyboard, and accessibility descriptions | picker pointer/key/semantics modifiers | Matched in source |

## Runtime evidence

The Desktop application was launched with a separate temporary bundle identifier
and temporary user home. It did not read or mutate the installed application's
settings.

- Appearance, dark, artwork on and no-artwork fallback:
  `Design/exports/theme-color/actual-desktop-expanded-appearance-dark.png`
- Picker, dark, Brand Pink:
  `Design/exports/theme-color/actual-desktop-expanded-picker-dark.png`
- Invalid `GG0000`, visible warning, disabled Apply:
  `Design/exports/theme-color/actual-desktop-picker-invalid-hex.png`
- Yellow seed, dark:
  `Design/exports/theme-color/actual-desktop-yellow-dark.png`
- Yellow seed, light:
  `Design/exports/theme-color/actual-desktop-yellow-light.png`
- Artwork color off, persisted Yellow summary:
  `Design/exports/theme-color/actual-desktop-expanded-appearance-artwork-off.png`

Observed behavior:

- choosing Yellow updated the picker preview without saving;
- Apply persisted `#FFD93D` and recolored the complete Miuix theme;
- light/dark mode kept readable text and controls for the high-luminance Yellow
  seed;
- invalid Hex showed a non-color-only warning and disabled Apply;
- Cancel discarded the invalid draft and retained `#FFD93D`;
- the no-artwork and artwork-off summaries matched the design state matrix.

## Platform verification

| Platform | Build/runtime status | Screenshot status |
| --- | --- | --- |
| Desktop Expanded | Built, launched, and exercised at `1018 × 683` | Available above |
| Android Compact | `:androidApp:assembleDebug` passed; physical Android 13 USB installation was rejected by the device with `INSTALL_FAILED_USER_RESTRICTED` | Not captured; no screenshot fabricated |
| iOS Compact | No simulator was booted; the current Xcode 26.4 toolchain fails the project cinterop before an app can be installed | Not captured; no screenshot fabricated |

### Android manual verification

1. Enable “Install via USB” on the connected test device.
2. Run `./gradlew :androidApp:assembleDebug`.
3. Run
   `adb install -r androidApp/build/outputs/apk/debug/androidApp-arm64-v8a-debug.apk`.
4. Open MelodyTrove → Settings → Appearance.
5. Capture artwork color on, artwork color off, the picker, Brand Pink
   light/dark, Yellow light/dark, and the no-artwork fallback.
6. Remove the QA package with
   `adb uninstall io.github.julystar.musicapp`.

### iOS manual verification

1. Resolve the Xcode 26.4 Foundation/cinterop toolchain failure, then run
   `./gradlew :shared:compileKotlinIosSimulatorArm64`.
2. Open `iosApp/App.xcodeproj`, select an iPhone simulator, and run the App
   scheme.
3. Repeat the Android state list at Compact width and capture the picker plus an
   applied custom color.

## Design differences and unsynced items

- No design-to-Compose copy, token, state, fallback, or interaction mismatch was
  found in the audited theme-color scope.
- The external Figma Make document was not modified because this environment did
  not expose an editable document. The exact manual handoff is
  `Design/docs/figma-theme-color-sync-checklist.md`.
- Android and iOS runtime screenshots are unverified platform evidence, not
  fabricated or claimed as completed design synchronization.


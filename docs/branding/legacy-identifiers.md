# Legacy identifier inventory

Date: 2026-07-28

The former product name was `TideTunes`. The following identifiers are retained
only to read existing data or accept existing integrations. New data must never
be written with them.

| Legacy identifier | Current identifier | Retention reason |
| --- | --- | --- |
| `TideTunes` | `MelodyTrove` | Historical name in migration messaging and old backup discovery |
| `com.github.tidetunes` | `io.github.julystar.musicapp` | Android credential/key identifiers and upgrade inventory |
| `tidetunes` URL scheme | `melodytrove` | Existing OAuth registrations and pending redirects |
| `~/.tidetunes` | platform data root plus `MelodyTrove` | Desktop in-place data migration |
| `tidetunes.db` | `library.db` | Existing Room database, WAL, and SHM migration |
| `tidetunes.preferences_pb` | `settings.preferences_pb` | Existing preferences migration |
| `tidetunes_secure_credentials` | `io.github.julystar.musicapp.credentials.preferences` | Android encrypted credential migration |
| `TideTunesCredentialKey` | `io.github.julystar.musicapp.credentials.key` | Android Keystore migration where the old sandbox is accessible |
| `TIDETUNES_*` environment variables | `MUSICAPP_*` | Developer/test compatibility during the transition |

The source of truth is:

- `migration/LegacyPaths.kt`
- `migration/LegacyIds.kt`
- `migration/LegacyCredentialIds.kt`
- `migration/LegacyPreferenceKeys.kt`
- `migration/LegacyDeepLinks.kt`
- `migration/LegacyEnvironmentVariables.kt`

Android Manifest and iOS `Info.plist` keep the old URL scheme alongside the new
one. README mentions the former name once in each language so existing users
can identify the project. These are deliberate audit allow-list entries.

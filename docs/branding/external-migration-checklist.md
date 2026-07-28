# External migration checklist

Date: 2026-07-28

Repository code cannot complete the following operator-owned changes. Complete
them before publishing a release under the new application identifiers.

- Completed 2026-07-28: renamed the GitHub repository to `MelodyTrove` and
  retained the former repository URL redirect.
- Verify GitHub branch protection, Actions secrets, repository topics, and the
  optional repository description before the release.
- Register `melodytrove://oauth2redirect/` with the OneDrive/Azure application.
  Keep the legacy redirect registered for at least one transition release.
- Create or update Android signing and store listings for application ID
  `io.github.julystar.musicapp`; update Firebase, app links, OAuth clients, and
  any MDM policies that key off the old package.
- Create or update the Apple App ID, provisioning profiles, capabilities,
  Keychain groups, associated domains, and App Store Connect record for bundle
  ID `io.github.julystar.musicapp`.
- Update release automation, package registries, download links, badges,
  website metadata, social links, and third-party plugin host allow lists.
- Verify macOS notarization identities, Windows signing metadata, Linux package
  metadata, and update feeds use the `MelodyTrove` product name.

## Mobile data-transfer limitation

Changing an Android application ID or iOS bundle ID normally creates a different
OS sandbox. Code in the new application cannot automatically open the former
application's private database, preferences, Android Keystore entries, or
default iOS Keychain group.

The in-app migration code handles legacy filenames and credentials when both
old and new data are available in the same sandbox, such as an internal build,
restored container, managed-device transfer, or pre-release package. For a
public package-ID transition, provide an export/import release of the former
app or a signed platform-supported transfer path before shipping the new ID.
Never copy secrets through logs or an unencrypted backup.

## Release verification

- Install over an accessible legacy data set and confirm library, playlists,
  settings, plugins, downloads, and credentials remain usable.
- Launch a clean install and confirm it creates only the new database,
  preferences, service IDs, and data paths.
- Exercise both URL schemes, then confirm all newly emitted redirects use
  `melodytrove`.
- Inspect a settings backup and diagnostics ZIP for `application`,
  `packageId`, and format/schema version fields.

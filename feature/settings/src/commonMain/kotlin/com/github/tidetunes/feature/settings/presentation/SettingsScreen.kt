package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AudioFocusMode
import org.jetbrains.compose.resources.stringResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_image
import tidetunes.core.presentation.generated.resources.icon_lyrics
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.core.presentation.generated.resources.icon_setting
import tidetunes.core.presentation.generated.resources.icon_wifitethering
import tidetunes.feature.settings.generated.resources.*

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToNetworkCache: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    SettingsPageLayout(title = stringResource(Res.string.settings_title)) {
        SettingsSection(title = "Personalization") {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_appearance_title),
                summary = stringResource(Res.string.settings_appearance_summary),
                icon = CoreRes.drawable.icon_image,
                onClick = onNavigateToAppearance,
            )
        }

        SettingsSection(title = "Playback") {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_playback_title),
                summary = when (state.settings.audioFocusMode) {
                    AudioFocusMode.Pause -> stringResource(Res.string.settings_playback_summary_pause)
                    AudioFocusMode.Duck -> stringResource(Res.string.settings_playback_summary_duck)
                    AudioFocusMode.Mix -> stringResource(Res.string.settings_playback_summary_mix)
                },
                icon = CoreRes.drawable.icon_play,
                onClick = onNavigateToPlayback,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_lyrics_title),
                summary = stringResource(Res.string.settings_lyrics_summary),
                icon = CoreRes.drawable.icon_lyrics,
                onClick = onNavigateToLyrics,
            )
        }

        SettingsSection(title = "Library & Data") {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_sources_title),
                summary = if (state.sourceAccounts.isEmpty()) {
                    stringResource(Res.string.settings_sources_summary_empty)
                } else {
                    stringResource(
                        Res.string.settings_sources_summary,
                        state.enabledSourceCount,
                        state.trackCount,
                    )
                },
                icon = CoreRes.drawable.icon_cloud,
                onClick = onNavigateToSource,
            )
            SettingsEntryCard(
                title = "Metadata plugins",
                summary = "Import and configure Lyrico Plugin API v3 providers",
                icon = CoreRes.drawable.icon_setting,
                onClick = onNavigateToPlugins,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_network_cache_title),
                summary = stringResource(
                    Res.string.settings_network_cache_summary,
                    formatBytes(state.settings.audioCacheLimitBytes),
                ),
                icon = CoreRes.drawable.icon_wifitethering,
                onClick = onNavigateToNetworkCache,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_storage_title),
                summary = stringResource(
                    Res.string.settings_storage_summary,
                    formatBytes(state.storageUsage.totalBytes),
                ),
                icon = CoreRes.drawable.icon_album,
                onClick = onNavigateToStorage,
            )
        }

        SettingsSection(title = "App Info") {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_about_title),
                summary = stringResource(
                    Res.string.settings_about_summary,
                    appVersion.ifBlank { stringResource(Res.string.settings_unavailable) },
                ),
                icon = CoreRes.drawable.icon_music_note,
                onClick = onNavigateToAbout,
            )
        }
    }
}

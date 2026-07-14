package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AudioFocusMode
import org.jetbrains.compose.resources.stringResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_image
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
    onNavigateToSource: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToNetworkCache: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    SettingsPageLayout(title = stringResource(Res.string.settings_title)) {
        SettingsSection(title = stringResource(Res.string.settings_home_section)) {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_appearance_title),
                summary = stringResource(Res.string.settings_appearance_summary),
                icon = CoreRes.drawable.icon_image,
                onClick = onNavigateToAppearance,
            )
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
                title = stringResource(Res.string.settings_plugins_title),
                summary = stringResource(Res.string.settings_plugins_summary),
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

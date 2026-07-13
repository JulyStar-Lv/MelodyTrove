package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToNetworkCache: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    SettingsPageLayout(title = stringResource(Res.string.settings_title)) {
        SettingsSection(title = stringResource(Res.string.settings_home_section)) {
            SettingsEntryCard(
                title = stringResource(Res.string.settings_appearance_title),
                summary = stringResource(Res.string.settings_appearance_summary),
                marker = "◐",
                accentColor = TideTunesBrand.Primary,
                onClick = onNavigateToAppearance,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_playback_title),
                summary = when (state.settings.audioFocusMode) {
                    AudioFocusMode.Pause -> stringResource(Res.string.settings_playback_summary_pause)
                    AudioFocusMode.Duck -> stringResource(Res.string.settings_playback_summary_duck)
                    AudioFocusMode.Mix -> stringResource(Res.string.settings_playback_summary_mix)
                },
                marker = "▷",
                accentColor = TideTunesBrand.SupportBlue,
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
                marker = "◎",
                accentColor = TideTunesBrand.SupportGreen,
                onClick = onNavigateToSource,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_network_cache_title),
                summary = stringResource(
                    Res.string.settings_network_cache_summary,
                    formatBytes(state.settings.audioCacheLimitBytes),
                ),
                marker = "≋",
                accentColor = TideTunesBrand.Secondary,
                onClick = onNavigateToNetworkCache,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_storage_title),
                summary = stringResource(
                    Res.string.settings_storage_summary,
                    formatBytes(state.storageUsage.totalBytes),
                ),
                marker = "▦",
                accentColor = TideTunesBrand.SupportOrange,
                onClick = onNavigateToStorage,
            )
            SettingsEntryCard(
                title = stringResource(Res.string.settings_about_title),
                summary = stringResource(
                    Res.string.settings_about_summary,
                    appVersion.ifBlank { stringResource(Res.string.settings_unavailable) },
                ),
                marker = "♪",
                accentColor = TideTunesBrand.Primary,
                onClick = onNavigateToAbout,
            )
        }
    }
}

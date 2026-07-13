package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AudioFocusMode
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

@Composable
fun PlaybackSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val capabilities = state.capabilities

    SettingsPageLayout(title = stringResource(Res.string.settings_playback_title), onBack = onBack) {
        if (capabilities.audioFocusSupported) {
            SettingsSection(title = stringResource(Res.string.settings_audio_focus_section)) {
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_pause),
                    summary = stringResource(Res.string.settings_audio_focus_pause_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Pause,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Pause)) },
                )
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_duck),
                    summary = stringResource(Res.string.settings_audio_focus_duck_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Duck,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Duck)) },
                )
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_mix),
                    summary = stringResource(Res.string.settings_audio_focus_mix_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Mix,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Mix)) },
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_playback_behavior_section)) {
            if (capabilities.deviceDisconnectSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_pause_disconnect),
                    summary = stringResource(Res.string.settings_pause_disconnect_summary),
                    checked = settings.pauseOnDisconnect,
                    onCheckedChange = { onAction(SettingsAction.SetPauseOnDisconnect(it)) },
                )
            }
            if (capabilities.gaplessPlaybackSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_gapless),
                    summary = stringResource(Res.string.settings_gapless_summary),
                    checked = settings.gaplessPlaybackEnabled,
                    onCheckedChange = { onAction(SettingsAction.SetGaplessPlaybackEnabled(it)) },
                )
            }
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_retry_playback),
                summary = stringResource(Res.string.settings_retry_playback_summary),
                checked = settings.retryPlaybackOnFailure,
                onCheckedChange = { onAction(SettingsAction.SetRetryPlaybackOnFailure(it)) },
            )
            if (capabilities.networkStatusSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_resume_network),
                    summary = stringResource(Res.string.settings_resume_network_summary),
                    checked = settings.resumePlaybackAfterNetworkRecovery,
                    onCheckedChange = {
                        onAction(SettingsAction.SetResumePlaybackAfterNetworkRecovery(it))
                    },
                )
            }
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_keep_screen_on),
                summary = stringResource(Res.string.settings_keep_screen_on_summary),
                checked = settings.keepScreenOnInPlayer,
                onCheckedChange = { onAction(SettingsAction.SetKeepScreenOnInPlayer(it)) },
            )
        }
    }
}

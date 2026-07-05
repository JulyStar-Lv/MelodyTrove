package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable

@Composable
fun PlaybackSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings

    SettingsPageLayout(title = "播放", onBack = onBack) {
        SettingsSection(title = "播放") {
            SettingsSwitchRow(
                title = "断连暂停",
                summary = "音频设备断开时暂停播放，不会自动恢复播放",
                checked = settings.pauseOnDisconnect,
                onCheckedChange = { onAction(SettingsAction.SetPauseOnDisconnect(it)) },
            )
            SettingsSwitchRow(
                title = "混合播放",
                summary = "允许与其他 App 同时播放声音",
                checked = settings.allowMixedPlayback,
                onCheckedChange = { onAction(SettingsAction.SetAllowMixedPlayback(it)) },
            )
            SettingsSwitchRow(
                title = "常亮",
                summary = "仅播放详情页保持屏幕常亮",
                checked = settings.keepScreenOnInPlayer,
                onCheckedChange = { onAction(SettingsAction.SetKeepScreenOnInPlayer(it)) },
            )
        }
    }
}

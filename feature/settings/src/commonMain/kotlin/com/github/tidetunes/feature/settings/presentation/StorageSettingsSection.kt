package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AUDIO_CACHE_LIMIT_DISABLED_BYTES
import com.github.tidetunes.core.domain.model.DEFAULT_AUDIO_CACHE_LIMIT_BYTES

private const val MB_512_BYTES = 536_870_912L
private const val GB_2_BYTES = 2_147_483_648L

@Composable
fun StorageSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val usage = state.storageUsage
    val audioLimit = settings.audioCacheLimitBytes
    val isPreset = audioLimit == AUDIO_CACHE_LIMIT_DISABLED_BYTES ||
        audioLimit == MB_512_BYTES ||
        audioLimit == DEFAULT_AUDIO_CACHE_LIMIT_BYTES ||
        audioLimit == GB_2_BYTES

    SettingsPageLayout(title = "存储", onBack = onBack) {
        SettingsSection(title = "缓存上限") {
            SettingsChoiceRow(
                title = "关闭",
                summary = "不主动保留播放缓存",
                selected = audioLimit == AUDIO_CACHE_LIMIT_DISABLED_BYTES,
                onClick = {
                    onAction(SettingsAction.SetAudioCacheLimitBytes(AUDIO_CACHE_LIMIT_DISABLED_BYTES))
                },
            )
            SettingsChoiceRow(
                title = "512 MB",
                summary = "最多保留 512 MB 播放缓存",
                selected = audioLimit == MB_512_BYTES,
                onClick = { onAction(SettingsAction.SetAudioCacheLimitBytes(MB_512_BYTES)) },
            )
            SettingsChoiceRow(
                title = "1 GB",
                summary = "默认缓存上限",
                selected = audioLimit == DEFAULT_AUDIO_CACHE_LIMIT_BYTES,
                onClick = {
                    onAction(SettingsAction.SetAudioCacheLimitBytes(DEFAULT_AUDIO_CACHE_LIMIT_BYTES))
                },
            )
            SettingsChoiceRow(
                title = "2 GB",
                summary = "最多保留 2 GB 播放缓存",
                selected = audioLimit == GB_2_BYTES,
                onClick = { onAction(SettingsAction.SetAudioCacheLimitBytes(GB_2_BYTES)) },
            )
            SettingsChoiceRow(
                title = "自定义",
                summary = if (isPreset) "0 到 10240 MB" else formatBytes(audioLimit),
                selected = !isPreset,
                onClick = { onAction(SettingsAction.OpenCustomCacheLimitDialog) },
            )
        }

        SettingsSection(title = "占用详情") {
            SettingsInfoRow(title = "音频", value = formatBytes(usage.audioBytes))
            SettingsInfoRow(title = "图片", value = formatBytes(usage.imageBytes))
            SettingsInfoRow(title = "数据库", value = formatBytes(usage.databaseBytes))
            SettingsInfoRow(title = "日志", value = formatBytes(usage.logBytes))
            SettingsInfoRow(title = "总计", value = formatBytes(usage.totalBytes))
            SettingsInfoRow(
                title = "刷新",
                value = if (state.storageRefreshing) "刷新中" else "重新统计占用",
                enabled = !state.storageRefreshing,
                onClick = { onAction(SettingsAction.RefreshStorageUsage) },
            )
        }

        SettingsSection(title = "清理") {
            SettingsDangerRow(
                title = "清理音频",
                summary = "清理播放缓存，不会删除下载文件",
                onClick = { onAction(SettingsAction.RequestClearAudio) },
            )
            SettingsDangerRow(
                title = "清理图片",
                summary = "清理封面、缩略图等图片缓存，下次进入页面重新加载",
                onClick = { onAction(SettingsAction.RequestClearImage) },
            )
        }
    }

    SettingsInputDialog(
        show = state.customCacheLimitDialogOpen,
        title = "自定义缓存上限",
        value = state.customCacheLimitInputMb,
        onValueChange = { onAction(SettingsAction.SetCustomCacheLimitInput(it)) },
        onConfirm = { onAction(SettingsAction.ApplyCustomCacheLimit) },
        onDismiss = { onAction(SettingsAction.DismissCustomCacheLimitDialog) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearAudio,
        title = "确认清理音频",
        message = "将清理音频播放缓存，不会删除用户下载文件。",
        confirmText = "确认清理",
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation == SettingsConfirmation.ClearImage,
        title = "确认清理图片",
        message = "将清理封面、缩略图等图片缓存。清理后下次进入页面会重新加载图片。",
        confirmText = "确认清理",
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
}

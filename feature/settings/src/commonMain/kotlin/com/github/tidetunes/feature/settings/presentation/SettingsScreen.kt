package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen(
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    SettingsPageLayout(title = "设置") {
        SettingsEntryCard(
            title = "外观",
            summary = "主题、取色、语言",
            onClick = onNavigateToAppearance,
        )
        SettingsEntryCard(
            title = "播放",
            summary = "断连暂停、混合播放、常亮",
            onClick = onNavigateToPlayback,
        )
        SettingsEntryCard(
            title = "音源",
            summary = "本地音乐、WebDAV",
            onClick = onNavigateToSource,
        )
        SettingsEntryCard(
            title = "存储",
            summary = "缓存上限、占用详情、清理缓存",
            onClick = onNavigateToStorage,
        )
        SettingsEntryCard(
            title = "关于",
            summary = "版本、构建、许可、仓库",
            onClick = onNavigateToAbout,
        )
    }
}

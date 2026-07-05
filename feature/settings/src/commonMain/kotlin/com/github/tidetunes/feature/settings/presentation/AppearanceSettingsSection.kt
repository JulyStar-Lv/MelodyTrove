package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppThemeMode

@Composable
fun AppearanceSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val dynamicColorSupported = state.capabilities.dynamicColorSupported
    val effectiveDynamicColor = settings.dynamicColorEnabled && dynamicColorSupported

    SettingsPageLayout(title = "外观", onBack = onBack) {
        SettingsSection(title = "主题") {
            SettingsChoiceRow(
                title = "系统",
                summary = "跟随系统明暗外观",
                selected = settings.themeMode == AppThemeMode.System,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.System)) },
            )
            SettingsChoiceRow(
                title = "浅色",
                summary = "始终使用浅色外观",
                selected = settings.themeMode == AppThemeMode.Light,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.Light)) },
            )
            SettingsChoiceRow(
                title = "深色",
                summary = "始终使用深色外观",
                selected = settings.themeMode == AppThemeMode.Dark,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.Dark)) },
            )
        }

        SettingsSection(title = "取色") {
            SettingsChoiceRow(
                title = "系统取色",
                summary = if (dynamicColorSupported) {
                    "使用系统动态配色"
                } else {
                    "当前环境暂不支持，已使用默认配色"
                },
                selected = effectiveDynamicColor,
                enabled = dynamicColorSupported,
                onClick = { onAction(SettingsAction.SetDynamicColorEnabled(true)) },
            )
            SettingsChoiceRow(
                title = "默认配色",
                summary = "使用 TideTunes 默认配色",
                selected = !effectiveDynamicColor,
                onClick = { onAction(SettingsAction.SetDynamicColorEnabled(false)) },
            )
        }

        SettingsSection(title = "语言") {
            SettingsChoiceRow(
                title = "系统",
                summary = "跟随系统语言，重启后生效",
                selected = settings.languageMode == AppLanguageMode.System,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.System)) },
            )
            SettingsChoiceRow(
                title = "中文",
                summary = "使用中文界面，重启后生效",
                selected = settings.languageMode == AppLanguageMode.Chinese,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.Chinese)) },
            )
            SettingsChoiceRow(
                title = "English",
                summary = "Use English after restart",
                selected = settings.languageMode == AppLanguageMode.English,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.English)) },
            )
        }
    }
}

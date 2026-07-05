package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable

@Composable
fun AboutSettingsSection(
    appVersion: String,
    appBuildInfo: String,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenRepository: () -> Unit,
) {
    SettingsPageLayout(title = "关于", onBack = onBack) {
        SettingsSection(title = "关于") {
            SettingsInfoRow(title = "版本", value = appVersion.ifBlank { "暂不可用" })
            SettingsInfoRow(title = "构建", value = appBuildInfo.ifBlank { "暂不可用" })
            SettingsInfoRow(
                title = "许可",
                value = "查看开源许可",
                onClick = onOpenLicenses,
            )
            SettingsInfoRow(
                title = "仓库",
                value = TIDE_TUNES_REPOSITORY_URL,
                onClick = onOpenRepository,
            )
        }
    }
}

@Composable
fun LicensesSettingsScreen(
    onBack: () -> Unit,
) {
    SettingsPageLayout(title = "许可", onBack = onBack) {
        SettingsSection(title = "开源许可") {
            SettingsInfoRow(
                title = "许可",
                value = "首版保留入口，后续接入完整开源许可列表。",
            )
        }
    }
}

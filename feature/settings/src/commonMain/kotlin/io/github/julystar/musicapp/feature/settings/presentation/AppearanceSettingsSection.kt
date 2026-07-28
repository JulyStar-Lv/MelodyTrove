package io.github.julystar.musicapp.feature.settings.presentation

import androidx.compose.runtime.Composable
import io.github.julystar.musicapp.core.domain.model.AppLanguageMode
import io.github.julystar.musicapp.core.domain.model.AppThemeMode
import org.jetbrains.compose.resources.stringResource
import musicapp.feature.settings.generated.resources.*

@Composable
fun AppearanceSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val dynamicColorSupported = state.capabilities.dynamicColorSupported
    val effectiveDynamicColor = settings.dynamicColorEnabled && dynamicColorSupported

    SettingsPageLayout(title = stringResource(Res.string.settings_appearance_title), onBack = onBack) {
        SettingsSection(title = stringResource(Res.string.settings_theme_section)) {
            SettingsSelectRow(
                label = stringResource(Res.string.settings_theme_section),
                subtitle = stringResource(settings.themeMode.summaryResource()),
                selected = settings.themeMode,
                options = AppThemeMode.entries.toList(),
                optionLabel = { mode -> stringResource(mode.titleResource()) },
                onSelect = { onAction(SettingsAction.SetThemeMode(it)) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_color_section)) {
            if (dynamicColorSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_dynamic_color),
                    summary = stringResource(Res.string.settings_dynamic_color_summary),
                    checked = effectiveDynamicColor,
                    onCheckedChange = { onAction(SettingsAction.SetDynamicColorEnabled(it)) },
                )
            }
            SettingsInfoRow(
                title = stringResource(Res.string.settings_default_color),
                value = if (dynamicColorSupported) {
                    stringResource(Res.string.settings_default_color_summary)
                } else {
                    stringResource(Res.string.settings_dynamic_color_unavailable)
                },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_language_section)) {
            SettingsSelectRow(
                label = stringResource(Res.string.settings_language_section),
                subtitle = stringResource(settings.languageMode.summaryResource()),
                selected = settings.languageMode,
                options = AppLanguageMode.entries.toList(),
                optionLabel = { mode -> stringResource(mode.titleResource()) },
                onSelect = { onAction(SettingsAction.SetLanguageMode(it)) },
            )
        }
    }
}

private fun AppThemeMode.titleResource() = when (this) {
    AppThemeMode.System -> Res.string.settings_theme_system
    AppThemeMode.Light -> Res.string.settings_theme_light
    AppThemeMode.Dark -> Res.string.settings_theme_dark
}

private fun AppThemeMode.summaryResource() = when (this) {
    AppThemeMode.System -> Res.string.settings_theme_system_summary
    AppThemeMode.Light -> Res.string.settings_theme_light_summary
    AppThemeMode.Dark -> Res.string.settings_theme_dark_summary
}

private fun AppLanguageMode.titleResource() = when (this) {
    AppLanguageMode.System -> Res.string.settings_language_system
    AppLanguageMode.Chinese -> Res.string.settings_language_chinese
    AppLanguageMode.English -> Res.string.settings_language_english
}

private fun AppLanguageMode.summaryResource() = when (this) {
    AppLanguageMode.System -> Res.string.settings_language_system_summary
    AppLanguageMode.Chinese -> Res.string.settings_language_chinese_summary
    AppLanguageMode.English -> Res.string.settings_language_english_summary
}

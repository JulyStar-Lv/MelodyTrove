package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppThemeMode
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

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
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_theme_system),
                summary = stringResource(Res.string.settings_theme_system_summary),
                selected = settings.themeMode == AppThemeMode.System,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.System)) },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_theme_light),
                summary = stringResource(Res.string.settings_theme_light_summary),
                selected = settings.themeMode == AppThemeMode.Light,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.Light)) },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_theme_dark),
                summary = stringResource(Res.string.settings_theme_dark_summary),
                selected = settings.themeMode == AppThemeMode.Dark,
                onClick = { onAction(SettingsAction.SetThemeMode(AppThemeMode.Dark)) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_color_section)) {
            if (dynamicColorSupported) {
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_dynamic_color),
                    summary = stringResource(Res.string.settings_dynamic_color_summary),
                    selected = effectiveDynamicColor,
                    onClick = { onAction(SettingsAction.SetDynamicColorEnabled(true)) },
                )
            }
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_default_color),
                summary = if (dynamicColorSupported) {
                    stringResource(Res.string.settings_default_color_summary)
                } else {
                    stringResource(Res.string.settings_dynamic_color_unavailable)
                },
                selected = !effectiveDynamicColor,
                onClick = { onAction(SettingsAction.SetDynamicColorEnabled(false)) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_language_section)) {
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_language_system),
                summary = stringResource(Res.string.settings_language_system_summary),
                selected = settings.languageMode == AppLanguageMode.System,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.System)) },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_language_chinese),
                summary = stringResource(Res.string.settings_language_chinese_summary),
                selected = settings.languageMode == AppLanguageMode.Chinese,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.Chinese)) },
            )
            SettingsChoiceRow(
                title = stringResource(Res.string.settings_language_english),
                summary = stringResource(Res.string.settings_language_english_summary),
                selected = settings.languageMode == AppLanguageMode.English,
                onClick = { onAction(SettingsAction.SetLanguageMode(AppLanguageMode.English)) },
            )
        }
    }
}

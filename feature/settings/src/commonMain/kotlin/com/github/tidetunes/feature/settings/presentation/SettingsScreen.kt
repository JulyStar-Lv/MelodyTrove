package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.domain.model.AppLanguageMode
import com.github.tidetunes.core.domain.model.AppThemeMode
import com.github.tidetunes.core.presentation.components.TideSlider
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    var allowMobileNetwork by remember { mutableStateOf(false) }
    var autoDownload by remember { mutableStateOf(false) }
    var autoPlay by remember { mutableStateOf(true) }
    var loudnessNormalization by remember { mutableStateOf(true) }
    var floatingLyrics by remember { mutableStateOf(false) }
    var blurMaterial by remember { mutableStateOf(true) }
    var pluginsEnabled by remember { mutableStateOf(true) }

    SettingsPageLayout(title = "Settings") {
        SettingsSection(title = "TRANSFER & DOWNLOAD") {
            SettingsSwitchRow(
                title = "Allow Mobile Network",
                summary = "Stream over cellular data",
                checked = allowMobileNetwork,
                marker = "≋",
                accentColor = TideTunesBrand.SupportBlue,
                onCheckedChange = { allowMobileNetwork = it },
            )
            SettingsInfoRow(
                title = "Streaming Quality",
                value = "Hi-Res FLAC 24bit/192kHz",
                marker = "◠",
                accentColor = TideTunesBrand.Secondary,
                onClick = onNavigateToPlayback,
            )
            SettingsInfoRow(
                title = "Auto Download",
                value = "Liked songs",
                marker = "↓",
                accentColor = TideTunesBrand.SupportBlue,
                onClick = {
                    autoDownload = !autoDownload
                    onNavigateToStorage()
                },
            )
            CacheSizeRow(
                cacheBytes = state.settings.audioCacheLimitBytes,
                onCacheBytesChange = { bytes -> onAction(SettingsAction.SetAudioCacheLimitBytes(bytes)) },
            )
        }

        SettingsSection(title = "PLAYBACK") {
            SettingsSwitchRow(
                title = "Auto Play",
                summary = "Continue with similar songs",
                checked = autoPlay,
                marker = "▷",
                accentColor = TideTunesBrand.Primary,
                onCheckedChange = { autoPlay = it },
            )
            SettingsInfoRow(
                title = "Repeat Mode",
                value = "Off",
                marker = "↻",
                accentColor = TideTunesBrand.Secondary,
                onClick = onNavigateToPlayback,
            )
            SettingsInfoRow(
                title = "Sleep Timer",
                value = "Off",
                marker = "⌁",
                accentColor = TideTunesBrand.SupportBlue,
                onClick = onNavigateToPlayback,
            )
            SettingsInfoRow(
                title = "Replay Gain",
                value = "Track gain",
                marker = "≋",
                accentColor = TideTunesBrand.SupportOrange,
                onClick = onNavigateToPlayback,
            )
            SettingsSwitchRow(
                title = "Loudness Normalization",
                checked = loudnessNormalization,
                marker = "◉",
                accentColor = TideTunesBrand.SupportBlue,
                onCheckedChange = { loudnessNormalization = it },
            )
            SettingsInfoRow(
                title = "Equalizer",
                value = "Default",
                marker = "≡",
                accentColor = TideTunesBrand.Secondary,
                onClick = onNavigateToPlayback,
            )
        }

        SettingsSection(title = "LIBRARY") {
            SettingsInfoRow(
                title = "Source Manager",
                value = "6 sources connected",
                marker = "◎",
                accentColor = TideTunesBrand.SupportGreen,
                onClick = onNavigateToSource,
            )
            SettingsInfoRow(
                title = "Library Manager",
                value = "7,446 songs indexed",
                marker = "▦",
                accentColor = TideTunesBrand.SupportBlue,
                onClick = onNavigateToSource,
            )
            SettingsSwitchRow(
                title = "Auto Scan",
                summary = "Scan library on startup",
                checked = state.settings.localScanSubdirectories,
                marker = "↺",
                accentColor = TideTunesBrand.SupportGreen,
                onCheckedChange = { onAction(SettingsAction.SetLocalScanSubdirectories(it)) },
            )
            SettingsInfoRow(
                title = "Metadata",
                value = "Fetch artwork & tags automatically",
                marker = "◇",
                accentColor = TideTunesBrand.Primary,
                onClick = onNavigateToSource,
            )
        }

        SettingsSection(title = "LYRICS") {
            SettingsSwitchRow(
                title = "Floating Lyrics",
                summary = "Show on lock screen",
                checked = floatingLyrics,
                marker = "♫",
                accentColor = TideTunesBrand.Primary,
                onCheckedChange = { floatingLyrics = it },
            )
            SettingsInfoRow(
                title = "Font Size",
                value = "Medium",
                marker = "Aa",
                accentColor = TideTunesBrand.Secondary,
            )
            SettingsInfoRow(
                title = "Translation",
                value = state.settings.languageMode.designLabel(),
                marker = "文",
                accentColor = TideTunesBrand.SupportGreen,
            )
        }

        SettingsSection(title = "APPEARANCE") {
            SettingsSwitchRow(
                title = "Dynamic Color",
                summary = "Adapt UI to artwork",
                checked = state.settings.dynamicColorEnabled,
                enabled = state.capabilities.dynamicColorSupported || state.settings.dynamicColorEnabled,
                marker = "◐",
                accentColor = TideTunesBrand.Primary,
                onCheckedChange = { onAction(SettingsAction.SetDynamicColorEnabled(it)) },
            )
            SettingsInfoRow(
                title = "Accent Color",
                value = "TidePink #FF5B8A",
                marker = "●",
                accentColor = TideTunesBrand.Primary,
                onClick = onNavigateToAppearance,
            )
            SettingsSwitchRow(
                title = "Blur & Material",
                summary = "Glassmorphism effects",
                checked = blurMaterial,
                marker = "◌",
                accentColor = TideTunesBrand.Secondary,
                onCheckedChange = { blurMaterial = it },
            )
            SettingsInfoRow(
                title = "Icon Shape",
                value = "Rounded",
                marker = "▢",
                accentColor = TideTunesBrand.SupportOrange,
                onClick = onNavigateToAppearance,
            )
            SettingsInfoRow(
                title = "Theme",
                value = state.settings.themeMode.designLabel(),
                marker = "☾",
                accentColor = TideTunesBrand.SupportBlue,
                onClick = onNavigateToAppearance,
            )
        }

        SettingsSection(title = "PLUGINS") {
            SettingsSwitchRow(
                title = "Enable Plugins",
                summary = "Third-party extensions",
                checked = pluginsEnabled,
                marker = "◈",
                accentColor = TideTunesBrand.Secondary,
                onCheckedChange = { pluginsEnabled = it },
            )
            SettingsInfoRow(
                title = "Plugin Manager",
                value = "0 plugins installed",
                marker = "▣",
                accentColor = TideTunesBrand.SupportBlue,
            )
            SettingsEntryCard(
                title = "Explore Plugins",
                summary = null,
                marker = "⌕",
                accentColor = TideTunesBrand.Primary,
            )
        }

        SettingsSection(title = "ADVANCED SOURCES") {
            SourceLinkRow("WebDAV", "W", onNavigateToSource)
            SourceLinkRow("OneDrive", "O", onNavigateToSource)
            SourceLinkRow("Google Drive", "G", onNavigateToSource)
            SourceLinkRow("SMB / NAS", "S", onNavigateToSource)
            SourceLinkRow("Emby", "E", onNavigateToSource)
            SourceLinkRow("Plex", "P", onNavigateToSource)
            SourceLinkRow("Jellyfin", "J", onNavigateToSource)
            SourceLinkRow("Navidrome", "N", onNavigateToSource)
            SourceLinkRow("Dropbox", "D", onNavigateToSource)
            SourceLinkRow("Custom API", "C", onNavigateToSource)
        }

        SettingsSection(title = "ABOUT") {
            SettingsInfoRow(
                title = "TideTunes",
                value = "Version 3.0.0 · Build 2024.12",
                marker = "♪",
                accentColor = TideTunesBrand.Primary,
                onClick = onNavigateToAbout,
            )
            SettingsInfoRow(
                title = "Design System",
                value = "v3.0 · HyperOS × Apple Music",
                marker = "DS",
                accentColor = TideTunesBrand.Secondary,
                onClick = onNavigateToAbout,
            )
            SettingsEntryCard(
                title = "Open Source Licenses",
                summary = null,
                marker = "§",
                accentColor = TideTunesBrand.SupportGreen,
                onClick = onNavigateToAbout,
            )
        }
    }
}

@Composable
private fun CacheSizeRow(
    cacheBytes: Long,
    onCacheBytesChange: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Cache Size",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        TideSlider(
            value = cacheBytes.toCacheSliderValue(),
            onValueChange = { value -> onCacheBytesChange(value.toCacheBytes()) },
            thumbSize = 18.dp,
            activeThumbSize = 20.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "512 MB",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "4 GB",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SourceLinkRow(
    title: String,
    marker: String,
    onClick: () -> Unit,
) {
    SettingsEntryCard(
        title = title,
        summary = null,
        marker = marker,
        accentColor = TideTunesBrand.SupportGreen,
        onClick = onClick,
    )
}

private fun AppThemeMode.designLabel(): String = when (this) {
    AppThemeMode.System -> "System"
    AppThemeMode.Light -> "Light"
    AppThemeMode.Dark -> "Dark"
}

private fun AppLanguageMode.designLabel(): String = when (this) {
    AppLanguageMode.System -> "English"
    AppLanguageMode.Chinese -> "Chinese"
    AppLanguageMode.English -> "English"
}

private fun Long.toCacheSliderValue(): Float {
    val clamped = coerceIn(DESIGN_CACHE_MIN_BYTES, DESIGN_CACHE_MAX_BYTES)
    return ((clamped - DESIGN_CACHE_MIN_BYTES).toFloat() / DESIGN_CACHE_RANGE_BYTES.toFloat())
        .coerceIn(0f, 1f)
}

private fun Float.toCacheBytes(): Long {
    val fraction = coerceIn(0f, 1f)
    return DESIGN_CACHE_MIN_BYTES + (DESIGN_CACHE_RANGE_BYTES * fraction).toLong()
}

private const val DESIGN_CACHE_MIN_BYTES = 512L * 1024L * 1024L
private const val DESIGN_CACHE_MAX_BYTES = 4L * 1024L * 1024L * 1024L
private const val DESIGN_CACHE_RANGE_BYTES = DESIGN_CACHE_MAX_BYTES - DESIGN_CACHE_MIN_BYTES

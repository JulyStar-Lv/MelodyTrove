package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TideSearchBar
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.core.presentation.components.LocalTideBottomContentInset
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_chevron_right
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_image
import tidetunes.core.presentation.generated.resources.icon_lyrics
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.core.presentation.generated.resources.icon_setting
import tidetunes.core.presentation.generated.resources.icon_wifitethering
import tidetunes.feature.settings.generated.resources.*
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPlayback: () -> Unit,
    onNavigateToLyrics: () -> Unit,
    onNavigateToSource: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToNetworkCache: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim().lowercase()

    fun matches(vararg terms: String): Boolean {
        if (normalizedQuery.isBlank()) return true
        val joined = terms.joinToString(" ").lowercase()
        return joined.contains(normalizedQuery)
    }

    val scrollState = rememberScrollState()
    val collapseDistance = with(LocalDensity.current) { 88.dp.roundToPx() }
    val actionBarProgress by remember(scrollState, collapseDistance) {
        derivedStateOf {
            (scrollState.value / collapseDistance.toFloat()).coerceIn(0f, 1f)
        }
    }
    val pageTitleAlpha = (1f - actionBarProgress / 0.70f).coerceIn(0f, 1f)
    val bottomContentInset = LocalTideBottomContentInset.current

    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            val compact = maxWidth < TideTunesTokens.adaptive.largeMinWidth
            val pagePadding = if (compact) 24.dp else TideTunesTokens.spacing.pageExpanded

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = pagePadding,
                        top = if (compact) 0.dp else 16.dp,
                        end = pagePadding,
                        bottom = 16.dp + bottomContentInset,
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
        // Page title
        if (compact) {
            SettingsMobileHeader(modifier = Modifier.alpha(pageTitleAlpha))
        } else {
            Text(
                text = stringResource(Res.string.settings_title),
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(pageTitleAlpha),
            )
        }

        // Search bar
        TideSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = stringResource(Res.string.settings_search_hint),
            onSearch = {},
            onClear = { searchQuery = "" },
        )

        // Personalization
        if (matches("appearance", "theme", "language", "lyrics", "translation", "alignment")) {
            SettingsSectionCard(title = stringResource(Res.string.settings_personalization_section)) {
                if (matches("appearance", "theme", "language")) {
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_appearance_title),
                        summary = stringResource(Res.string.settings_appearance_card_summary),
                        icon = CoreRes.drawable.icon_image,
                        onClick = onNavigateToAppearance,
                    )
                }
                if (matches("lyrics", "translation", "alignment")) {
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_lyrics_title),
                        summary = stringResource(Res.string.settings_lyrics_card_summary),
                        icon = CoreRes.drawable.icon_lyrics,
                        onClick = onNavigateToLyrics,
                        showDivider = false,
                    )
                }
            }
        }

        // Playback
        if (matches("playback", "audio", "focus", "queue", "replaygain", "dsp")) {
            SettingsSectionCard(title = stringResource(Res.string.settings_playback_title)) {
                SettingsNavRow(
                    title = stringResource(Res.string.settings_playback_title),
                    summary = stringResource(Res.string.settings_playback_card_summary),
                    icon = CoreRes.drawable.icon_play,
                    onClick = onNavigateToPlayback,
                    showDivider = false,
                )
            }
        }

        // Library & data
        if (matches("library", "sources", "local", "webdav", "metadata", "plugins", "lyrico",
                "network", "cache", "streaming", "storage", "data", "cleanup", "backup")
        ) {
            SettingsSectionCard(title = stringResource(Res.string.settings_library_data_section)) {
                if (matches("library", "sources", "local", "webdav")) {
                    val sourceCount = state.sourceAccounts.size
                    val readyCount = state.enabledSourceCount
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_sources_title),
                        summary = stringResource(
                            Res.string.settings_sources_card_summary,
                            sourceCount,
                            readyCount,
                        ),
                        icon = CoreRes.drawable.icon_cloud,
                        onClick = onNavigateToSource,
                    )
                }
                if (matches("metadata", "plugins", "lyrico")) {
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_metadata_plugins),
                        summary = stringResource(Res.string.settings_metadata_plugins_summary),
                        icon = CoreRes.drawable.icon_setting,
                        onClick = onNavigateToPlugins,
                    )
                }
                if (matches("network", "cache", "streaming")) {
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_network_cache_title),
                        summary = stringResource(Res.string.settings_network_cache_card_summary),
                        icon = CoreRes.drawable.icon_wifitethering,
                        onClick = onNavigateToNetworkCache,
                    )
                }
                if (matches("storage", "data", "cleanup", "backup")) {
                    SettingsNavRow(
                        title = stringResource(Res.string.settings_storage_title),
                        summary = stringResource(
                            Res.string.settings_storage_card_summary,
                            formatBytes(state.storageUsage.totalBytes),
                        ),
                        icon = CoreRes.drawable.icon_album,
                        onClick = onNavigateToStorage,
                        showDivider = false,
                    )
                }
            }
        }

        // App & info
        if (matches("about", "version", "build", "privacy", "licenses", "logs", "diagnostics",
                "incident", "crash", "safe mode")
        ) {
            SettingsSectionCard(title = stringResource(Res.string.settings_app_info_section)) {
                if (shouldShowDiagnosticsCenter(
                        state.capabilities,
                        matches(
                            "logs",
                            "diagnostics",
                            "incident",
                            "crash",
                            "safe mode",
                            "privacy",
                        ),
                    )
                ) {
                    SettingsNavRow(
                        title = stringResource(Res.string.diagnostics_title),
                        summary = stringResource(Res.string.diagnostics_card_summary),
                        icon = CoreRes.drawable.icon_setting,
                        onClick = onNavigateToDiagnostics,
                    )
                }
                SettingsNavRow(
                    title = stringResource(Res.string.settings_about_title),
                    summary = stringResource(
                        Res.string.settings_about_card_summary,
                        appVersion.ifBlank { "—" },
                    ),
                    icon = CoreRes.drawable.icon_music_note,
                    onClick = onNavigateToAbout,
                    showDivider = false,
                )
            }
        }

        // No results
        if (normalizedQuery.isNotBlank() && !matches(
                "appearance", "playback", "lyrics", "sources", "plugins",
                "network", "storage", "about", "theme", "language", "audio",
                "focus", "queue", "replaygain", "dsp", "translation", "alignment",
                "library", "local", "webdav", "metadata", "lyrico", "cache",
                "streaming", "data", "cleanup", "backup", "version", "build",
                "privacy", "licenses",
                "logs", "diagnostics", "incident", "crash", "safe mode",
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.settings_search_empty, searchQuery),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }

                Spacer(modifier = Modifier.height(32.dp))
            }
            TideStickyGlassActionBar(
                title = stringResource(Res.string.settings_title),
                collapseFraction = actionBarProgress,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

internal fun shouldShowDiagnosticsCenter(
    capabilities: com.github.tidetunes.core.domain.model.SettingsCapabilities,
    queryMatches: Boolean,
): Boolean = capabilities.diagnosticsCenterSupported && queryMatches

@Composable
private fun SettingsMobileHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title1.copy(
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Section Card ──

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    TideSettingsGroup(title = title, content = content)
}

// ── Nav Row ──

@Composable
private fun SettingsNavRow(
    title: String,
    summary: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
        }
        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summary,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Chevron
        Icon(
            painter = painterResource(CoreRes.drawable.icon_chevron_right),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 70.dp)
                .height(1.dp)
                .background(MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.35f)),
        )
    }
}

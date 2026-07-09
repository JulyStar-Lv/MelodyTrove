package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.ImportCover
import com.github.tidetunes.core.presentation.components.SimpleFormText
import com.github.tidetunes.core.presentation.components.TideTabItem
import com.github.tidetunes.core.presentation.components.TideTabs
import com.github.tidetunes.core.presentation.components.TideTabsVariant
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.icon_download
import tidetunes.feature.playlist.generated.resources.music_count_unit
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_cancel
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_ok
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_reset
import tidetunes.feature.playlist.generated.resources.playlists_dialog_cover
import tidetunes.feature.playlist.generated.resources.playlists_dialog_import_info
import tidetunes.feature.playlist.generated.resources.playlists_dialog_playlist_full_import_desc
import tidetunes.feature.playlist.generated.resources.playlists_dialog_playlist_name
import tidetunes.feature.playlist.generated.resources.playlists_dialog_tab_empty
import tidetunes.feature.playlist.generated.resources.playlists_dialog_tab_full
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CreatePlaylistScreen(
    state: CreatePlaylistState,
    onAction: (CreatePlaylistAction) -> Unit,
) {
    TideDialog(
        show = state.isOpen,
        onDismiss = { onAction(CreatePlaylistAction.Close) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TideTabs(
                items = listOf(
                    TideTabItem(label = stringResource(Res.string.playlists_dialog_tab_full)),
                    TideTabItem(label = stringResource(Res.string.playlists_dialog_tab_empty)),
                ),
                selectedIndex = if (state.mode == CreatePlaylistTab.Full) 0 else 1,
                onSelectedIndexChange = { index ->
                    onAction(
                        if (index == 0) {
                            CreatePlaylistAction.SwitchToFull
                        } else {
                            CreatePlaylistAction.SwitchToEmpty
                        },
                    )
                },
                variant = TideTabsVariant.Segmented,
            )
            if (state.mode == CreatePlaylistTab.Full) {
                FullImportSection(state = state, onAction = onAction)
            } else {
                SimpleFormText(
                    label = stringResource(Res.string.playlists_dialog_playlist_name),
                    value = state.name,
                    onChange = { onAction(CreatePlaylistAction.UpdateName(it)) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row {
                    if (state.fullImported && state.mode == CreatePlaylistTab.Full) {
                        TideTextButton(
                            text = stringResource(Res.string.playlists_dialog_button_reset),
                            variant = TideTextButtonVariant.Default,
                            size = TideTextButtonSize.Medium,
                            onClick = { onAction(CreatePlaylistAction.Reset) },
                        )
                    }
                }
                Row {
                    TideTextButton(
                        text = stringResource(Res.string.playlists_dialog_button_cancel),
                        variant = TideTextButtonVariant.Default,
                        size = TideTextButtonSize.Medium,
                        onClick = { onAction(CreatePlaylistAction.Close) },
                    )
                    TideTextButton(
                        text = stringResource(Res.string.playlists_dialog_button_ok),
                        variant = TideTextButtonVariant.Primary,
                        size = TideTextButtonSize.Medium,
                        enabled = state.canSubmit,
                        onClick = { onAction(CreatePlaylistAction.Submit) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FullImportSection(
    state: CreatePlaylistState,
    onAction: (CreatePlaylistAction) -> Unit,
) {
    if (!state.fullImported) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .clip(RoundedCornerShape(TideTunesTokens.shapes.lg))
                .clickable { onAction(CreatePlaylistAction.PrepareImport) }
                .background(MiuixTheme.colorScheme.tertiaryContainer)
                .border(
                    1.dp,
                    TideTunesBrand.Primary.copy(alpha = 0.18f),
                    RoundedCornerShape(TideTunesTokens.shapes.lg),
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_download),
                contentDescription = null,
                tint = TideTunesBrand.Primary,
            )
            Box(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.playlists_dialog_playlist_full_import_desc),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.footnote1,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        val musicCountSuffix = stringResource(Res.string.music_count_unit)

        Column(modifier = Modifier.fillMaxWidth()) {
            FullImportHeader(text = stringResource(Res.string.playlists_dialog_import_info))
            Text(
                text = "${state.musicCount} $musicCountSuffix",
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Box(modifier = Modifier.height(12.dp))
            FullImportHeader(text = stringResource(Res.string.playlists_dialog_playlist_name))
            SimpleFormText(
                label = null,
                value = state.name,
                onChange = { onAction(CreatePlaylistAction.UpdateName(it)) },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                for (name in state.recommendNames) {
                    TideTextButton(
                        modifier = Modifier.widthIn(max = 120.dp),
                        text = name,
                        variant = TideTextButtonVariant.Default,
                        size = TideTextButtonSize.Small,
                        enabled = true,
                        onClick = { onAction(CreatePlaylistAction.UpdateName(name)) },
                    )
                }
            }
            Box(modifier = Modifier.height(12.dp))
            FullImportHeader(text = stringResource(Res.string.playlists_dialog_cover))
            ImportCover(
                artwork = state.coverArtwork,
                onAdd = { onAction(CreatePlaylistAction.NavigateToImport) },
                onRemove = { onAction(CreatePlaylistAction.ClearCover) },
            )
        }
    }
}

@Composable
private fun FullImportHeader(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.tidetunes.core.presentation.components.ImportCover
import com.github.tidetunes.core.presentation.components.SimpleFormText
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.StringResource
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

@Composable
fun CreatePlaylistScreen(
    state: CreatePlaylistState,
    onAction: (CreatePlaylistAction) -> Unit,
) {
    if (!state.isOpen) return

    Dialog(onDismissRequest = { onAction(CreatePlaylistAction.Close) }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(24.dp, 24.dp),
        ) {
            Row {
                Tab(
                    stringRes = Res.string.playlists_dialog_tab_full,
                    isActive = state.mode == CreatePlaylistTab.Full,
                    onClick = { onAction(CreatePlaylistAction.SwitchToFull) },
                )
                Tab(
                    stringRes = Res.string.playlists_dialog_tab_empty,
                    isActive = state.mode == CreatePlaylistTab.Empty,
                    onClick = { onAction(CreatePlaylistAction.SwitchToEmpty) },
                )
            }
            Box(modifier = Modifier.height(8.dp))
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
                        TideTunesTextButton(
                            text = stringResource(Res.string.playlists_dialog_button_reset),
                            type = TideTunesTextButtonType.Primary,
                            size = TideTunesTextButtonSize.Medium,
                            onClick = { onAction(CreatePlaylistAction.Reset) },
                        )
                    }
                }
                Row {
                    TideTunesTextButton(
                        text = stringResource(Res.string.playlists_dialog_button_cancel),
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Medium,
                        onClick = { onAction(CreatePlaylistAction.Close) },
                    )
                    TideTunesTextButton(
                        text = stringResource(Res.string.playlists_dialog_button_ok),
                        type = TideTunesTextButtonType.Primary,
                        size = TideTunesTextButtonSize.Medium,
                        disabled = !state.canSubmit,
                        onClick = { onAction(CreatePlaylistAction.Submit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Tab(
    stringRes: StringResource,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val activeColor = MiuixTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            modifier = Modifier.padding(8.dp, 0.dp),
            text = stringResource(stringRes),
            fontSize = 11.sp,
            color = if (!isActive) Color.Unspecified else activeColor,
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(1.dp)
                    .offset(0.dp, (-4).dp)
                    .background(activeColor),
            )
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
                .clip(RoundedCornerShape(6.dp))
                .clickable { onAction(CreatePlaylistAction.PrepareImport) }
                .background(MiuixTheme.colorScheme.surfaceVariant)
                .padding(0.dp, 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_download),
                contentDescription = null,
            )
            Box(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.playlists_dialog_playlist_full_import_desc),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        val musicCountSuffix = stringResource(Res.string.music_count_unit)

        Column(modifier = Modifier.fillMaxWidth()) {
            FullImportHeader(text = stringResource(Res.string.playlists_dialog_import_info))
            Text(text = "${state.musicCount} $musicCountSuffix")
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
                    TideTunesTextButton(
                        modifier = Modifier.widthIn(max = 120.dp),
                        text = name,
                        type = TideTunesTextButtonType.Default,
                        size = TideTunesTextButtonSize.Small,
                        disabled = false,
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
    Text(text = text, fontSize = 10.sp)
}

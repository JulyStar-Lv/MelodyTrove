package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.tidetunes.core.presentation.components.ImportCover
import com.github.tidetunes.core.presentation.components.SimpleFormText
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_cancel
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_ok
import tidetunes.feature.playlist.generated.resources.playlists_dialog_cover
import tidetunes.feature.playlist.generated.resources.playlists_dialog_playlist_name

@Composable
fun EditPlaylistScreen(
    state: EditPlaylistState,
    onAction: (EditPlaylistAction) -> Unit,
) {
    if (!state.isOpen) return

    Dialog(
        onDismissRequest = { onAction(EditPlaylistAction.Close) }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(24.dp, 24.dp),
        ) {
            EditHeader(text = stringResource(Res.string.playlists_dialog_playlist_name))
            SimpleFormText(
                label = null,
                value = state.name,
                onChange = { onAction(EditPlaylistAction.UpdateName(it)) },
            )
            Box(modifier = Modifier.height(12.dp))
            EditHeader(text = stringResource(Res.string.playlists_dialog_cover))
            ImportCover(
                artwork = state.coverArtwork,
                onAdd = { onAction(EditPlaylistAction.NavigateToCoverImport) },
                onRemove = { onAction(EditPlaylistAction.ClearCover) },
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TideTunesTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_cancel),
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(EditPlaylistAction.Close) },
                )
                TideTunesTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_ok),
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    disabled = !state.canSubmit,
                    onClick = { onAction(EditPlaylistAction.Submit) },
                )
            }
        }
    }
}

@Composable
private fun EditHeader(text: String) {
    Text(text = text, fontSize = 10.sp)
}

package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.ImportCover
import com.github.tidetunes.core.presentation.components.SimpleFormText
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_cancel
import tidetunes.feature.playlist.generated.resources.playlists_dialog_button_ok
import tidetunes.feature.playlist.generated.resources.playlists_dialog_cover
import tidetunes.feature.playlist.generated.resources.playlists_dialog_playlist_name
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EditPlaylistScreen(
    state: EditPlaylistState,
    onAction: (EditPlaylistAction) -> Unit,
) {
    TideDialog(
        show = state.isOpen,
        onDismiss = { onAction(EditPlaylistAction.Close) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                TideTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_cancel),
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Medium,
                    onClick = { onAction(EditPlaylistAction.Close) },
                )
                TideTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_ok),
                    variant = TideTextButtonVariant.Primary,
                    size = TideTextButtonSize.Medium,
                    enabled = state.canSubmit,
                    onClick = { onAction(EditPlaylistAction.Submit) },
                )
            }
        }
    }
}

@Composable
private fun EditHeader(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

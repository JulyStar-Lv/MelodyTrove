package io.github.julystar.musicapp.feature.playlist.presentation

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
import io.github.julystar.musicapp.core.presentation.components.ImportCover
import io.github.julystar.musicapp.core.presentation.components.SimpleFormText
import io.github.julystar.musicapp.core.presentation.components.DesignDialog
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import org.jetbrains.compose.resources.stringResource
import musicapp.feature.playlist.generated.resources.Res
import musicapp.feature.playlist.generated.resources.playlists_dialog_button_cancel
import musicapp.feature.playlist.generated.resources.playlists_dialog_button_ok
import musicapp.feature.playlist.generated.resources.playlists_dialog_cover
import musicapp.feature.playlist.generated.resources.playlists_dialog_playlist_name
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EditPlaylistScreen(
    state: EditPlaylistState,
    onAction: (EditPlaylistAction) -> Unit,
) {
    DesignDialog(
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
                DesignTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_cancel),
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Medium,
                    onClick = { onAction(EditPlaylistAction.Close) },
                )
                DesignTextButton(
                    text = stringResource(Res.string.playlists_dialog_button_ok),
                    variant = DesignTextButtonVariant.Primary,
                    size = DesignTextButtonSize.Medium,
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

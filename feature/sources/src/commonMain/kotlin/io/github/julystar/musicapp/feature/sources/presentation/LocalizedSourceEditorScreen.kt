package io.github.julystar.musicapp.feature.sources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.DesignIconButton
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonVariant
import io.github.julystar.musicapp.core.presentation.theme.DesignPalette
import musicapp.feature.sources.generated.resources.Res
import musicapp.feature.sources.generated.resources.icon_arrow_back
import musicapp.feature.sources.generated.resources.icon_delete
import musicapp.feature.sources.generated.resources.icon_link
import musicapp.feature.sources.generated.resources.icon_save
import musicapp.feature.sources.generated.resources.source_editor_back
import musicapp.feature.sources.generated.resources.source_editor_delete
import musicapp.feature.sources.generated.resources.source_editor_edit
import musicapp.feature.sources.generated.resources.source_editor_new
import musicapp.feature.sources.generated.resources.source_editor_save
import musicapp.feature.sources.generated.resources.source_editor_source
import musicapp.feature.sources.generated.resources.source_editor_test
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Localized production wrapper for the source editor.
 * The credential and provider forms remain in [SourceEditorScreen]; this wrapper
 * replaces its legacy hard-coded header without changing source state handling.
 */
@Composable
fun LocalizedSourceEditorScreen(
    state: SourceEditorState,
    onAction: (SourceEditorAction) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SourceEditorScreen(state = state, onAction = onAction)
        Row(
            modifier = Modifier
                .zIndex(2f)
                .fillMaxWidth()
                .height(64.dp)
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesignIconButton(
                size = DesignIconButtonSize.Medium,
                variant = DesignIconButtonVariant.Default,
                painter = painterResource(Res.drawable.icon_arrow_back),
                contentDescription = stringResource(Res.string.source_editor_back),
                onClick = { onAction(SourceEditorAction.Back) },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank { stringResource(Res.string.source_editor_source) },
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (state.isNew) {
                            Res.string.source_editor_new
                        } else {
                            Res.string.source_editor_edit
                        },
                    ),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isNew) {
                    DesignIconButton(
                        size = DesignIconButtonSize.Medium,
                        variant = DesignIconButtonVariant.Error,
                        painter = painterResource(Res.drawable.icon_delete),
                        contentDescription = stringResource(Res.string.source_editor_delete),
                        enabled = !state.isWorking,
                        onClick = { onAction(SourceEditorAction.Delete) },
                    )
                }
                DesignIconButton(
                    size = DesignIconButtonSize.Medium,
                    variant = DesignIconButtonVariant.Default,
                    painter = painterResource(Res.drawable.icon_link),
                    contentDescription = stringResource(Res.string.source_editor_test),
                    tint = when (state.testStatus) {
                        SourceTestStatus.Success -> DesignPalette.SupportGreen
                        SourceTestStatus.Failed -> MiuixTheme.colorScheme.error
                        else -> Color.Unspecified
                    },
                    enabled = !state.isWorking,
                    onClick = { onAction(SourceEditorAction.TestConnection) },
                )
                DesignIconButton(
                    size = DesignIconButtonSize.Medium,
                    variant = DesignIconButtonVariant.PrimaryFilled,
                    painter = painterResource(Res.drawable.icon_save),
                    contentDescription = stringResource(Res.string.source_editor_save),
                    enabled = !state.isWorking,
                    onClick = { onAction(SourceEditorAction.Save) },
                )
            }
        }
    }
}

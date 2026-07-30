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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.julystar.musicapp.core.presentation.components.DesignIconButton
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonColors
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonVariant
import musicapp.feature.sources.generated.resources.Res
import musicapp.feature.sources.generated.resources.icon_back
import musicapp.feature.sources.generated.resources.icon_deleteseep
import musicapp.feature.sources.generated.resources.icon_ok
import musicapp.feature.sources.generated.resources.icon_wifitethering
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
 *
 * The provider and credential forms remain in [SourceEditorScreen]. This wrapper
 * only replaces the legacy hard-coded header and delegates all actions to the
 * existing source-editor state machine.
 */
@Composable
fun LocalizedSourceEditorScreen(
    state: SourceEditorState,
    onAction: (SourceEditorAction) -> Unit,
) {
    val testColors = when (state.testStatus) {
        SourceConnectionTestStatus.None -> null
        SourceConnectionTestStatus.Testing -> DesignIconButtonColors(
            iconTint = MiuixTheme.colorScheme.onTertiaryContainer,
        )
        SourceConnectionTestStatus.Success -> DesignIconButtonColors(
            iconTint = MiuixTheme.colorScheme.primary,
        )
        else -> DesignIconButtonColors(
            iconTint = MiuixTheme.colorScheme.error,
        )
    }

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
                painter = painterResource(Res.drawable.icon_back),
                contentDescription = stringResource(Res.string.source_editor_back),
                onClick = { onAction(SourceEditorAction.NavigateBack) },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.ifBlank {
                        stringResource(Res.string.source_editor_source)
                    },
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (state.isCreated) {
                            Res.string.source_editor_new
                        } else {
                            Res.string.source_editor_edit
                        },
                    ),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isCreated) {
                    DesignIconButton(
                        size = DesignIconButtonSize.Medium,
                        variant = DesignIconButtonVariant.Error,
                        painter = painterResource(Res.drawable.icon_deleteseep),
                        contentDescription = stringResource(Res.string.source_editor_delete),
                        onClick = { onAction(SourceEditorAction.OpenRemoveDialog) },
                    )
                }
                DesignIconButton(
                    size = DesignIconButtonSize.Medium,
                    variant = DesignIconButtonVariant.Default,
                    painter = painterResource(Res.drawable.icon_wifitethering),
                    contentDescription = stringResource(Res.string.source_editor_test),
                    colors = testColors,
                    enabled = state.testStatus != SourceConnectionTestStatus.Testing,
                    onClick = { onAction(SourceEditorAction.TestConnection) },
                )
                DesignIconButton(
                    size = DesignIconButtonSize.Medium,
                    variant = DesignIconButtonVariant.Primary,
                    painter = painterResource(Res.drawable.icon_ok),
                    contentDescription = stringResource(Res.string.source_editor_save),
                    onClick = { onAction(SourceEditorAction.Save) },
                )
            }
        }
    }
}

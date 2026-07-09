package com.github.tidetunes.feature.sources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TideChip
import com.github.tidetunes.core.presentation.components.TideStatusBadge
import com.github.tidetunes.core.presentation.components.TideStatusTone
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.sources.generated.resources.Res
import tidetunes.feature.sources.generated.resources.dashboard_devices_add
import tidetunes.feature.sources.generated.resources.icon_cloud
import tidetunes.feature.sources.generated.resources.icon_plus

@Composable
fun SourcesScreen(
    state: SourcesState,
    onAction: (SourcesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = modifier) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageMedium
        Column(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.sources.isEmpty()) {
                EmptySourcesCard(
                    onClick = { onAction(SourcesAction.AddSource) },
                )
                return@Column
            }

            state.sources.forEach { source ->
                SourceCard(
                    source = source,
                    onClick = { onAction(SourcesAction.OpenSource(source.id)) },
                )
            }
        }
    }
}

@Composable
private fun EmptySourcesCard(
    onClick: () -> Unit,
) {
    TideCardSurface(
        modifier = Modifier.height(96.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(Res.drawable.icon_plus),
                contentDescription = null,
            )
            Box(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(Res.string.dashboard_devices_add),
                textAlign = TextAlign.Center,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SourceCard(
    source: SourceAccountUi,
    onClick: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    TideCardSurface(
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.height(164.dp))
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(shapes.md))
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(Res.drawable.icon_cloud),
                    tint = MiuixTheme.colorScheme.primary,
                    contentDescription = null,
                )
            }
            Box(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TideStatusBadge(
                        label = "Configured",
                        tone = TideStatusTone.Success,
                    )
                    Text(
                        text = source.sourceType,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = source.title,
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Storage: ${source.storageLabel()}",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Music: ${source.musicCountLabel()}",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SourceActionStrip()
            }
            TideChevron(
                direction = TideChevronDirection.Right,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SourceActionStrip() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TideChip(
            label = "Sync",
            enabled = false,
        )
        TideChip(
            label = "Logs",
            enabled = false,
        )
        TideChip(
            label = "Settings",
            selected = true,
        )
    }
}

private fun SourceAccountUi.storageLabel(): String {
    return subtitle.ifBlank { "Default library" }
}

private fun SourceAccountUi.musicCountLabel(): String {
    val unit = if (musicCount == 1L) "track" else "tracks"
    return "$musicCount $unit"
}

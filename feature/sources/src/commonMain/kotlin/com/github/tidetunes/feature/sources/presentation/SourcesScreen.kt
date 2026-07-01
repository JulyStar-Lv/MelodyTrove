package com.github.tidetunes.feature.sources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(
        modifier = modifier,
    ) {
        if (state.sources.isEmpty()) {
            EmptySourcesCard(
                onClick = { onAction(SourcesAction.AddSource) },
            )
            return
        }

        state.sources.forEach { source ->
            SourceRow(
                source = source,
                onClick = { onAction(SourcesAction.OpenSource(source.id)) },
            )
        }
    }
}

@Composable
private fun EmptySourcesCard(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
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
            )
        }
    }
}

@Composable
private fun SourceRow(
    source: SourceAccountUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.height(48.dp))
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(Res.drawable.icon_cloud),
            contentDescription = null,
        )
        Box(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = source.title,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (source.subtitle.isNotBlank()) {
                Text(
                    text = source.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


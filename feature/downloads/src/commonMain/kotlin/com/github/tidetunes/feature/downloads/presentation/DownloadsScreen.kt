package com.github.tidetunes.feature.downloads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideEmptyState
import com.github.tidetunes.core.presentation.components.TideIconBadge
import com.github.tidetunes.core.presentation.components.TideIconBadgeVariant
import com.github.tidetunes.core.presentation.components.TideLinearProgressIndicator
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideStatusBadge
import com.github.tidetunes.core.presentation.components.TideStatusTone
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.downloads.generated.resources.Res
import tidetunes.feature.downloads.generated.resources.downloads_cancel
import tidetunes.feature.downloads.generated.resources.downloads_empty
import tidetunes.feature.downloads.generated.resources.downloads_pause
import tidetunes.feature.downloads.generated.resources.downloads_resume
import tidetunes.feature.downloads.generated.resources.downloads_retry
import tidetunes.feature.downloads.generated.resources.downloads_title
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DownloadsScreen(
    state: DownloadsState,
    onAction: (DownloadsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            TidePageHeader(
                title = stringResource(Res.string.downloads_title),
                subtitle = "${state.tasks.size} tasks",
            )
            if (state.tasks.isEmpty()) {
                TideEmptyState(
                    title = stringResource(Res.string.downloads_empty),
                    message = "Start a download from the library or search",
                    modifier = Modifier.weight(1f),
                )
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.tasks, key = { index, task -> task.lazyListKey(index) }) { _, task ->
                    DownloadTaskRow(task = task, onAction = onAction)
                }
            }
        }
    }
}

internal fun DownloadTaskUi.lazyListKey(index: Int): String = "download-task-$index-${id.value}"

@Composable
private fun DownloadTaskRow(task: DownloadTaskUi, onAction: (DownloadsAction) -> Unit) {
    TideCardSurface(contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TideIconBadge(variant = TideIconBadgeVariant.Neutral, marker = "D")
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, color = MiuixTheme.colorScheme.onSurface, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = task.subtitle, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TideStatusBadge(label = task.statusLabel, tone = TideStatusTone.Info)
            }
            task.progressFraction?.let { progress -> TideLinearProgressIndicator(progress = progress) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.progressLabel, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    task.errorMessage?.let { message ->
                        Text(text = message, color = MiuixTheme.colorScheme.error, style = MiuixTheme.textStyles.footnote1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                DownloadTaskActions(task = task, onAction = onAction)
            }
        }
    }
}

@Composable
private fun DownloadTaskActions(task: DownloadTaskUi, onAction: (DownloadsAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (task.canPause) TideTextButton(text = stringResource(Res.string.downloads_pause), variant = TideTextButtonVariant.Primary, size = TideTextButtonSize.Small, onClick = { onAction(DownloadsAction.Pause(task.id)) })
        if (task.canResume) TideTextButton(text = stringResource(Res.string.downloads_resume), variant = TideTextButtonVariant.Primary, size = TideTextButtonSize.Small, onClick = { onAction(DownloadsAction.Resume(task.id)) })
        if (task.canRetry) TideTextButton(text = stringResource(Res.string.downloads_retry), variant = TideTextButtonVariant.Primary, size = TideTextButtonSize.Small, onClick = { onAction(DownloadsAction.Retry(task.id)) })
        if (task.canCancel) TideTextButton(text = stringResource(Res.string.downloads_cancel), variant = TideTextButtonVariant.Error, size = TideTextButtonSize.Small, onClick = { onAction(DownloadsAction.Cancel(task.id)) })
    }
}

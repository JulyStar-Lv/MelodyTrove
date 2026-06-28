package com.github.tidetunes.feature.downloads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.downloads.generated.resources.Res
import tidetunes.feature.downloads.generated.resources.downloads_cancel
import tidetunes.feature.downloads.generated.resources.downloads_empty
import tidetunes.feature.downloads.generated.resources.downloads_pause
import tidetunes.feature.downloads.generated.resources.downloads_resume
import tidetunes.feature.downloads.generated.resources.downloads_retry
import tidetunes.feature.downloads.generated.resources.downloads_title
import tidetunes.feature.downloads.generated.resources.icon_download

@Composable
fun DownloadsScreen(
    state: DownloadsState,
    onAction: (DownloadsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.downloads_title),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
        )
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.downloads_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = state.tasks,
                key = { task -> task.id.value },
            ) { task ->
                DownloadTaskRow(
                    task = task,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun DownloadTaskRow(
    task: DownloadTaskUi,
    onAction: (DownloadsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_download),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = task.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = task.statusLabel,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        task.progressFraction?.let { progress ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp)),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.progressLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                task.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DownloadTaskActions(
                task = task,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun DownloadTaskActions(
    task: DownloadTaskUi,
    onAction: (DownloadsAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (task.canPause) {
            TideTunesTextButton(
                text = stringResource(Res.string.downloads_pause),
                type = TideTunesTextButtonType.Primary,
                size = TideTunesTextButtonSize.Small,
                onClick = { onAction(DownloadsAction.Pause(task.id)) },
            )
        }
        if (task.canResume) {
            TideTunesTextButton(
                text = stringResource(Res.string.downloads_resume),
                type = TideTunesTextButtonType.Primary,
                size = TideTunesTextButtonSize.Small,
                onClick = { onAction(DownloadsAction.Resume(task.id)) },
            )
        }
        if (task.canRetry) {
            TideTunesTextButton(
                text = stringResource(Res.string.downloads_retry),
                type = TideTunesTextButtonType.Primary,
                size = TideTunesTextButtonSize.Small,
                onClick = { onAction(DownloadsAction.Retry(task.id)) },
            )
        }
        if (task.canCancel) {
            TideTunesTextButton(
                text = stringResource(Res.string.downloads_cancel),
                type = TideTunesTextButtonType.Error,
                size = TideTunesTextButtonSize.Small,
                onClick = { onAction(DownloadsAction.Cancel(task.id)) },
            )
        }
    }
}

package com.github.tidetunes.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.dashboard.generated.resources.Res
import tidetunes.feature.dashboard.generated.resources.icon_download
import tidetunes.feature.dashboard.generated.resources.icon_plus
import tidetunes.feature.dashboard.generated.resources.icon_timelapse
import tidetunes.feature.dashboard.generated.resources.dashboard_devices
import tidetunes.feature.dashboard.generated.resources.dashboard_devices_add
import tidetunes.feature.dashboard.generated.resources.dashboard_downloads
import tidetunes.feature.dashboard.generated.resources.dashboard_import_cancel
import tidetunes.feature.dashboard.generated.resources.dashboard_import_pause
import tidetunes.feature.dashboard.generated.resources.dashboard_import_resume
import tidetunes.feature.dashboard.generated.resources.dashboard_import_retry
import tidetunes.feature.dashboard.generated.resources.dashboard_imports
import tidetunes.feature.dashboard.generated.resources.dashboard_sleep_mode

private val paddingX = 24.dp
private val paddingY = 12.dp

@Composable
private fun Title(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardState,
    onAction: (DashboardAction) -> Unit,
    sourcesContent: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
        ) {
            Title(title = stringResource(Res.string.dashboard_sleep_mode))
        }
        SleepModeBlock(
            enabled = state.sleepEnabled,
            hour = state.sleepHour,
            minute = state.sleepMinute,
            onOpenTimer = { onAction(DashboardAction.OpenSleepTimer) },
        )
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
        ) {
            Title(title = stringResource(Res.string.dashboard_imports))
        }
        ImportStatusBlock(
            jobs = state.importJobs,
            onPause = { onAction(DashboardAction.PauseImport(it)) },
            onResume = { onAction(DashboardAction.ResumeImport(it)) },
            onRetry = { onAction(DashboardAction.RetryImport(it)) },
            onCancel = { onAction(DashboardAction.CancelImport(it)) },
        )
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
        ) {
            Title(title = stringResource(Res.string.dashboard_downloads))
        }
        DownloadsBlock(
            onClick = { onAction(DashboardAction.NavigateToDownloads) },
        )
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Title(title = stringResource(Res.string.dashboard_devices))
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Small,
                buttonType = TideTunesIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_plus),
                onClick = { onAction(DashboardAction.NavigateToAddDevice) },
            )
        }
        sourcesContent()
    }
}

@Composable
private fun SleepModeBlock(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onOpenTimer: () -> Unit,
) {
    val blockBg = if (enabled) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(paddingX, 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenTimer() },
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .background(blockBg)
                .padding(32.dp, 24.dp),
        ) {
            Text(
                text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                fontSize = 32.sp,
                color = tint,
            )
            Icon(
                painter = painterResource(Res.drawable.icon_timelapse),
                contentDescription = null,
                tint = tint,
            )
        }
    }
}

@Composable
private fun ImportStatusBlock(
    jobs: List<ImportJobUi>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    if (jobs.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(paddingX, paddingY),
    ) {
        jobs.forEach { job ->
            ImportJobRow(
                job = job,
                onPause = { onPause(job.id) },
                onResume = { onResume(job.id) },
                onRetry = { onRetry(job.id) },
                onCancel = { onCancel(job.id) },
            )
        }
    }
}

@Composable
private fun ImportJobRow(
    job: ImportJobUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val bgColor = if (job.hasError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val titleColor = if (job.hasError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val detailColor = if (job.hasError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp, 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = job.statusLabel,
                color = titleColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${job.importedCount}/${job.scannedCount}",
                    color = detailColor,
                    fontSize = 12.sp,
                )
                if (job.status == com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus.Queued ||
                    job.status == com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus.Running
                ) {
                    Text(
                        text = stringResource(Res.string.dashboard_import_pause),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onPause),
                    )
                }
                if (job.canResume) {
                    Text(
                        text = stringResource(Res.string.dashboard_import_resume),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onResume),
                    )
                }
                if (job.canRetry) {
                    Text(
                        text = stringResource(Res.string.dashboard_import_retry),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onRetry),
                    )
                }
                if (job.isActive) {
                    Text(
                        text = stringResource(Res.string.dashboard_import_cancel),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(onClick = onCancel),
                    )
                }
            }
        }
        Text(
            text = "skipped ${job.skippedCount} · failed ${job.failedCount}",
            color = detailColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val checkpoint = job.checkpoint
        if (!checkpoint.isNullOrBlank()) {
            Text(
                text = checkpoint.substringAfterLast('/'),
                color = detailColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val error = job.errorMessage
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = detailColor,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DownloadsBlock(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(paddingX, paddingY)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(Res.drawable.icon_download),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Box(modifier = Modifier.padding(8.dp))
        Text(
            text = stringResource(Res.string.dashboard_downloads),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

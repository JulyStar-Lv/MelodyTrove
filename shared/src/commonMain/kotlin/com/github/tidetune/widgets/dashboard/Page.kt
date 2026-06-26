package com.github.tidetune.widgets.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.icon_cloud
import tidetune.shared.generated.resources.icon_plus
import tidetune.shared.generated.resources.icon_timelapse
import tidetune.shared.generated.resources.dashboard_devices
import tidetune.shared.generated.resources.dashboard_devices_add
import tidetune.shared.generated.resources.dashboard_import_cancel
import tidetune.shared.generated.resources.dashboard_imports
import tidetune.shared.generated.resources.dashboard_sleep_mode
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import com.github.tidetune.components.TideTuneIconButton
import com.github.tidetune.components.TideTuneIconButtonSize
import com.github.tidetune.components.TideTuneIconButtonType
import com.github.tidetune.viewmodels.EditStorageVM
import com.github.tidetune.viewmodels.ImportStatusVM
import com.github.tidetune.viewmodels.SleepModeLeftTime
import com.github.tidetune.viewmodels.SleepModeVM
import com.github.tidetune.viewmodels.StoragesVM
import com.github.tidetune.core.LocalNavController
import com.github.tidetune.core.RouteAddDevices
import com.github.tidetune.platform.currentTimeMillis
import com.github.tidetune.singleton.ImportStatusItem
import uniffi.tidetune_core.Storage
import uniffi.tidetune_core.StorageType

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
private fun SleepModeBlock(vm: SleepModeVM = koinViewModel()) {
    val state by vm.state.collectAsState()
    val blockBg = if (state.enabled) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = if (state.enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    var leftTime by remember {
        mutableStateOf(SleepModeLeftTime(state.expiredMs - currentTimeMillis()))
    }

    LaunchedEffect(state.expiredMs, state.enabled) {
        while (true) {
            leftTime = SleepModeLeftTime(state.expiredMs - currentTimeMillis())

            if (!state.enabled) {
                break
            }
            kotlinx.coroutines.delay(1_000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(paddingX, 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                vm.openModal(leftTime)
            },
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
                text = "${leftTime.hour.toString().padStart(2, '0')}:${leftTime.minute.toString().padStart(2, '0')}",
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
    importStatusVM: ImportStatusVM = koinViewModel(),
) {
    val jobs by importStatusVM.recentJobs.collectAsState()

    if (jobs.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(paddingX, paddingY),
    ) {
        jobs.forEach { job ->
            ImportJobRow(
                job = job,
                onCancel = { importStatusVM.cancel(job.id) },
            )
        }
    }
}

@Composable
private fun ImportJobRow(
    job: ImportStatusItem,
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
                text = job.status,
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
private fun ColumnScope.DevicesBlock(
    storageItems: List<Storage>,
    editStoragesVM: EditStorageVM = koinViewModel()
) {
    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .weight(1f)
            .padding(paddingX, paddingY)
    ) {
        if (storageItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        navController.navigate(RouteAddDevices((-1).toString()))
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        modifier = Modifier.size(12.dp),
                        painter = painterResource(Res.drawable.icon_plus),
                        contentDescription = null
                    )
                    Box(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(Res.string.dashboard_devices_add),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }
        for (item in storageItems) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(RouteAddDevices(item.id.value.toString()))
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val title = item.alias.ifBlank {
                    item.addr
                }
                val subTitle = item.addr

                Box(modifier = Modifier.height(48.dp))
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(Res.drawable.icon_cloud),
                    contentDescription = null
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subTitle.isNotBlank()) {
                        Text(
                            text = subTitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSubpage(
    storageVM: StoragesVM = koinViewModel(),
    editStoragesVM: EditStorageVM = koinViewModel()
) {
    val navController = LocalNavController.current
    val storages by storageVM.storages.collectAsState()
    val storageItems = storages.filter { v -> v.typ != StorageType.LOCAL }

    LaunchedEffect(Unit) {
        storageVM.reload()
    }

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
        SleepModeBlock()
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
        ) {
            Title(title = stringResource(Res.string.dashboard_imports))
        }
        ImportStatusBlock()
        Box(modifier = Modifier.height(48.dp))
        Row(
            modifier = Modifier
                .padding(paddingX, 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Title(title = stringResource(Res.string.dashboard_devices))
            if (storageItems.isNotEmpty()) {
                TideTuneIconButton(
                    sizeType = TideTuneIconButtonSize.Small,
                    buttonType = TideTuneIconButtonType.Primary,
                    painter = painterResource(Res.drawable.icon_plus),
                    onClick = {
                        navController.navigate(RouteAddDevices((-1).toString()))
                    }
                )
            }
        }
        DevicesBlock(storageItems)
    }
}

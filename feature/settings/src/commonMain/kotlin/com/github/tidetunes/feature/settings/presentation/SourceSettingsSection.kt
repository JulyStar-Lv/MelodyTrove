package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.tidetunes.core.domain.model.DEFAULT_IGNORED_SOURCE_DIRECTORIES
import com.github.tidetunes.core.domain.model.MIN_SCANNED_AUDIO_DURATION_MS
import com.github.tidetunes.core.domain.model.SUPPORTED_AUDIO_EXTENSIONS
import com.github.tidetunes.core.domain.model.SourceConnectionTestStatus
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import com.github.tidetunes.service.librarysync.domain.LibrarySyncFailure
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SourceSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings

    SettingsPageLayout(title = "音源", onBack = onBack) {
        SettingsSection(title = "本地音乐") {
            SettingsSwitchRow(
                title = "本地音乐",
                summary = "关闭后隐藏本地音乐，不删除扫描记录",
                checked = settings.localMusicEnabled,
                onCheckedChange = { onAction(SettingsAction.SetLocalMusicEnabled(it)) },
            )
            if (state.localDirectories.isEmpty()) {
                SettingsInfoRow(title = "音乐目录", value = "未添加目录")
            } else {
                state.localDirectories.forEach { directory ->
                    SettingsInfoRow(
                        title = directory.displayName.ifBlank { "音乐目录" },
                        value = directory.path,
                        enabled = settings.localMusicEnabled,
                        onClick = {
                            onAction(
                                SettingsAction.RequestRemoveLocalDirectory(
                                    id = directory.id,
                                    title = directory.displayName.ifBlank { directory.path },
                                )
                            )
                        },
                    )
                }
            }
            SettingsInfoRow(
                title = "添加目录",
                value = "选择本地音乐文件夹",
                enabled = settings.localMusicEnabled,
                onClick = { onAction(SettingsAction.RequestAddLocalDirectory) },
            )
            SettingsInfoRow(
                title = "扫描本地",
                value = if (state.localDirectories.isEmpty()) "请先添加目录" else "扫描所有本地目录",
                enabled = settings.localMusicEnabled && state.localDirectories.isNotEmpty(),
                onClick = { onAction(SettingsAction.ScanLocalMusic) },
            )
            SettingsSwitchRow(
                title = "扫描子目录",
                summary = "递归扫描目录下的子文件夹",
                checked = settings.localScanSubdirectories,
                enabled = settings.localMusicEnabled,
                onCheckedChange = { onAction(SettingsAction.SetLocalScanSubdirectories(it)) },
            )
            SettingsSwitchRow(
                title = "忽略短音频",
                summary = "忽略 ${MIN_SCANNED_AUDIO_DURATION_MS / 1000} 秒以下音频",
                checked = settings.ignoreShortAudio,
                enabled = settings.localMusicEnabled,
                onCheckedChange = { onAction(SettingsAction.SetIgnoreShortAudio(it)) },
            )
        }

        SettingsSection(title = "WebDAV") {
            SettingsSwitchRow(
                title = "WebDAV",
                summary = "关闭后隐藏 WebDAV 音乐，不删除账号或远程文件",
                checked = settings.webDavEnabled,
                onCheckedChange = { onAction(SettingsAction.SetWebDavEnabled(it)) },
            )
            if (state.webDavAccounts.isEmpty()) {
                SettingsInfoRow(title = "账号", value = "未添加 WebDAV 账号")
            } else {
                state.webDavAccounts.forEach { account ->
                    SettingsInfoRow(
                        title = account.title,
                        value = "根目录 ${account.rootPath} · ${account.subtitle}",
                        enabled = settings.webDavEnabled,
                        onClick = { onAction(SettingsAction.OpenEditWebDavDialog(account.accountId)) },
                    )
                    SettingsInfoRow(
                        title = "扫描 ${account.title}",
                        value = "扫描根目录 ${account.rootPath}",
                        enabled = settings.webDavEnabled,
                        onClick = { onAction(SettingsAction.ScanWebDavAccount(account.accountId)) },
                    )
                }
            }
            SettingsInfoRow(
                title = "添加账号",
                value = "添加 WebDAV 音乐源",
                enabled = settings.webDavEnabled,
                onClick = { onAction(SettingsAction.OpenAddWebDavDialog) },
            )
            SettingsSwitchRow(
                title = "扫描子目录",
                summary = "递归扫描 WebDAV 子文件夹",
                checked = settings.webDavScanSubdirectories,
                enabled = settings.webDavEnabled,
                onCheckedChange = { onAction(SettingsAction.SetWebDavScanSubdirectories(it)) },
            )
        }

        SettingsSection(title = "扫描状态") {
            val latestTask = state.scanTasks.firstOrNull()
            if (latestTask == null) {
                SettingsInfoRow(title = "状态", value = "暂无扫描记录")
            } else {
                SettingsInfoRow(
                    title = latestTask.folderDisplayPath,
                    value = latestTask.statusSummary(),
                    onClick = if (latestTask.status.isActiveInSettings()) {
                        { onAction(SettingsAction.CancelScan(latestTask.id)) }
                    } else {
                        null
                    },
                )
                if (latestTask.failedCount > 0) {
                    SettingsInfoRow(
                        title = "失败原因",
                        value = "查看 ${latestTask.failedCount} 条失败原因",
                        onClick = { onAction(SettingsAction.OpenScanFailures(latestTask.id)) },
                    )
                } else {
                    latestTask.errorMessage?.let { message ->
                        SettingsInfoRow(title = "失败原因", value = message)
                    }
                }
            }
        }

        SettingsSection(title = "扫描规则") {
            SettingsInfoRow(title = "支持格式", value = SUPPORTED_AUDIO_EXTENSIONS.joinToString(", "))
            SettingsInfoRow(title = "隐藏文件", value = "默认忽略")
            SettingsInfoRow(title = "忽略目录", value = DEFAULT_IGNORED_SOURCE_DIRECTORIES.joinToString(", "))
        }
    }

    WebDavAccountDialog(
        state = state,
        dialog = state.webDavDialog,
        onAction = onAction,
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation is SettingsConfirmation.RemoveLocalDirectory,
        title = "移除音乐目录",
        message = "仅移除这个音乐源目录和对应扫描结果，不会删除本地文件。",
        confirmText = "确认移除",
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    SettingsConfirmDialog(
        show = state.pendingConfirmation is SettingsConfirmation.DeleteWebDavAccount,
        title = "删除 WebDAV 账号",
        message = "将删除账号和本地扫描结果，不会删除 WebDAV 服务器上的文件。",
        confirmText = "确认删除",
        onConfirm = { onAction(SettingsAction.ConfirmPendingAction) },
        onDismiss = { onAction(SettingsAction.DismissConfirmation) },
    )
    ScanFailureDialog(
        show = state.failureDialogTaskId != null,
        failures = state.failureDetails,
        onDismiss = { onAction(SettingsAction.DismissScanFailures) },
    )
}

@Composable
private fun ScanFailureDialog(
    show: Boolean,
    failures: List<LibrarySyncFailure>,
    onDismiss: () -> Unit,
) {
    if (!show) return

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "失败原因",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (failures.isEmpty()) {
                Text(
                    text = "暂无失败明细，重新扫描后会记录每条失败原因。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                failures.forEach { failure ->
                    ScanFailureItem(failure = failure.toScanFailureDisplay())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TideTunesTextButton(
                    text = "关闭",
                    type = TideTunesTextButtonType.Default,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ScanFailureItem(failure: ScanFailureDisplay) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "文件：${failure.fileName}",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
        )
        failure.directory?.let { directory ->
            Text(
                text = "位置：$directory",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Text(
            text = "原因：${failure.reason}",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun WebDavAccountDialog(
    state: SettingsUiState,
    dialog: WebDavAccountDialogState?,
    onAction: (SettingsAction) -> Unit,
) {
    if (dialog == null) return

    Dialog(onDismissRequest = { onAction(SettingsAction.DismissWebDavDialog) }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (dialog.isEditing) "编辑 WebDAV" else "添加 WebDAV",
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppTextField(
                value = dialog.name,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogName(it)) },
                label = "名称（可选）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = dialog.serverUrl,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogServerUrl(it)) },
                label = "服务器 URL",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = dialog.username,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogUsername(it)) },
                label = "用户名（可选）",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = dialog.password,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogPassword(it)) },
                label = if (dialog.isEditing) "密码（留空表示不修改）" else "密码（可选）",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppTextField(
                value = dialog.rootPath,
                onValueChange = { onAction(SettingsAction.SetWebDavDialogRootPath(it)) },
                label = "根目录",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.webDavConnectionTestMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body2,
                    color = if (state.webDavConnectionTestStatus == SourceConnectionTestStatus.Error) {
                        MiuixTheme.colorScheme.error
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (dialog.isEditing && dialog.accountId != null) {
                    TideTunesTextButton(
                        text = "删除",
                        type = TideTunesTextButtonType.Error,
                        size = TideTunesTextButtonSize.Medium,
                        onClick = {
                            onAction(
                                SettingsAction.RequestDeleteWebDavAccount(
                                    accountId = dialog.accountId,
                                    title = dialog.name.ifBlank { dialog.serverUrl },
                                )
                            )
                        },
                    )
                }
                TideTunesTextButton(
                    text = "取消",
                    type = TideTunesTextButtonType.Default,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.DismissWebDavDialog) },
                )
                TideTunesTextButton(
                    text = "测试",
                    type = TideTunesTextButtonType.Default,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.TestWebDavConnection) },
                )
                TideTunesTextButton(
                    text = if (state.sourceOperationInProgress) "保存中" else "保存",
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                    onClick = { onAction(SettingsAction.SaveWebDavAccount) },
                )
            }
        }
    }
}

private fun LibrarySyncTask.statusSummary(): String {
    val statusText = when (status) {
        LibrarySyncStatus.Queued -> "等待中"
        LibrarySyncStatus.Running -> "扫描中"
        LibrarySyncStatus.Paused -> "已暂停"
        LibrarySyncStatus.Completed -> "已完成"
        LibrarySyncStatus.CompletedWithErrors -> "已完成，有错误"
        LibrarySyncStatus.Failed -> "失败"
        LibrarySyncStatus.Cancelled -> "已取消"
        LibrarySyncStatus.Unknown -> "未知"
    }
    val actionHint = if (status.isActiveInSettings()) " · 点按取消" else ""
    return "$statusText · 总数：$scannedCount，导入：$importedCount，失败：$failedCount$actionHint"
}

private fun LibrarySyncStatus.isActiveInSettings(): Boolean {
    return this == LibrarySyncStatus.Queued ||
        this == LibrarySyncStatus.Running ||
        this == LibrarySyncStatus.Paused
}

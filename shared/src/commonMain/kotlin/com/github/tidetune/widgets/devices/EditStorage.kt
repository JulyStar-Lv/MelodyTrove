package com.github.tidetune.widgets.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.icon_back
import tidetune.shared.generated.resources.icon_cloud
import tidetune.shared.generated.resources.icon_deleteseep
import tidetune.shared.generated.resources.icon_ok
import tidetune.shared.generated.resources.icon_wifitethering
import tidetune.shared.generated.resources.storage_edit_addr
import tidetune.shared.generated.resources.storage_edit_alias
import tidetune.shared.generated.resources.storage_edit_anonymous
import tidetune.shared.generated.resources.storage_edit_form_address
import tidetune.shared.generated.resources.storage_edit_form_password
import tidetune.shared.generated.resources.storage_edit_form_username
import tidetune.shared.generated.resources.storage_edit_import_library_action
import tidetune.shared.generated.resources.storage_edit_import_library_label
import tidetune.shared.generated.resources.storage_edit_oauth
import tidetune.shared.generated.resources.storage_edit_onedrive_alias_not_empty
import tidetune.shared.generated.resources.storage_edit_onedrive_connect
import tidetune.shared.generated.resources.storage_edit_onedrive_disconnect
import tidetune.shared.generated.resources.storage_edit_onedrive_drive
import tidetune.shared.generated.resources.storage_edit_onedrive_drive_loading
import tidetune.shared.generated.resources.storage_edit_onedrive_drive_required
import tidetune.shared.generated.resources.storage_edit_onedrive_should_auth
import tidetune.shared.generated.resources.storage_edit_password
import tidetune.shared.generated.resources.storage_edit_username
import tidetune.shared.generated.resources.storage_remove_desc_count
import tidetune.shared.generated.resources.storage_remove_desc_main
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetune.components.ConfirmDialog
import com.github.tidetune.components.TideTuneIconButton
import com.github.tidetune.components.TideTuneIconButtonColors
import com.github.tidetune.components.TideTuneIconButtonSize
import com.github.tidetune.components.TideTuneIconButtonType
import com.github.tidetune.components.TideTuneTextButton
import com.github.tidetune.components.TideTuneTextButtonSize
import com.github.tidetune.components.TideTuneTextButtonType
import com.github.tidetune.components.FormSwitch
import com.github.tidetune.components.FormText
import com.github.tidetune.components.FormWidget
import com.github.tidetune.viewmodels.EditStorageVM
import com.github.tidetune.core.LocalNavController
import com.github.tidetune.core.RouteImport
import com.github.tidetune.singleton.RouteImportType
import kotlinx.coroutines.flow.update
import uniffi.tidetune_core.StorageConnectionTestResult
import uniffi.tidetune_core.StorageType
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import uniffi.tidetune_core.ArgUpsertStorage


private fun buildStr(s: String): AnnotatedString {
    val spans = s.split("$$")

    return buildAnnotatedString {
        for (s in spans) {
            if (s.startsWith("B__")) {
                val s = s.slice("B__".length until s.length)

                withStyle(style = SpanStyle(
                    fontWeight = FontWeight(700)
                )) {
                    append(s)
                }
            } else {
                append(s)
            }
        }
    }
}

@Composable
private fun RemoveDialog(
    editStorageVM: EditStorageVM = koinViewModel()
) {
    val navController = LocalNavController.current
    val title by editStorageVM.title.collectAsState()
    val musicCount by editStorageVM.musicCount.collectAsState()
    val isOpen by editStorageVM.removeModalOpen.collectAsState()

    val mainDesc = buildStr(
        stringResource(Res.string.storage_remove_desc_main)
            .replace("E_TITLE", title)
    )
    val countDesc = buildStr(
        stringResource(Res.string.storage_remove_desc_count)
            .replace("E_MCNT", musicCount.toString())
    )

    ConfirmDialog(
        open = isOpen,
        onConfirm = {
            editStorageVM.closeRemoveModal()
            editStorageVM.remove()
            navController.popBackStack()
        },
        onCancel = {
            editStorageVM.closeRemoveModal()
        },
    ) {
        Text(
            text = mainDesc,
            fontSize = 14.sp
        )
        Text(
            text = countDesc,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun StorageBlock(
    title: String,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (isActive) { MaterialTheme.colorScheme.primary } else { MaterialTheme.colorScheme.surfaceVariant }
    val tint = if (isActive) { MaterialTheme.colorScheme.surface } else { MaterialTheme.colorScheme.onSurface }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onSelect() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_cloud),
                contentDescription = null,
                tint = tint,
            )
            Text(
                text = title,
                color = tint,
            )
        }
    }
}


@Composable
private fun WebdavConfig(
    editStorageVM: EditStorageVM = koinViewModel()
) {
    val form by editStorageVM.form.collectAsState()
    val validated by editStorageVM.validated.collectAsState()
    val isAnonymous = form.isAnonymous;

    FormSwitch(
        label = stringResource(Res.string.storage_edit_anonymous),
        value = isAnonymous,
        onChange = { editStorageVM.updateForm { storage ->
            storage.isAnonymous = !storage.isAnonymous
            storage
        }}
    )
    FormText(
        label = stringResource(Res.string.storage_edit_alias),
        value = form.alias,
        onChange = { value -> editStorageVM.updateForm { storage ->
            storage.alias = value
            storage
        } },
    )
    FormText(
        label = stringResource(Res.string.storage_edit_addr),
        value = form.addr,
        onChange = { value -> editStorageVM.updateForm { storage ->
            storage.addr = value
            storage
        } },
        error = if (validated.addrEmpty) {
            Res.string.storage_edit_form_address
        } else {
            null
        }
    )
    if (!isAnonymous) {
        FormText(
            label = stringResource(Res.string.storage_edit_username),
            value = form.username,
            onChange = { value -> editStorageVM.updateForm { storage ->
                storage.username = value
                storage
            } },
            error = if (validated.usernameEmpty) {
                Res.string.storage_edit_form_username
            } else {
                null
            }
        )
        FormText(
            label = stringResource(Res.string.storage_edit_password),
            value = form.password,
            isPassword = true,
            onChange = { value -> editStorageVM.updateForm { storage ->
                storage.password = value
                storage
            } },
            error = if (validated.passwordEmpty) {
                Res.string.storage_edit_form_password
            } else {
                null
            }
        )
    }
}

@Composable
private fun OneDriveConfig(
    editStorageVM: EditStorageVM = koinViewModel()
) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val form by editStorageVM.form.collectAsState()
    val validated by editStorageVM.validated.collectAsState()
    val drives by editStorageVM.oneDriveDrives.collectAsState()
    val drivesLoading by editStorageVM.oneDriveDrivesLoading.collectAsState()
    val connected = form.password.isNotEmpty()

    FormText(
        label = stringResource(Res.string.storage_edit_alias),
        value = form.alias,
        onChange = { value -> editStorageVM.updateForm { storage ->
            storage.alias = value
            storage
        } },
        error = if (validated.aliasEmpty) {
            Res.string.storage_edit_onedrive_alias_not_empty
        } else {
            null
        }
    )
    FormWidget(
        label = stringResource(Res.string.storage_edit_oauth)
    ) {
        if (!connected) {
            TideTuneTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_connect),
                type = TideTuneTextButtonType.PrimaryVariant,
                size = TideTuneTextButtonSize.Medium,
                onClick = {
                    coroutineScope.launch {
                        uriHandler.openUri(editStorageVM.startOneDriveOAuth())
                    }
                },
            )
            if (validated.passwordEmpty) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 0.dp,
                        vertical = 2.dp,
                    ),
                    text = stringResource(Res.string.storage_edit_onedrive_should_auth),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                )
            }
        }
        if (connected) {
            TideTuneTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_disconnect),
                type = TideTuneTextButtonType.Error,
                size = TideTuneTextButtonSize.Medium,
                onClick = {
                    editStorageVM.disconnectOneDrive()
                },
            )
        }
    }
    if (connected) {
        FormWidget(
            label = stringResource(Res.string.storage_edit_onedrive_drive)
        ) {
            Column {
                if (drivesLoading) {
                    Text(
                        text = stringResource(Res.string.storage_edit_onedrive_drive_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                drives.forEach { drive ->
                    val selected = drive.id == form.addr
                    TideTuneTextButton(
                        text = if (selected) "✓ ${drive.name}" else drive.name,
                        type = if (selected) {
                            TideTuneTextButtonType.Primary
                        } else {
                            TideTuneTextButtonType.Default
                        },
                        size = TideTuneTextButtonSize.Medium,
                        onClick = { editStorageVM.selectOneDriveDrive(drive) },
                    )
                }
                if (validated.addrEmpty) {
                    Text(
                        text = stringResource(Res.string.storage_edit_onedrive_drive_required),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun EditStoragesPage(
    editStorageVM: EditStorageVM = koinViewModel()
) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val form by editStorageVM.form.collectAsState();
    val isCreated by editStorageVM.isCreated.collectAsState();
    val testing by editStorageVM.testResult.collectAsState()

    val storageType = form.typ;

    val testingColors = when (testing) {
        StorageConnectionTestResult.NONE -> null
        StorageConnectionTestResult.TESTING -> TideTuneIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MaterialTheme.colorScheme.tertiary,
        )
        StorageConnectionTestResult.SUCCESS -> TideTuneIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MaterialTheme.colorScheme.primary,
        )
        else -> TideTuneIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MaterialTheme.colorScheme.error,
        )
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row {
                TideTuneIconButton(
                    sizeType = TideTuneIconButtonSize.Medium,
                    buttonType = TideTuneIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_back),
                    onClick = {
                        navController.popBackStack()
                    }
                )
            }
            Row {
                if (!isCreated) {
                    TideTuneIconButton(
                        sizeType = TideTuneIconButtonSize.Medium,
                        buttonType = TideTuneIconButtonType.Error,
                        painter = painterResource(Res.drawable.icon_deleteseep),
                        onClick = {
                            editStorageVM.openRemoveModal()
                        }
                    )
                }
                TideTuneIconButton(
                    sizeType = TideTuneIconButtonSize.Medium,
                    buttonType = TideTuneIconButtonType.Default,
                    disabled = testing == StorageConnectionTestResult.TESTING,
                    painter = painterResource(Res.drawable.icon_wifitethering),
                    overrideColors = testingColors,
                    onClick = {
                        editStorageVM.test()
                    }
                )
                TideTuneIconButton(
                    sizeType = TideTuneIconButtonSize.Medium,
                    buttonType = TideTuneIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_ok),
                    onClick = {
                        coroutineScope.launch {
                            val finished = editStorageVM.finish()
                            if (finished) {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(30.dp, 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StorageBlock(
                        title = "WebDAV",
                        isActive = storageType == StorageType.WEBDAV,
                        onSelect = {
                            editStorageVM.changeType(StorageType.WEBDAV)
                        }
                    )
                    StorageBlock(
                        title = "OneDrive",
                        isActive = storageType == StorageType.ONE_DRIVE,
                        onSelect = {
                            editStorageVM.changeType(StorageType.ONE_DRIVE)
                        }
                    )
                }
                Box(modifier = Modifier.height(30.dp))
                if (storageType == StorageType.WEBDAV) {
                    WebdavConfig()
                }
                if (storageType == StorageType.ONE_DRIVE) {
                    OneDriveConfig()
                }
                if (!isCreated) {
                    FormWidget(
                        label = stringResource(Res.string.storage_edit_import_library_label),
                    ) {
                        TideTuneTextButton(
                            text = stringResource(Res.string.storage_edit_import_library_action),
                            type = TideTuneTextButtonType.PrimaryVariant,
                            size = TideTuneTextButtonSize.Medium,
                            onClick = {
                                editStorageVM.prepareImportLibraryFolder()
                                navController.navigate(RouteImport(RouteImportType.LibraryFolder))
                            },
                        )
                    }
                }
            }
        }
    }
    RemoveDialog()
}

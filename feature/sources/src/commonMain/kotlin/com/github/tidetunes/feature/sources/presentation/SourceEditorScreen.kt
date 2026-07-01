package com.github.tidetunes.feature.sources.presentation

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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.ConfirmDialog
import com.github.tidetunes.core.presentation.components.FormSwitch
import com.github.tidetunes.core.presentation.components.FormText
import com.github.tidetunes.core.presentation.components.FormWidget
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonColors
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.presentation.components.TideTunesTextButton
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesTextButtonType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.sources.generated.resources.Res
import tidetunes.feature.sources.generated.resources.icon_back
import tidetunes.feature.sources.generated.resources.icon_cloud
import tidetunes.feature.sources.generated.resources.icon_deleteseep
import tidetunes.feature.sources.generated.resources.icon_ok
import tidetunes.feature.sources.generated.resources.icon_wifitethering
import tidetunes.feature.sources.generated.resources.storage_edit_addr
import tidetunes.feature.sources.generated.resources.storage_edit_alias
import tidetunes.feature.sources.generated.resources.storage_edit_anonymous
import tidetunes.feature.sources.generated.resources.storage_edit_form_address
import tidetunes.feature.sources.generated.resources.storage_edit_form_password
import tidetunes.feature.sources.generated.resources.storage_edit_form_username
import tidetunes.feature.sources.generated.resources.storage_edit_import_library_action
import tidetunes.feature.sources.generated.resources.storage_edit_import_library_label
import tidetunes.feature.sources.generated.resources.storage_edit_oauth
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_alias_not_empty
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_connect
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_disconnect
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_drive
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_drive_loading
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_drive_required
import tidetunes.feature.sources.generated.resources.storage_edit_onedrive_should_auth
import tidetunes.feature.sources.generated.resources.storage_edit_password
import tidetunes.feature.sources.generated.resources.storage_edit_username
import tidetunes.feature.sources.generated.resources.storage_remove_desc_count
import tidetunes.feature.sources.generated.resources.storage_remove_desc_main

private fun buildStr(s: String): AnnotatedString {
    val spans = s.split("$$")

    return buildAnnotatedString {
        for (span in spans) {
            if (span.startsWith("B__")) {
                withStyle(style = SpanStyle(fontWeight = FontWeight(700))) {
                    append(span.slice("B__".length until span.length))
                }
            } else {
                append(span)
            }
        }
    }
}

@Composable
private fun RemoveDialog(
    state: SourceEditorState,
    onAction: (SourceEditorAction) -> Unit,
) {
    val mainDesc = buildStr(
        stringResource(Res.string.storage_remove_desc_main)
            .replace("E_TITLE", state.title)
    )
    val countDesc = buildStr(
        stringResource(Res.string.storage_remove_desc_count)
            .replace("E_MCNT", state.musicCount.toString())
    )

    ConfirmDialog(
        open = state.removeDialogOpen,
        onConfirm = {
            onAction(SourceEditorAction.ConfirmRemove)
        },
        onCancel = {
            onAction(SourceEditorAction.CloseRemoveDialog)
        },
    ) {
        Text(
            text = mainDesc,
            fontSize = 14.sp,
        )
        Text(
            text = countDesc,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun StorageBlock(
    title: String,
    isActive: Boolean,
    onSelect: () -> Unit,
) {
    val bgColor = if (isActive) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    val tint = if (isActive) {
        MiuixTheme.colorScheme.surface
    } else {
        MiuixTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onSelect() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
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
private fun WebDavConfig(
    state: WebDavSourceEditorState,
    validation: SourceEditorValidation,
    onAction: (SourceEditorAction) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    FormSwitch(
        label = stringResource(Res.string.storage_edit_anonymous),
        value = state.isAnonymous,
        onChange = { value ->
            onAction(SourceEditorAction.WebDavAnonymousChanged(value))
        },
    )
    FormText(
        label = stringResource(Res.string.storage_edit_alias),
        value = state.alias,
        onChange = { value ->
            onAction(SourceEditorAction.WebDavAliasChanged(value))
        },
    )
    FormText(
        label = stringResource(Res.string.storage_edit_addr),
        value = state.address,
        onChange = { value ->
            onAction(SourceEditorAction.WebDavAddressChanged(value))
        },
        error = if (validation.addressEmpty) {
            Res.string.storage_edit_form_address
        } else {
            null
        },
    )
    if (!state.isAnonymous) {
        FormText(
            label = stringResource(Res.string.storage_edit_username),
            value = state.username,
            onChange = { value ->
                onAction(SourceEditorAction.WebDavUsernameChanged(value))
            },
            error = if (validation.usernameEmpty) {
                Res.string.storage_edit_form_username
            } else {
                null
            },
        )
        FormText(
            label = stringResource(Res.string.storage_edit_password),
            value = password,
            isPassword = true,
            onChange = { value ->
                password = value
                onAction(SourceEditorAction.WebDavPasswordChanged(value))
            },
            error = if (validation.passwordEmpty) {
                Res.string.storage_edit_form_password
            } else {
                null
            },
        )
    }
}

@Composable
private fun OneDriveConfig(
    state: OneDriveSourceEditorState,
    validation: SourceEditorValidation,
    onAction: (SourceEditorAction) -> Unit,
) {
    FormText(
        label = stringResource(Res.string.storage_edit_alias),
        value = state.alias,
        onChange = { value ->
            onAction(SourceEditorAction.OneDriveAliasChanged(value))
        },
        error = if (validation.aliasEmpty) {
            Res.string.storage_edit_onedrive_alias_not_empty
        } else {
            null
        },
    )
    FormWidget(
        label = stringResource(Res.string.storage_edit_oauth),
    ) {
        if (!state.connected) {
            TideTunesTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_connect),
                type = TideTunesTextButtonType.PrimaryVariant,
                size = TideTunesTextButtonSize.Medium,
                onClick = {
                    onAction(SourceEditorAction.ConnectOneDrive)
                },
            )
            if (validation.passwordEmpty) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 0.dp,
                        vertical = 2.dp,
                    ),
                    text = stringResource(Res.string.storage_edit_onedrive_should_auth),
                    color = MiuixTheme.colorScheme.error,
                    fontSize = 11.sp,
                )
            }
        }
        if (state.connected) {
            TideTunesTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_disconnect),
                type = TideTunesTextButtonType.Error,
                size = TideTunesTextButtonSize.Medium,
                onClick = {
                    onAction(SourceEditorAction.DisconnectOneDrive)
                },
            )
        }
    }
    if (state.connected) {
        FormWidget(
            label = stringResource(Res.string.storage_edit_onedrive_drive),
        ) {
            Column {
                if (state.drivesLoading) {
                    Text(
                        text = stringResource(Res.string.storage_edit_onedrive_drive_loading),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 12.sp,
                    )
                }
                state.drives.forEach { drive ->
                    val selected = drive.id == state.selectedDriveId
                    TideTunesTextButton(
                        text = drive.name,
                        type = if (selected) {
                            TideTunesTextButtonType.Primary
                        } else {
                            TideTunesTextButtonType.Default
                        },
                        size = TideTunesTextButtonSize.Medium,
                        onClick = {
                            onAction(SourceEditorAction.SelectOneDriveDrive(drive.id))
                        },
                    )
                }
                if (validation.addressEmpty) {
                    Text(
                        text = stringResource(Res.string.storage_edit_onedrive_drive_required),
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun SourceEditorScreen(
    state: SourceEditorState,
    onAction: (SourceEditorAction) -> Unit,
) {
    val storageType = state.storageType

    val testingColors = when (state.testStatus) {
        SourceConnectionTestStatus.None -> null
        SourceConnectionTestStatus.Testing -> TideTunesIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.onTertiaryContainer,
        )
        SourceConnectionTestStatus.Success -> TideTunesIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.primary,
        )
        SourceConnectionTestStatus.Error -> TideTunesIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.error,
        )
    }

    Column(
        modifier = Modifier
            .background(MiuixTheme.colorScheme.surface)
            .fillMaxSize(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
        ) {
            Row {
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_back),
                    onClick = {
                        onAction(SourceEditorAction.NavigateBack)
                    },
                )
            }
            Row {
                if (!state.isCreated) {
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Error,
                        painter = painterResource(Res.drawable.icon_deleteseep),
                        onClick = {
                            onAction(SourceEditorAction.OpenRemoveDialog)
                        },
                    )
                }
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    disabled = state.testStatus == SourceConnectionTestStatus.Testing,
                    painter = painterResource(Res.drawable.icon_wifitethering),
                    overrideColors = testingColors,
                    onClick = {
                        onAction(SourceEditorAction.TestConnection)
                    },
                )
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_ok),
                    onClick = {
                        onAction(SourceEditorAction.Save)
                    },
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(30.dp, 12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StorageBlock(
                        title = "WebDAV",
                        isActive = storageType == SourceEditorType.WebDav,
                        onSelect = {
                            onAction(SourceEditorAction.ChangeType(SourceEditorType.WebDav))
                        },
                    )
                    StorageBlock(
                        title = "OneDrive",
                        isActive = storageType == SourceEditorType.OneDrive,
                        onSelect = {
                            onAction(SourceEditorAction.ChangeType(SourceEditorType.OneDrive))
                        },
                    )
                }
                Box(modifier = Modifier.height(30.dp))
                if (storageType == SourceEditorType.WebDav) {
                    WebDavConfig(
                        state = state.webDav,
                        validation = state.validation,
                        onAction = onAction,
                    )
                }
                if (storageType == SourceEditorType.OneDrive) {
                    OneDriveConfig(
                        state = state.oneDrive,
                        validation = state.validation,
                        onAction = onAction,
                    )
                }
                if (!state.isCreated) {
                    FormWidget(
                        label = stringResource(Res.string.storage_edit_import_library_label),
                    ) {
                        TideTunesTextButton(
                            text = stringResource(Res.string.storage_edit_import_library_action),
                            type = TideTunesTextButtonType.PrimaryVariant,
                            size = TideTunesTextButtonSize.Medium,
                            onClick = {
                                onAction(SourceEditorAction.ImportLibraryFolder)
                            },
                        )
                    }
                }
            }
        }
    }
    RemoveDialog(
        state = state,
        onAction = onAction,
    )
}

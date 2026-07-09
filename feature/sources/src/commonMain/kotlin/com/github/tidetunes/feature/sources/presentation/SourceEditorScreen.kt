package com.github.tidetunes.feature.sources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.ConfirmDialog
import com.github.tidetunes.core.presentation.components.FormSwitch
import com.github.tidetunes.core.presentation.components.FormText
import com.github.tidetunes.core.presentation.components.FormWidget
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonColors
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
        )
        Text(
            text = countDesc,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun StorageBlock(
    title: String,
    isActive: Boolean,
    onSelect: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes
    val bgColor = if (isActive) {
        TideTunesBrand.Primary
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val tint = if (isActive) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    val borderColor = if (isActive) {
        TideTunesBrand.Primary.copy(alpha = 0.22f)
    } else {
        MiuixTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(shapes.lg))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(shapes.lg))
            .clickable { onSelect() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_cloud),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = title,
                color = tint,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            TideTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_connect),
                variant = TideTextButtonVariant.PrimaryFilled,
                size = TideTextButtonSize.Medium,
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
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
        if (state.connected) {
            TideTextButton(
                text = stringResource(Res.string.storage_edit_onedrive_disconnect),
                variant = TideTextButtonVariant.Error,
                size = TideTextButtonSize.Medium,
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
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
                state.drives.forEach { drive ->
                    val selected = drive.id == state.selectedDriveId
                    TideTextButton(
                        text = drive.name,
                        variant = if (selected) {
                            TideTextButtonVariant.Primary
                        } else {
                            TideTextButtonVariant.Default
                        },
                        size = TideTextButtonSize.Medium,
                        onClick = {
                            onAction(SourceEditorAction.SelectOneDriveDrive(drive.id))
                        },
                    )
                }
                if (validation.addressEmpty) {
                    Text(
                        text = stringResource(Res.string.storage_edit_onedrive_drive_required),
                        color = MiuixTheme.colorScheme.error,
                        style = MiuixTheme.textStyles.footnote1,
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
    val spacing = TideTunesTokens.spacing
    val shapes = TideTunesTokens.shapes

    val testingColors = when (state.testStatus) {
        SourceConnectionTestStatus.None -> null
        SourceConnectionTestStatus.Testing -> TideIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.onTertiaryContainer,
        )
        SourceConnectionTestStatus.Success -> TideIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.primary,
        )
        SourceConnectionTestStatus.Error -> TideIconButtonColors(
            buttonBg = Color.Transparent,
            iconTint = MiuixTheme.colorScheme.error,
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageMedium

        Column(
            modifier = Modifier
                .background(MiuixTheme.colorScheme.background)
                .fillMaxSize(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = 12.dp)
                    .fillMaxWidth(),
            ) {
                TideIconButton(
                    size = TideIconButtonSize.Medium,
                    variant = TideIconButtonVariant.Default,
                    painter = painterResource(Res.drawable.icon_back),
                    onClick = {
                        onAction(SourceEditorAction.NavigateBack)
                    },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.title.ifBlank { "Source" },
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (state.isCreated) "New source" else "Edit source",
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!state.isCreated) {
                        TideIconButton(
                            size = TideIconButtonSize.Medium,
                            variant = TideIconButtonVariant.Error,
                            painter = painterResource(Res.drawable.icon_deleteseep),
                            onClick = {
                                onAction(SourceEditorAction.OpenRemoveDialog)
                            },
                        )
                    }
                    TideIconButton(
                        size = TideIconButtonSize.Medium,
                        variant = TideIconButtonVariant.Default,
                        enabled = state.testStatus != SourceConnectionTestStatus.Testing,
                        painter = painterResource(Res.drawable.icon_wifitethering),
                        colors = testingColors,
                        onClick = {
                            onAction(SourceEditorAction.TestConnection)
                        },
                    )
                    TideIconButton(
                        size = TideIconButtonSize.Medium,
                        variant = TideIconButtonVariant.Default,
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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = horizontalPadding, vertical = 12.dp),
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
                    TideCardSurface(
                        cornerRadius = shapes.xl,
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
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
                                    TideTextButton(
                                        text = stringResource(Res.string.storage_edit_import_library_action),
                                        variant = TideTextButtonVariant.PrimaryFilled,
                                        size = TideTextButtonSize.Medium,
                                        onClick = {
                                            onAction(SourceEditorAction.ImportLibraryFolder)
                                        },
                                    )
                                }
                            }
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
}

package io.github.julystar.musicapp.plugin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.presentation.components.AppSwitch
import io.github.julystar.musicapp.core.presentation.components.DesignDialog
import io.github.julystar.musicapp.core.presentation.components.DesignPreferenceRow
import io.github.julystar.musicapp.core.presentation.components.DesignSettingsGroup
import io.github.julystar.musicapp.core.presentation.components.DesignStickyGlassActionBar
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.components.FormSwitch
import io.github.julystar.musicapp.core.presentation.components.FormText
import io.github.julystar.musicapp.core.presentation.components.LocalDesignBottomContentInset
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import io.github.julystar.musicapp.plugin.install.ManifestConfigField
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import musicapp.shared.generated.resources.Res
import musicapp.shared.generated.resources.plugins_automatic_lookup
import musicapp.shared.generated.resources.plugins_automatic_lookup_summary
import musicapp.shared.generated.resources.plugins_batch_lookup
import musicapp.shared.generated.resources.plugins_batch_lookup_summary
import musicapp.shared.generated.resources.plugins_cancel
import musicapp.shared.generated.resources.plugins_capabilities
import musicapp.shared.generated.resources.plugins_choose_option
import musicapp.shared.generated.resources.plugins_choose_zip
import musicapp.shared.generated.resources.plugins_clear_cache
import musicapp.shared.generated.resources.plugins_configuration
import musicapp.shared.generated.resources.plugins_configuration_fields
import musicapp.shared.generated.resources.plugins_configure
import musicapp.shared.generated.resources.plugins_confirm_uninstall
import musicapp.shared.generated.resources.plugins_confirm_uninstall_message
import musicapp.shared.generated.resources.plugins_count
import musicapp.shared.generated.resources.plugins_disabled
import musicapp.shared.generated.resources.plugins_empty
import musicapp.shared.generated.resources.plugins_empty_summary
import musicapp.shared.generated.resources.plugins_enabled
import musicapp.shared.generated.resources.plugins_import
import musicapp.shared.generated.resources.plugins_install
import musicapp.shared.generated.resources.plugins_install_success
import musicapp.shared.generated.resources.plugins_installing
import musicapp.shared.generated.resources.plugins_installed
import musicapp.shared.generated.resources.plugins_last_error
import musicapp.shared.generated.resources.plugins_manual_lookup
import musicapp.shared.generated.resources.plugins_manual_lookup_summary
import musicapp.shared.generated.resources.plugins_no_configuration
import musicapp.shared.generated.resources.plugins_no_installable
import musicapp.shared.generated.resources.plugins_operation_failed
import musicapp.shared.generated.resources.plugins_overview
import musicapp.shared.generated.resources.plugins_permissions
import musicapp.shared.generated.resources.plugins_required
import musicapp.shared.generated.resources.plugins_save
import musicapp.shared.generated.resources.plugins_selected_archive
import musicapp.shared.generated.resources.plugins_status
import musicapp.shared.generated.resources.plugins_title
import musicapp.shared.generated.resources.plugins_uninstall
import musicapp.shared.generated.resources.plugins_validation_failed
import musicapp.shared.generated.resources.plugins_version_author
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LocalizedPluginSettingsRoot(
    onBack: () -> Unit,
    manager: PluginManager = koinInject(),
) {
    val plugins by manager.plugins().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val bottomInset = LocalDesignBottomContentInset.current
    var busy by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf<String?>(null) }
    var selectedArchive by remember { mutableStateOf<PlatformFile?>(null) }
    var importState by remember { mutableStateOf(LocalizedPluginImportState.Idle) }
    var editingPluginId by remember { mutableStateOf<String?>(null) }
    var pendingUninstall by remember { mutableStateOf<PluginSummary?>(null) }

    val operationFailed = stringResource(Res.string.plugins_operation_failed)
    val noInstallable = stringResource(Res.string.plugins_no_installable)
    val validationFailedPattern = stringResource(Res.string.plugins_validation_failed, 0)

    fun runOperation(
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        if (busy) return
        scope.launch {
            busy = true
            operationError = null
            runCatching { block() }
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    operationError = error.message ?: operationFailed
                    onFailure()
                }
            busy = false
        }
    }

    val zipPicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip")),
    ) { file ->
        file ?: return@rememberFilePickerLauncher
        selectedArchive = file
        importState = LocalizedPluginImportState.Selected
        operationError = null
    }

    val editingPlugin = plugins.firstOrNull { it.id == editingPluginId }

    LocalizedPluginConfigurationDialog(
        plugin = editingPlugin,
        manager = manager,
        busy = busy,
        runOperation = ::runOperation,
        onDismiss = { editingPluginId = null },
    )
    LocalizedPluginRemovalDialog(
        plugin = pendingUninstall,
        busy = busy,
        onDismiss = { pendingUninstall = null },
        onConfirm = {
            val plugin = pendingUninstall ?: return@LocalizedPluginRemovalDialog
            pendingUninstall = null
            runOperation { manager.uninstall(plugin.id) }
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = DesignTokens.spacing.pageCompact,
                    top = DesignTokens.adaptive.compactHeaderHeight + 8.dp,
                    end = DesignTokens.spacing.pageCompact,
                    bottom = 16.dp + bottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DesignSettingsGroup(title = stringResource(Res.string.plugins_overview)) {
                DesignPreferenceRow(
                    title = stringResource(Res.string.plugins_title),
                    summary = stringResource(
                        Res.string.plugins_count,
                        plugins.size,
                        plugins.count { it.enabled },
                    ),
                    showDivider = false,
                )
            }

            DesignSettingsGroup(title = stringResource(Res.string.plugins_installed)) {
                if (plugins.isEmpty()) {
                    DesignPreferenceRow(
                        title = stringResource(Res.string.plugins_empty),
                        summary = stringResource(Res.string.plugins_empty_summary),
                        showDivider = false,
                    )
                } else {
                    plugins.forEachIndexed { index, plugin ->
                        LocalizedPluginRow(
                            plugin = plugin,
                            busy = busy,
                            showDivider = index != plugins.lastIndex,
                            onEnabledChange = { enabled ->
                                runOperation { manager.setEnabled(plugin.id, enabled) }
                            },
                            onConfigure = { editingPluginId = plugin.id },
                            onUninstall = { pendingUninstall = plugin },
                        )
                    }
                }
            }

            DesignSettingsGroup(title = stringResource(Res.string.plugins_import)) {
                DesignPreferenceRow(
                    title = stringResource(Res.string.plugins_choose_zip),
                    summary = selectedArchive?.let {
                        stringResource(Res.string.plugins_selected_archive, it.name)
                    } ?: stringResource(Res.string.plugins_empty_summary),
                    enabled = !busy,
                    onClick = { zipPicker.launch() },
                    showDivider = selectedArchive != null,
                )
                selectedArchive?.let { archive ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = archive.name,
                            color = MiuixTheme.colorScheme.onSurface,
                            style = MiuixTheme.textStyles.body2,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesignTextButton(
                                text = if (importState == LocalizedPluginImportState.Installing) {
                                    stringResource(Res.string.plugins_installing)
                                } else {
                                    stringResource(Res.string.plugins_install)
                                },
                                variant = DesignTextButtonVariant.PrimaryFilled,
                                size = DesignTextButtonSize.Medium,
                                enabled = !busy,
                                onClick = {
                                    importState = LocalizedPluginImportState.Installing
                                    runOperation(
                                        onSuccess = {
                                            selectedArchive = null
                                            importState = LocalizedPluginImportState.Success
                                        },
                                        onFailure = {
                                            importState = LocalizedPluginImportState.Selected
                                        },
                                    ) {
                                        val localZip = FileKit.cacheDir / "plugin-import.zip"
                                        try {
                                            localZip.write(archive)
                                            val result = manager.installFromZip(localZip.path)
                                            if (result.installed.isEmpty()) error(noInstallable)
                                            if (result.failed.isNotEmpty()) {
                                                error(
                                                    validationFailedPattern.replace(
                                                        "0",
                                                        result.failed.size.toString(),
                                                    ),
                                                )
                                            }
                                        } finally {
                                            localZip.delete()
                                        }
                                    }
                                },
                            )
                            DesignTextButton(
                                text = stringResource(Res.string.plugins_cancel),
                                variant = DesignTextButtonVariant.Default,
                                size = DesignTextButtonSize.Medium,
                                enabled = !busy,
                                onClick = {
                                    selectedArchive = null
                                    importState = LocalizedPluginImportState.Idle
                                },
                            )
                        }
                    }
                }
                if (importState == LocalizedPluginImportState.Success) {
                    DesignPreferenceRow(
                        title = stringResource(Res.string.plugins_install_success),
                        showDivider = false,
                    )
                }
            }

            operationError?.let { message ->
                DesignSettingsGroup(title = stringResource(Res.string.plugins_status)) {
                    DesignPreferenceRow(
                        title = stringResource(Res.string.plugins_operation_failed),
                        summary = message,
                        titleColor = MiuixTheme.colorScheme.error,
                        showDivider = false,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        DesignStickyGlassActionBar(
            title = stringResource(Res.string.plugins_title),
            collapseFraction = 1f,
            onNavigateBack = onBack,
            showBackButtonBackground = false,
            compactTitle = true,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LocalizedPluginRow(
    plugin: PluginSummary,
    busy: Boolean,
    showDivider: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onUninstall: () -> Unit,
) {
    Column {
        DesignPreferenceRow(
            title = plugin.name,
            summary = buildString {
                append(stringResource(Res.string.plugins_version_author, plugin.versionName, plugin.author))
                if (plugin.capabilities.isNotEmpty()) {
                    append("\n")
                    append(
                        stringResource(
                            Res.string.plugins_capabilities,
                            plugin.capabilities.joinToString(", "),
                        ),
                    )
                }
                plugin.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                    append("\n")
                    append(stringResource(Res.string.plugins_last_error, error))
                }
            },
            enabled = !busy,
            onClick = onConfigure,
            trailing = {
                AppSwitch(
                    checked = plugin.enabled,
                    enabled = !busy,
                    onCheckedChange = onEnabledChange,
                )
            },
            showDivider = true,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DesignTextButton(
                text = stringResource(Res.string.plugins_configure),
                variant = DesignTextButtonVariant.Primary,
                size = DesignTextButtonSize.Small,
                enabled = !busy,
                onClick = onConfigure,
            )
            DesignTextButton(
                text = stringResource(Res.string.plugins_uninstall),
                variant = DesignTextButtonVariant.Error,
                size = DesignTextButtonSize.Small,
                enabled = !busy,
                onClick = onUninstall,
            )
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.06f)),
            )
        }
    }
}

@Composable
private fun LocalizedPluginConfigurationDialog(
    plugin: PluginSummary?,
    manager: PluginManager,
    busy: Boolean,
    runOperation: (
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        block: suspend () -> Unit,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var values by remember(plugin?.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var automatic by remember(plugin?.id) { mutableStateOf(false) }
    var batch by remember(plugin?.id) { mutableStateOf(false) }

    LaunchedEffect(plugin?.id, plugin?.updatedAt) {
        plugin ?: return@LaunchedEffect
        values = manager.config(plugin.id)
        automatic = plugin.allowAutomaticLookup
        batch = plugin.allowBatchLookup
    }

    DesignDialog(
        show = plugin != null,
        onDismiss = onDismiss,
    ) {
        val selected = plugin ?: return@DesignDialog
        Text(
            text = stringResource(Res.string.plugins_configuration, selected.name),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.plugins_permissions),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            DesignPreferenceRow(
                title = stringResource(Res.string.plugins_manual_lookup),
                summary = stringResource(Res.string.plugins_manual_lookup_summary),
                trailing = {
                    AppSwitch(
                        checked = selected.allowManualLookup,
                        onCheckedChange = {},
                        enabled = false,
                    )
                },
            )
            DesignPreferenceRow(
                title = stringResource(Res.string.plugins_automatic_lookup),
                summary = stringResource(Res.string.plugins_automatic_lookup_summary),
                enabled = !busy,
                trailing = {
                    AppSwitch(
                        checked = automatic,
                        enabled = !busy,
                        onCheckedChange = { automatic = it },
                    )
                },
            )
            DesignPreferenceRow(
                title = stringResource(Res.string.plugins_batch_lookup),
                summary = stringResource(Res.string.plugins_batch_lookup_summary),
                enabled = !busy,
                trailing = {
                    AppSwitch(
                        checked = batch,
                        enabled = !busy,
                        onCheckedChange = { batch = it },
                    )
                },
                showDivider = false,
            )

            Text(
                text = stringResource(Res.string.plugins_configuration_fields),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            if (selected.configFields.isEmpty()) {
                Text(
                    text = stringResource(Res.string.plugins_no_configuration),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            } else {
                selected.configFields.forEach { field ->
                    LocalizedPluginConfigField(
                        field = field,
                        value = values[field.key].orEmpty(),
                        enabled = !busy,
                        onValueChange = { value -> values = values + (field.key to value) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DesignTextButton(
                    text = stringResource(Res.string.plugins_save),
                    variant = DesignTextButtonVariant.PrimaryFilled,
                    size = DesignTextButtonSize.Medium,
                    enabled = !busy,
                    onClick = {
                        runOperation(
                            { onDismiss() },
                            {},
                        ) {
                            manager.setLookupPermissions(
                                pluginId = selected.id,
                                allowManual = selected.allowManualLookup,
                                allowAutomatic = automatic,
                                allowBatch = batch,
                            )
                            selected.configFields
                                .filterNot { it.type.equals("markdown", ignoreCase = true) }
                                .forEach { field ->
                                    val value = values[field.key]
                                    manager.setConfig(
                                        pluginId = selected.id,
                                        key = field.key,
                                        value = value?.takeUnless { it.isBlank() && !field.required },
                                    )
                                }
                        }
                    },
                )
                DesignTextButton(
                    text = stringResource(Res.string.plugins_clear_cache),
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Medium,
                    enabled = !busy,
                    onClick = {
                        runOperation({}, {}) { manager.clearCache(selected.id) }
                    },
                )
                DesignTextButton(
                    text = stringResource(Res.string.plugins_cancel),
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Medium,
                    enabled = !busy,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun LocalizedPluginConfigField(
    field: ManifestConfigField,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val requiredSuffix = if (field.required) {
        " · ${stringResource(Res.string.plugins_required)}"
    } else {
        ""
    }
    when (field.type.lowercase()) {
        "markdown" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = field.title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                field.summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
        }

        "switch", "boolean" -> FormSwitch(
            label = field.title + requiredSuffix,
            value = value.toBooleanStrictOrNull() ?: false,
            onChange = { if (enabled) onValueChange(it.toString()) },
        )

        "dropdown", "select" -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = field.title + requiredSuffix,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                )
                if (field.options.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.plugins_choose_option),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        field.options.forEach { option ->
                            DesignTextButton(
                                text = option.label,
                                variant = if (value == option.value) {
                                    DesignTextButtonVariant.Primary
                                } else {
                                    DesignTextButtonVariant.Default
                                },
                                size = DesignTextButtonSize.Small,
                                enabled = enabled,
                                onClick = { onValueChange(option.value) },
                            )
                        }
                    }
                }
            }
        }

        else -> FormText(
            label = field.title + requiredSuffix,
            value = value,
            onChange = { if (enabled) onValueChange(it) },
            isPassword = field.type.equals("password", ignoreCase = true),
        )
    }
}

@Composable
private fun LocalizedPluginRemovalDialog(
    plugin: PluginSummary?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DesignDialog(
        show = plugin != null,
        onDismiss = onDismiss,
    ) {
        val selected = plugin ?: return@DesignDialog
        Text(
            text = stringResource(Res.string.plugins_confirm_uninstall),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.plugins_confirm_uninstall_message, selected.name),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignTextButton(
                text = stringResource(Res.string.plugins_uninstall),
                variant = DesignTextButtonVariant.Error,
                size = DesignTextButtonSize.Medium,
                enabled = !busy,
                onClick = onConfirm,
            )
            DesignTextButton(
                text = stringResource(Res.string.plugins_cancel),
                variant = DesignTextButtonVariant.Default,
                size = DesignTextButtonSize.Medium,
                enabled = !busy,
                onClick = onDismiss,
            )
        }
    }
}

private enum class LocalizedPluginImportState {
    Idle,
    Selected,
    Installing,
    Success,
}

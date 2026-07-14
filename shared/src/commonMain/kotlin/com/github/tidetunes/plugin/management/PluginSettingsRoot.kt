package com.github.tidetunes.plugin.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppSwitch
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.ConfirmDialog
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TidePreferenceRow
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.plugin.install.ManifestConfigField
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PluginSettingsRoot(
    onBack: () -> Unit,
    manager: PluginManager = koinInject(),
) {
    val plugins by manager.plugins().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val configValues = remember { mutableStateMapOf<String, Map<String, String>>() }
    var zipPath by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingUninstall by remember { mutableStateOf<PluginSummary?>(null) }

    LaunchedEffect(plugins.map { it.id to it.updatedAt }) {
        plugins.forEach { plugin ->
            configValues[plugin.id] = manager.config(plugin.id)
        }
        configValues.keys.toList()
            .filter { pluginId -> plugins.none { it.id == pluginId } }
            .forEach(configValues::remove)
    }

    fun runOperation(block: suspend () -> String) {
        if (busy) return
        scope.launch {
            busy = true
            status = runCatching { block() }
                .getOrElse { error -> error.message ?: "Plugin operation failed" }
            busy = false
        }
    }

    ConfirmDialog(
        open = pendingUninstall != null,
        onConfirm = {
            val plugin = pendingUninstall ?: return@ConfirmDialog
            pendingUninstall = null
            runOperation {
                manager.uninstall(plugin.id)
                "Uninstalled ${plugin.name}"
            }
        },
        onCancel = { pendingUninstall = null },
    ) {
        Text(
            text = "Uninstall ${pendingUninstall?.name.orEmpty()}? Plugin files, configuration, cache and private runtime context will be removed.",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = TideTunesTokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                TideChevron(direction = TideChevronDirection.Left)
            }
            Text(
                text = "Metadata plugins",
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TideTunesTokens.spacing.pageCompact, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TideSettingsGroup(title = "Import") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Enter a local Lyrico v3 plugin ZIP path. Import runs off the UI thread and validates the archive before replacing an installed version.",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    AppTextField(
                        value = zipPath,
                        onValueChange = { zipPath = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Plugin ZIP path",
                        enabled = !busy,
                        singleLine = true,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TideTextButton(
                            text = "Import ZIP",
                            variant = TideTextButtonVariant.PrimaryFilled,
                            size = TideTextButtonSize.Medium,
                            enabled = zipPath.isNotBlank() && !busy,
                            onClick = {
                                runOperation {
                                    val result = manager.installFromZip(zipPath.trim())
                                    val installed = result.installed.joinToString { it.name }
                                    if (result.failed.isEmpty()) {
                                        "Installed $installed"
                                    } else {
                                        "Installed $installed; ${result.failed.size} plugin entries failed validation"
                                    }
                                }
                            },
                        )
                    }
                }
            }

            status?.let { message ->
                TideSettingsGroup(title = "Status") {
                    TidePreferenceRow(
                        title = if (busy) "Working…" else "Last operation",
                        summary = message,
                        showDivider = false,
                    )
                }
            }

            if (plugins.isEmpty()) {
                TideSettingsGroup(title = "Installed plugins") {
                    TidePreferenceRow(
                        title = "No plugins installed",
                        summary = "Import a ZIP that follows Lyrico Plugin API v3.",
                        showDivider = false,
                    )
                }
            }

            plugins.forEach { plugin ->
                PluginCard(
                    plugin = plugin,
                    values = configValues[plugin.id].orEmpty(),
                    busy = busy,
                    onValuesChange = { values -> configValues[plugin.id] = values },
                    onEnabledChange = { enabled ->
                        runOperation {
                            manager.setEnabled(plugin.id, enabled)
                            "${plugin.name} ${if (enabled) "enabled" else "disabled"}"
                        }
                    },
                    onPermissionsChange = { manual, automatic, batch ->
                        runOperation {
                            manager.setLookupPermissions(
                                pluginId = plugin.id,
                                allowManual = manual,
                                allowAutomatic = automatic,
                                allowBatch = batch,
                            )
                            "Updated lookup permissions for ${plugin.name}"
                        }
                    },
                    onSaveConfig = {
                        val values = configValues[plugin.id].orEmpty()
                        runOperation {
                            plugin.configFields.forEach { field ->
                                val value = values[field.key]
                                manager.setConfig(
                                    pluginId = plugin.id,
                                    key = field.key,
                                    value = value?.takeUnless { it.isBlank() && !field.required },
                                )
                            }
                            "Saved configuration for ${plugin.name}"
                        }
                    },
                    onClearCache = {
                        runOperation {
                            manager.clearCache(plugin.id)
                            "Cleared cache for ${plugin.name}"
                        }
                    },
                    onUninstall = { pendingUninstall = plugin },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PluginCard(
    plugin: PluginSummary,
    values: Map<String, String>,
    busy: Boolean,
    onValuesChange: (Map<String, String>) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPermissionsChange: (Boolean, Boolean, Boolean) -> Unit,
    onSaveConfig: () -> Unit,
    onClearCache: () -> Unit,
    onUninstall: () -> Unit,
) {
    TideSettingsGroup(title = plugin.name) {
        TidePreferenceRow(
            title = "${plugin.name} ${plugin.versionName}",
            summary = listOfNotNull(
                plugin.author.takeIf(String::isNotBlank),
                plugin.description.takeIf(String::isNotBlank),
            ).joinToString(" · ").ifBlank { "Plugin ${plugin.id}" },
        )
        TidePreferenceRow(
            title = "Capabilities",
            summary = plugin.capabilities.joinToString().ifBlank { "No capabilities declared" },
        )
        TidePreferenceRow(
            title = "Enabled",
            summary = "Required before the plugin can participate in any lookup flow",
            enabled = !busy,
            onClick = { onEnabledChange(!plugin.enabled) },
            trailing = {
                AppSwitch(
                    checked = plugin.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !busy,
                )
            },
        )
        PermissionRow(
            title = "Manual lookup",
            checked = plugin.allowManualLookup,
            enabled = plugin.enabled && !busy,
            onChange = { value ->
                onPermissionsChange(value, plugin.allowAutomaticLookup, plugin.allowBatchLookup)
            },
        )
        PermissionRow(
            title = "Automatic lookup",
            checked = plugin.allowAutomaticLookup,
            enabled = plugin.enabled && !busy,
            onChange = { value ->
                onPermissionsChange(plugin.allowManualLookup, value, plugin.allowBatchLookup)
            },
        )
        PermissionRow(
            title = "Batch lookup",
            checked = plugin.allowBatchLookup,
            enabled = plugin.enabled && !busy,
            onChange = { value ->
                onPermissionsChange(plugin.allowManualLookup, plugin.allowAutomaticLookup, value)
            },
        )

        if (plugin.configFields.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Configuration",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                plugin.configFields.forEach { field ->
                    PluginConfigEditor(
                        field = field,
                        value = values[field.key].orEmpty(),
                        enabled = !busy,
                        onValueChange = { value ->
                            onValuesChange(values + (field.key to value))
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TideTextButton(
                        text = "Save configuration",
                        variant = TideTextButtonVariant.Primary,
                        size = TideTextButtonSize.Medium,
                        enabled = !busy,
                        onClick = onSaveConfig,
                    )
                }
            }
        }

        plugin.lastError?.let { error ->
            TidePreferenceRow(
                title = "Last plugin error",
                summary = error,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TideTextButton(
                text = "Clear cache",
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Medium,
                enabled = !busy,
                onClick = onClearCache,
            )
            TideTextButton(
                text = "Uninstall",
                variant = TideTextButtonVariant.Error,
                size = TideTextButtonSize.Medium,
                enabled = !busy,
                onClick = onUninstall,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    TidePreferenceRow(
        title = title,
        enabled = enabled,
        onClick = { onChange(!checked) },
        trailing = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun PluginConfigEditor(
    field: ManifestConfigField,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    when (field.type) {
        "boolean" -> TidePreferenceRow(
            title = field.title,
            summary = field.summary,
            enabled = enabled,
            onClick = { onValueChange((value.toBooleanStrictOrNull() != true).toString()) },
            trailing = {
                AppSwitch(
                    checked = value.toBooleanStrictOrNull() == true,
                    onCheckedChange = { onValueChange(it.toString()) },
                    enabled = enabled,
                )
            },
            showDivider = false,
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = field.title,
                enabled = enabled,
                singleLine = true,
                visualTransformation = if (field.type == "password") {
                    PasswordVisualTransformation()
                } else {
                    androidx.compose.ui.text.input.VisualTransformation.None
                },
            )
            field.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

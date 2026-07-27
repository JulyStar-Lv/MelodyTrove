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
import com.github.tidetunes.core.presentation.components.LocalTideBottomContentInset
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TidePreferenceRow
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import com.github.tidetunes.plugin.install.ManifestConfigField
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PluginSettingsRoot(
    onBack: () -> Unit,
    manager: PluginManager = koinInject(),
) {
    val plugins by manager.plugins().collectAsState(initial = emptyList())
    val bottomContentInset = LocalTideBottomContentInset.current
    val scope = rememberCoroutineScope()
    val configValues = remember { mutableStateMapOf<String, Map<String, String>>() }
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

    val zipPicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("zip")),
    ) { file ->
        file ?: return@rememberFilePickerLauncher
        runOperation {
            val localZip = FileKit.cacheDir / "plugin-import.zip"
            try {
                localZip.write(file)
                val result = manager.installFromZip(localZip.path)
                val installed = result.installed.joinToString { it.name }
                if (result.installed.isEmpty()) {
                    val reason = result.failed.firstOrNull()?.reason ?: "No installable plugin found in ZIP"
                    error(reason)
                } else if (result.failed.isEmpty()) {
                    "Installed $installed from ${file.name}"
                } else {
                    "Installed $installed; ${result.failed.size} plugin entries failed validation"
                }
            } finally {
                localZip.delete()
            }
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
                .padding(
                    start = TideTunesTokens.spacing.pageCompact,
                    top = 8.dp,
                    end = TideTunesTokens.spacing.pageCompact,
                    bottom = 8.dp + bottomContentInset,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TideSettingsGroup(title = "Import") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Choose a local Lyrico v3 plugin ZIP. Import runs off the UI thread and validates the archive before replacing an installed version.",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TideTextButton(
                            text = "Choose ZIP",
                            variant = TideTextButtonVariant.PrimaryFilled,
                            size = TideTextButtonSize.Medium,
                            enabled = !busy,
                            onClick = zipPicker::launch,
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
                        val missingRequired = plugin.configFields.firstOrNull { field ->
                            field.type != "markdown" &&
                                isPluginConfigFieldVisible(field, values) &&
                                field.required &&
                                values[field.key].isNullOrBlank()
                        }
                        if (missingRequired != null) {
                            status = "Required configuration is missing: ${missingRequired.title}"
                        } else {
                            runOperation {
                                plugin.configFields.filterNot { it.type == "markdown" }.forEach { field ->
                                    val value = values[field.key]
                                    manager.setConfig(
                                        pluginId = plugin.id,
                                        key = field.key,
                                        value = value?.takeUnless { it.isBlank() && !field.required },
                                    )
                                }
                                "Saved configuration for ${plugin.name}"
                            }
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
                plugin.configFields
                    .filter { field -> isPluginConfigFieldVisible(field, values) }
                    .forEach { field ->
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
        "switch", "boolean" -> TidePreferenceRow(
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
        "dropdown", "select" -> if (field.options.isEmpty()) {
            PluginTextConfigEditor(field, value, enabled, onValueChange)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = field.title,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                field.options.forEach { option ->
                    TidePreferenceRow(
                        title = option.label,
                        summary = option.summary ?: option.value,
                        enabled = enabled,
                        onClick = { onValueChange(option.value) },
                        trailing = {
                            if (value == option.value) {
                                Text(
                                    text = "Selected",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                            }
                        },
                        showDivider = false,
                    )
                }
                field.summary?.let { summary ->
                    ConfigSummary(summary)
                }
            }
        }
        "markdown" -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = field.title,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
            )
            field.defaultValue?.takeIf(String::isNotBlank)?.let { content ->
                Text(
                    text = content,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            field.summary?.let { summary -> ConfigSummary(summary) }
        }
        else -> PluginTextConfigEditor(field, value, enabled, onValueChange)
    }
}

@Composable
private fun PluginTextConfigEditor(
    field: ManifestConfigField,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AppTextField(
            value = value,
            onValueChange = { updated ->
                onValueChange(
                    if (field.type == "number") updated.filter(Char::isDigit) else updated,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            label = field.title,
            enabled = enabled,
            singleLine = field.type != "textarea",
            visualTransformation = if (field.type == "password") {
                PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
        )
        field.summary?.let { summary ->
            ConfigSummary(summary)
        }
    }
}

@Composable
private fun ConfigSummary(summary: String) {
    Text(
        text = summary,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

internal fun isPluginConfigFieldVisible(
    field: ManifestConfigField,
    values: Map<String, String>,
): Boolean = field.dependency?.matches(values, depth = 0) ?: true

private fun JsonObject.matches(
    values: Map<String, String>,
    depth: Int,
): Boolean {
    if (depth > 16) return false
    (this["match"] as? JsonObject)?.let { match ->
        val key = (match["key"] as? JsonPrimitive)?.contentOrNull ?: return false
        val expected = (match["value"] as? JsonPrimitive)?.contentOrNull ?: return false
        return values[key] == expected
    }
    (this["and"] as? JsonObject)?.let { and ->
        val conditions = and["conditions"] as? JsonArray ?: return false
        return conditions.all { condition ->
            (condition as? JsonObject)?.matches(values, depth + 1) == true
        }
    }
    (this["or"] as? JsonObject)?.let { or ->
        val conditions = or["conditions"] as? JsonArray ?: return false
        return conditions.any { condition ->
            (condition as? JsonObject)?.matches(values, depth + 1) == true
        }
    }
    (this["not"] as? JsonObject)?.let { not ->
        val condition = not["condition"] as? JsonObject ?: return false
        return !condition.matches(values, depth + 1)
    }
    return false
}

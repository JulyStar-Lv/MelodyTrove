package com.github.tidetunes.plugin.management

import com.github.tidetunes.database.PluginConfigEntity
import com.github.tidetunes.database.PluginDao
import com.github.tidetunes.database.PluginEntity
import com.github.tidetunes.plugin.currentTimeMillis
import com.github.tidetunes.plugin.install.ManifestConfigField
import com.github.tidetunes.plugin.install.ParsedManifest
import com.github.tidetunes.plugin.runtime.InstalledPlugin
import com.github.tidetunes.plugin.runtime.PluginConfigProvider
import com.github.tidetunes.plugin.runtime.PluginRuntimeDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okio.Path

data class PluginSummary(
    val id: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val author: String,
    val description: String,
    val capabilities: List<String>,
    val enabled: Boolean,
    val allowManualLookup: Boolean,
    val allowAutomaticLookup: Boolean,
    val allowBatchLookup: Boolean,
    val installedAt: Long,
    val updatedAt: Long,
    val entryFile: String,
    val includeDirs: List<String>,
    val iconPath: String?,
    val configFields: List<ManifestConfigField>,
    val lastError: String?,
    val lastErrorAt: Long?,
)

class PluginRepository(
    private val pluginDao: PluginDao,
    private val pluginsDir: Path,
) : PluginConfigProvider {
    private val json = Json { ignoreUnknownKeys = true }

    fun allPlugins(): Flow<List<PluginSummary>> = pluginDao.all().map { plugins ->
        plugins.map(PluginEntity::toSummary)
    }

    suspend fun allSnapshot(): List<PluginSummary> =
        pluginDao.allSnapshot().map(PluginEntity::toSummary)

    suspend fun getPlugin(pluginId: String): PluginSummary? =
        pluginDao.findByPluginId(pluginId)?.toSummary()

    override suspend fun config(pluginId: String): Map<String, String> =
        pluginDao.configsFor(pluginId).associate { it.configKey to it.configValue }

    suspend fun setConfig(
        pluginId: String,
        key: String,
        value: String?,
    ) {
        if (value == null) {
            pluginDao.deleteConfig(pluginId, key)
        } else {
            pluginDao.setConfig(
                PluginConfigEntity(
                    pluginId = pluginId,
                    configKey = key,
                    configValue = value,
                    updatedAt = currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun importPluginDefaults(manifest: ParsedManifest) {
        manifest.configFields
            .filter { it.defaultValue != null && it.defaultValue.isNotEmpty() }
            .forEach { field ->
                if (pluginDao.configValue(manifest.id, field.key) == null) {
                    setConfig(manifest.id, field.key, field.defaultValue)
                }
            }
    }

    suspend fun setEnabled(
        pluginId: String,
        enabled: Boolean,
    ) {
        pluginDao.setEnabled(pluginId, enabled)
    }

    suspend fun setLookupPermissions(
        pluginId: String,
        allowManual: Boolean,
        allowAutomatic: Boolean,
        allowBatch: Boolean,
    ) {
        pluginDao.setLookupPermissions(
            pluginId = pluginId,
            allowManual = allowManual,
            allowAutomatic = allowAutomatic,
            allowBatch = allowBatch,
        )
    }

    suspend fun recordError(
        pluginId: String,
        error: Throwable,
    ) {
        val message = error.message
            ?.take(2_000)
            ?.ifBlank { error::class.simpleName }
            ?: error::class.simpleName
            ?: "Plugin execution failed"
        pluginDao.setLastError(pluginId, message, currentTimeMillis())
    }

    suspend fun clearError(pluginId: String) {
        pluginDao.clearLastError(pluginId)
    }

    fun PluginSummary.toInstalledPlugin(): InstalledPlugin = InstalledPlugin(
        descriptor = PluginRuntimeDescriptor(
            pluginId = id,
            pluginName = name,
            pluginVersionCode = versionCode,
            pluginUpdatedAt = updatedAt,
            entryFile = entryFile,
            includeDirs = includeDirs,
            directory = (pluginsDir / id).toString(),
        ),
        capabilities = capabilities.toSet(),
        enabled = enabled,
        allowManualLookup = allowManualLookup,
        allowAutomaticLookup = allowAutomaticLookup,
        allowBatchLookup = allowBatchLookup,
    )

    private fun PluginEntity.toSummary(): PluginSummary = PluginSummary(
        id = pluginId,
        name = name,
        versionName = versionName,
        versionCode = versionCode,
        author = author,
        description = description,
        capabilities = decodeStringList(capabilitiesJson),
        enabled = enabled,
        allowManualLookup = allowManualLookup,
        allowAutomaticLookup = allowAutomaticLookup,
        allowBatchLookup = allowBatchLookup,
        installedAt = installedAt,
        updatedAt = updatedAt,
        entryFile = entryFile,
        includeDirs = decodeStringList(includeDirsJson),
        iconPath = iconPath,
        configFields = decodeConfigFields(manifestRawJson),
        lastError = lastError,
        lastErrorAt = lastErrorAt,
    )

    private fun decodeStringList(raw: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(raw)
    }.getOrDefault(emptyList())

    private fun decodeConfigFields(manifestRawJson: String): List<ManifestConfigField> = runCatching {
        val manifest = json.parseToJsonElement(manifestRawJson).jsonObject
        (manifest["configFields"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { field ->
                ManifestConfigField(
                    key = field["key"]?.jsonPrimitive?.content.orEmpty(),
                    title = field["title"]?.jsonPrimitive?.content.orEmpty(),
                    summary = field["summary"]?.jsonPrimitive?.contentOrNull,
                    group = field["group"]?.jsonPrimitive?.contentOrNull,
                    type = field["type"]?.jsonPrimitive?.content ?: "text",
                    required = field["required"]?.jsonPrimitive?.booleanOrNull == true,
                    defaultValue = field["defaultValue"]?.jsonPrimitive?.contentOrNull,
                    dependency = field["dependency"] as? JsonObject,
                )
            }
            .orEmpty()
    }.getOrDefault(emptyList())
}

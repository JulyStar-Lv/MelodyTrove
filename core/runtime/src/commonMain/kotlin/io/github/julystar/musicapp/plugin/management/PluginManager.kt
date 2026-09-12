package io.github.julystar.musicapp.plugin.management

import io.github.julystar.musicapp.plugin.install.PluginInstallResult
import io.github.julystar.musicapp.plugin.install.PluginInstaller
import io.github.julystar.musicapp.plugin.runtime.PluginResultParser
import io.github.julystar.musicapp.plugin.runtime.PluginRuntimeManager
import io.github.julystar.musicapp.plugin.runtime.PluginRuntimeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import uniffi.app_backend.clearPluginCache

/** Coordinates database, files, private context, cache and runtime lifecycle changes. */
class PluginManager(
    private val repository: PluginRepository,
    private val installer: PluginInstaller,
    private val runtimeManager: PluginRuntimeManager,
    private val resultParser: PluginResultParser,
    private val runtimeSettings: PluginRuntimeSettings,
) {
    fun plugins(): Flow<List<PluginSummary>> = repository.allPlugins()

    suspend fun installFromZip(zipPath: String): PluginInstallResult {
        val result = installer.installAllFromZip(zipPath.toPath())
        result.installed.forEach { manifest ->
            runtimeManager.invalidate(manifest.id)
            resultParser.clearPlugin(manifest.id)
            clearCacheInternal(manifest.id)
        }
        return result
    }

    suspend fun setEnabled(
        pluginId: String,
        enabled: Boolean,
    ) {
        if (!enabled) {
            runtimeManager.onDisabled(pluginId)
            resultParser.clearPlugin(pluginId)
        }
        repository.setEnabled(pluginId, enabled)
    }

    suspend fun setLookupPermissions(
        pluginId: String,
        allowManual: Boolean,
        allowAutomatic: Boolean,
        allowBatch: Boolean,
    ) {
        repository.setLookupPermissions(
            pluginId = pluginId,
            allowManual = allowManual,
            allowAutomatic = allowAutomatic,
            allowBatch = allowBatch,
        )
    }

    suspend fun setConfig(
        pluginId: String,
        key: String,
        value: String?,
    ) {
        repository.setConfig(pluginId, key, value)
    }

    suspend fun config(pluginId: String): Map<String, String> = repository.config(pluginId)

    suspend fun clearCache(pluginId: String) {
        runtimeManager.invalidate(pluginId)
        resultParser.clearPlugin(pluginId)
        clearCacheInternal(pluginId)
        repository.clearError(pluginId)
    }

    suspend fun uninstall(pluginId: String) {
        runtimeManager.onUninstalled(pluginId)
        resultParser.clearPlugin(pluginId)
        clearCacheInternal(pluginId)
        installer.uninstall(pluginId)
    }

    private suspend fun clearCacheInternal(pluginId: String) = withContext(Dispatchers.Default) {
        clearPluginCache(
            cacheDirectory = runtimeSettings.cacheDirectory,
            pluginId = pluginId,
        )
    }
}

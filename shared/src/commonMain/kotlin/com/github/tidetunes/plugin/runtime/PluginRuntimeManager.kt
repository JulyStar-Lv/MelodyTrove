package com.github.tidetunes.plugin.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PluginRuntimeManager(
    private val factory: PluginRuntimeFactory,
    private val bundleBuilder: PluginScriptBundleBuilder,
) {
    private data class Entry(val key: PluginRuntimeCacheKey, val runtime: PluginRuntime)

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    suspend fun runtime(plugin: PluginRuntimeDescriptor): PluginRuntime = mutex.withLock {
        val bundle = bundleBuilder.build(plugin)
        val key = PluginRuntimeCacheKey(
            plugin.pluginId,
            plugin.pluginVersionCode,
            plugin.pluginUpdatedAt,
            bundle.sourceHash,
        )
        entries[plugin.pluginId]?.takeIf { it.key == key }?.runtime ?: factory.create(plugin).also { runtime ->
            try {
                runtime.load(bundle)
            } catch (error: Throwable) {
                runtime.close()
                throw error
            }
            entries.remove(plugin.pluginId)?.runtime?.close()
            entries[plugin.pluginId] = Entry(key, runtime)
        }
    }

    suspend fun call(
        plugin: PluginRuntimeDescriptor,
        functionName: String,
        requestJson: String,
        timeoutMs: Long,
    ): String {
        val runtime = runtime(plugin)
        return try {
            runtime.call(functionName, requestJson, timeoutMs)
        } catch (error: PluginRuntimeError) {
            if (error.requiresRuntimeRebuild()) invalidate(plugin.pluginId)
            throw error
        }
    }

    suspend fun invalidate(pluginId: String) = mutex.withLock {
        entries.remove(pluginId)?.runtime?.close()
    }

    suspend fun onDisabled(pluginId: String) = invalidate(pluginId)

    suspend fun onUninstalled(pluginId: String) = invalidate(pluginId)

    suspend fun closeAll() = mutex.withLock {
        entries.values.forEach { it.runtime.close() }
        entries.clear()
    }
}

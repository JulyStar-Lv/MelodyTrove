package com.github.tidetunes.plugin.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PluginRuntimeManager(
    private val factory: PluginRuntimeFactory,
    private val bundleBuilder: PluginScriptBundleBuilder,
    private val loadTimeoutMs: Long = factory.settings.loadTimeoutMs,
) {
    private data class Entry(
        val key: PluginRuntimeCacheKey,
        val runtime: PluginRuntime,
    )

    private val stateMutex = Mutex()
    private val pluginLocks = mutableMapOf<String, Mutex>()
    private val entries = mutableMapOf<String, Entry>()
    private var closed = false

    suspend fun runtime(plugin: PluginRuntimeDescriptor): PluginRuntime {
        val bundle = bundleBuilder.build(plugin)
        val key = PluginRuntimeCacheKey(
            pluginId = plugin.pluginId,
            pluginVersionCode = plugin.pluginVersionCode,
            pluginUpdatedAt = plugin.pluginUpdatedAt,
            scriptSourceHash = bundle.sourceHash,
        )

        stateMutex.withLock {
            checkOpenLocked()
            entries[plugin.pluginId]
                ?.takeIf { it.key == key }
                ?.runtime
                ?.let { return it }
        }

        return lockFor(plugin.pluginId).withLock {
            stateMutex.withLock {
                checkOpenLocked()
                entries[plugin.pluginId]
                    ?.takeIf { it.key == key }
                    ?.runtime
                    ?.let { return@withLock it }
            }?.let { return@withLock it }

            val created = factory.create(plugin)
            try {
                created.load(bundle, loadTimeoutMs)
            } catch (throwable: Throwable) {
                created.close()
                throw throwable
            }

            val previous = stateMutex.withLock {
                if (closed) {
                    null
                } else {
                    entries.put(plugin.pluginId, Entry(key, created))?.runtime
                }
            }
            if (closed) {
                created.close()
                throw PluginRuntimeError.Closed("Plugin runtime manager is closed")
            }
            if (previous !== created) previous?.close()
            created
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
            if (error.requiresRuntimeRebuild()) {
                invalidateIfSame(plugin.pluginId, runtime)
            }
            throw error
        }
    }

    suspend fun invalidate(pluginId: String) {
        lockFor(pluginId).withLock {
            val runtime = stateMutex.withLock { entries.remove(pluginId)?.runtime }
            runtime?.cancelCurrentCall()
            runtime?.close()
        }
    }

    suspend fun onDisabled(pluginId: String) = invalidate(pluginId)

    suspend fun onUninstalled(pluginId: String) = invalidate(pluginId)

    suspend fun closeAll() {
        val runtimes = stateMutex.withLock {
            if (closed && entries.isEmpty()) return
            closed = true
            entries.values.map(Entry::runtime).also { entries.clear() }
        }
        runtimes.forEach(PluginRuntime::cancelCurrentCall)
        runtimes.forEach(PluginRuntime::close)
    }

    internal suspend fun cachedPluginIds(): Set<String> = stateMutex.withLock { entries.keys.toSet() }

    private suspend fun invalidateIfSame(
        pluginId: String,
        expected: PluginRuntime,
    ) {
        lockFor(pluginId).withLock {
            val removed = stateMutex.withLock {
                entries[pluginId]
                    ?.takeIf { it.runtime === expected }
                    ?.also { entries.remove(pluginId) }
                    ?.runtime
            }
            removed?.cancelCurrentCall()
            removed?.close()
        }
    }

    private suspend fun lockFor(pluginId: String): Mutex = stateMutex.withLock {
        pluginLocks.getOrPut(pluginId) { Mutex() }
    }

    private fun checkOpenLocked() {
        if (closed) throw PluginRuntimeError.Closed("Plugin runtime manager is closed")
    }
}

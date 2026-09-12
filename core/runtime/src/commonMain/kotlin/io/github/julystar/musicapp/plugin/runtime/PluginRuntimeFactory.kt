package io.github.julystar.musicapp.plugin.runtime

import uniffi.app_backend.PluginRuntimeOptions
import uniffi.app_backend.createPluginRuntime

class PluginRuntimeFactory(
    val settings: PluginRuntimeSettings,
) {
    fun create(plugin: PluginRuntimeDescriptor): PluginRuntime = try {
        RustPluginRuntime(
            createPluginRuntime(
                PluginRuntimeOptions(
                    pluginId = plugin.pluginId,
                    pluginName = plugin.pluginName,
                    appName = settings.appName,
                    packageName = settings.packageName,
                    appVersionName = settings.appVersionName,
                    appVersionCode = settings.appVersionCode.toULong(),
                    cacheDirectory = settings.cacheDirectory,
                    memoryLimitBytes = settings.memoryLimitBytes.toULong(),
                    stackLimitBytes = settings.stackLimitBytes.toULong(),
                    defaultTimeoutMs = settings.defaultTimeoutMs.toULong(),
                    loadTimeoutMs = settings.loadTimeoutMs.toULong(),
                    allowHttp = settings.allowHttp,
                    allowHttps = settings.allowHttps,
                    allowPrivateNetwork = settings.allowPrivateNetwork,
                    maxHttpRequestBytes = settings.maxHttpRequestBytes.toULong(),
                    maxHttpResponseBytes = settings.maxHttpResponseBytes.toULong(),
                    maxPluginCacheBytes = settings.maxPluginCacheBytes.toULong(),
                    maxInflateBytes = settings.maxInflateBytes.toULong(),
                ),
            ),
        )
    } catch (throwable: Throwable) {
        throw throwable.toPluginRuntimeError()
    }
}

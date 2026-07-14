package com.github.tidetunes.plugin.runtime

import uniffi.tidetunes_backend.PluginRuntimeOptions
import uniffi.tidetunes_backend.createPluginRuntime

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
                    maxHttpResponseBytes = settings.maxHttpResponseBytes.toULong(),
                ),
            ),
        )
    } catch (throwable: Throwable) {
        throw throwable.toPluginRuntimeError()
    }
}

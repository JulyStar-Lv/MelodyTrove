package com.github.tidetunes.plugin.runtime
import uniffi.tidetunes_backend.PluginRuntimeOptions
import uniffi.tidetunes_backend.createPluginRuntime
class PluginRuntimeFactory(private val settings:PluginRuntimeSettings){fun create(plugin:PluginRuntimeDescriptor):PluginRuntime=try{RustPluginRuntime(createPluginRuntime(PluginRuntimeOptions(plugin.pluginId,plugin.pluginName,settings.appName,settings.packageName,settings.appVersionName,settings.appVersionCode.toULong(),settings.cacheDirectory,settings.memoryLimitBytes.toULong(),settings.stackLimitBytes.toULong(),settings.defaultTimeoutMs.toULong(),settings.allowHttp,settings.allowHttps,settings.allowPrivateNetwork,settings.maxHttpResponseBytes.toULong())))}catch(t:Throwable){throw t.toPluginRuntimeError()}}

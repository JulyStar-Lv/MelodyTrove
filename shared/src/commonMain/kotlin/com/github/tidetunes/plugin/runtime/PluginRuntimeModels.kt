package com.github.tidetunes.plugin.runtime

data class PluginScriptBundle(val pluginId:String,val source:String,val filename:String,val sourceHash:String)
data class PluginRuntimeDescriptor(val pluginId:String,val pluginName:String,val pluginVersionCode:Long,val pluginUpdatedAt:Long,val entryFile:String,val includeDirs:List<String>,val directory:String)
data class PluginRuntimeSettings(val appName:String="TideTunes",val packageName:String="com.github.tidetunes",val appVersionName:String,val appVersionCode:Long=0,val cacheDirectory:String,val memoryLimitBytes:Long=4L*1024*1024,val stackLimitBytes:Long=2L*1024*1024,val defaultTimeoutMs:Long=5_000,val allowHttp:Boolean=false,val allowHttps:Boolean=true,val allowPrivateNetwork:Boolean=false,val maxHttpResponseBytes:Long=16L*1024*1024)
data class PluginRuntimeCacheKey(val pluginId:String,val pluginVersionCode:Long,val pluginUpdatedAt:Long,val scriptSourceHash:String)

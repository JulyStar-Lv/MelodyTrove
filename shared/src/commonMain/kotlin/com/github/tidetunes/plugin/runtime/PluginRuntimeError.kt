package com.github.tidetunes.plugin.runtime

sealed class PluginRuntimeError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Closed(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Initialization(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Script(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class FunctionNotFound(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class InvalidRequest(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Timeout(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Cancelled(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class OutOfMemory(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class HostApi(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Poisoned(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
    class Internal(message: String, cause: Throwable? = null) : PluginRuntimeError(message, cause)
}

internal fun Throwable.toPluginRuntimeError(): PluginRuntimeError {
    val message = message.orEmpty()
    val name = this::class.simpleName.orEmpty()
    return when {
        name.contains("FunctionNotFound") -> PluginRuntimeError.FunctionNotFound(message, this)
        name.contains("InvalidRequest") -> PluginRuntimeError.InvalidRequest(message, this)
        name.contains("OutOfMemory") -> PluginRuntimeError.OutOfMemory(message, this)
        name.contains("Initialization") -> PluginRuntimeError.Initialization(message, this)
        name.contains("Cancelled") -> PluginRuntimeError.Cancelled(message, this)
        name.contains("Timeout") -> PluginRuntimeError.Timeout(message, this)
        name.contains("Poisoned") -> PluginRuntimeError.Poisoned(message, this)
        name.contains("HostApi") -> PluginRuntimeError.HostApi(message, this)
        name.contains("Closed") -> PluginRuntimeError.Closed(message, this)
        name.contains("Script") -> PluginRuntimeError.Script(message, this)
        else -> PluginRuntimeError.Internal(message.ifBlank { "Plugin runtime failed" }, this)
    }
}

internal fun PluginRuntimeError.requiresRuntimeRebuild(): Boolean =
    this is PluginRuntimeError.Timeout ||
        this is PluginRuntimeError.Cancelled ||
        this is PluginRuntimeError.Closed ||
        this is PluginRuntimeError.OutOfMemory ||
        this is PluginRuntimeError.Poisoned ||
        this is PluginRuntimeError.Internal

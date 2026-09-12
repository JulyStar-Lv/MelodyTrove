package io.github.julystar.musicapp.plugin.runtime

interface PluginRuntime : AutoCloseable {
    suspend fun load(
        bundle: PluginScriptBundle,
        timeoutMs: Long,
    )

    suspend fun call(
        functionName: String,
        requestJson: String,
        timeoutMs: Long,
    ): String

    fun cancelCurrentCall()

    override fun close()
}

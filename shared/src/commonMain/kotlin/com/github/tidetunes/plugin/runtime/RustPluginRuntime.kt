package com.github.tidetunes.plugin.runtime

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.tidetunes_backend.PluginCallRequest
import uniffi.tidetunes_backend.PluginLoadRequest
import uniffi.tidetunes_backend.PluginRuntimeHandle

class RustPluginRuntime(
    private val handle: PluginRuntimeHandle,
) : PluginRuntime {
    private val mutex = Mutex()
    private val nextId = atomic(0L)
    private val currentId = atomic(0L)
    private val closed = atomic(false)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun load(
        bundle: PluginScriptBundle,
        timeoutMs: Long,
    ) = mutex.withLock {
        checkOpen()
        val operationId = nextOperationId()
        currentId.value = operationId
        try {
            runBlockingOperation(operationId) {
                handle.load(
                    PluginLoadRequest(
                        operationId = operationId.toULong(),
                        script = bundle.source,
                        filename = bundle.filename,
                        timeoutMs = timeoutMs.coerceAtLeast(1).toULong(),
                    ),
                )
            }
        } finally {
            currentId.compareAndSet(operationId, 0)
        }
    }

    override suspend fun call(
        functionName: String,
        requestJson: String,
        timeoutMs: Long,
    ): String = mutex.withLock {
        checkOpen()
        val operationId = nextOperationId()
        currentId.value = operationId
        try {
            runBlockingOperation(operationId) {
                handle.callJson(
                    PluginCallRequest(
                        operationId = operationId.toULong(),
                        functionName = functionName,
                        requestJson = requestJson,
                        timeoutMs = timeoutMs.coerceAtLeast(1).toULong(),
                    ),
                )
            }
        } finally {
            currentId.compareAndSet(operationId, 0)
        }
    }

    override fun cancelCurrentCall() {
        currentId.value
            .takeIf { it != 0L }
            ?.let { handle.cancelOperation(it.toULong()) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        cancelCurrentCall()
        workerScope.cancel()
        handle.shutdown()
        handle.destroy()
    }

    private suspend fun <T> runBlockingOperation(
        operationId: Long,
        block: () -> T,
    ): T = suspendCancellableCoroutine { continuation ->
        val worker = workerScope.launch {
            try {
                val result = block()
                if (continuation.isActive) continuation.resume(result)
            } catch (throwable: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        if (throwable is CancellationException) throwable else throwable.toPluginRuntimeError(),
                    )
                }
            }
        }
        worker.invokeOnCompletion { cause ->
            if (cause != null && continuation.isActive) {
                continuation.resumeWithException(
                    if (cause is CancellationException) {
                        PluginRuntimeError.Cancelled("Plugin operation was cancelled", cause)
                    } else {
                        cause.toPluginRuntimeError()
                    },
                )
            }
        }
        continuation.invokeOnCancellation {
            handle.cancelOperation(operationId.toULong())
            worker.cancel()
        }
    }

    private fun nextOperationId(): Long {
        var id = nextId.incrementAndGet()
        if (id == 0L) id = nextId.incrementAndGet()
        return id
    }

    private fun checkOpen() {
        if (closed.value) throw PluginRuntimeError.Closed("runtime is closed")
    }
}

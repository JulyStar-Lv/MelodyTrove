package com.github.tidetunes.plugin.runtime
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.tidetunes_backend.PluginCallRequest
import uniffi.tidetunes_backend.PluginRuntimeHandle

class RustPluginRuntime(private val handle:PluginRuntimeHandle):PluginRuntime{
 private val mutex=Mutex();private val nextId=atomic(0L);private val currentId=atomic(0L);private val closed=atomic(false)
 override suspend fun load(bundle:PluginScriptBundle)=mutex.withLock{checkOpen();try{withContext(Dispatchers.Default){handle.load(bundle.source,bundle.filename)}}catch(t:Throwable){throw t.toPluginRuntimeError()}}
 override suspend fun call(functionName:String,requestJson:String,timeoutMs:Long):String=mutex.withLock{checkOpen();val id=nextId.incrementAndGet();currentId.value=id;val job=currentCoroutineContext().job;val cancellation=job.invokeOnCompletion{cause->if(cause is CancellationException)handle.cancelOperation(id.toULong())};try{withContext(Dispatchers.Default){handle.callJson(PluginCallRequest(id.toULong(),functionName,requestJson,timeoutMs.toULong()))}}catch(t:Throwable){if(t is CancellationException)throw t;throw t.toPluginRuntimeError()}finally{cancellation.dispose();currentId.compareAndSet(id,0)}}
 override fun cancelCurrentCall(){currentId.value.takeIf{it!=0L}?.let{handle.cancelOperation(it.toULong())}}
 override fun close(){if(closed.compareAndSet(false,true)){handle.shutdown();handle.destroy()}}
 private fun checkOpen(){if(closed.value)throw PluginRuntimeError.Closed("runtime is closed")}
}

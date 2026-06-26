package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.singleton.Bridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.tidetune_core.ListLogFile
import uniffi.tidetune_core.ctTriggerError
import uniffi.tidetune_core.ctsListLogFiles
import uniffi.tidetune_core.ctsTriggerError
import uniffi.tidetune_core.ctsTriggerPanic
import kotlin.collections.orEmpty


class DebugMoreVM constructor(
    private val bridge: Bridge
) : ViewModel() {
    private val _logs: MutableStateFlow<List<ListLogFile>> = MutableStateFlow(emptyList())

    val logs = _logs.asStateFlow()

    fun reload() {
        val v = bridge.runSync { ctsListLogFiles(it) }
        _logs.value = v?.files.orEmpty()
    }

    fun triggerRustError() {
        bridge.runSyncRaw { ctsTriggerError(it) }
    }

    fun triggerRustAsyncError() {
        viewModelScope.launch {
            bridge.runRaw { ctTriggerError(it) }
        }
    }
    fun triggerRustPanic() {
        bridge.runSyncRaw { ctsTriggerPanic(it) }
    }

    fun triggerKotlinError() {
        throw RuntimeException("Kotlin error triggered")
    }

    fun triggerKotlinAsyncError() {
        viewModelScope.launch {
            throw RuntimeException("Kotlin async error triggered")
        }
    }
}

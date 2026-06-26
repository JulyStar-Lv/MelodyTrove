package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import com.github.tidetune.singleton.Bridge
import com.github.tidetune.singleton.ToastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uniffi.tidetune_core.ListLogFile
import uniffi.tidetune_core.ctsListLogFiles


class LogVM constructor(
    private val bridge: Bridge
) : ViewModel() {
    private val _logs: MutableStateFlow<List<ListLogFile>> = MutableStateFlow(emptyList())

    val logs = _logs.asStateFlow()

    fun reload() {
        val v = bridge.runSync { ctsListLogFiles(it) }
        _logs.value = v?.files.orEmpty()
    }
}

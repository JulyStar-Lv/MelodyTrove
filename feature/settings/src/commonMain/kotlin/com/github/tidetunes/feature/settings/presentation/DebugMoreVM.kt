package com.github.tidetunes.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.LogFile
import com.github.tidetunes.core.domain.repository.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebugMoreVM(
    private val logRepository: LogRepository,
) : ViewModel() {
    private val _logs: MutableStateFlow<List<LogFile>> = MutableStateFlow(emptyList())

    val logs = _logs.asStateFlow()

    fun reload() {
        _logs.value = logRepository.listLogFiles()
    }

    fun triggerRustError() {
        logRepository.triggerTestError()
    }

    fun triggerRustPanic() {
        logRepository.triggerTestPanic()
    }

    fun triggerRustAsyncError() {
        // Note: triggerTestError is synchronous on Bridge.runSyncRaw.
        // The async variant used Bridge.runRaw previously which is fire-and-forget.
        // Wrapping in launch preserves the same behavior.
        viewModelScope.launch {
            logRepository.triggerTestError()
        }
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

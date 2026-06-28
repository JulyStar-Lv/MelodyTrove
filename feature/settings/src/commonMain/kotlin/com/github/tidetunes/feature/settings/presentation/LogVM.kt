package com.github.tidetunes.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.github.tidetunes.core.domain.model.LogFile
import com.github.tidetunes.core.domain.repository.LogRepository
import com.github.tidetunes.core.domain.repository.ToastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogVM(
    private val logRepository: LogRepository,
) : ViewModel() {
    private val _logs: MutableStateFlow<List<LogFile>> = MutableStateFlow(emptyList())

    val logs = _logs.asStateFlow()

    fun reload() {
        _logs.value = logRepository.listLogFiles()
    }
}

package io.github.julystar.musicapp.feature.importing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImportStatusVM(
    private val librarySyncController: LibrarySyncController,
) : ViewModel() {
    val recentJobs = librarySyncController.recentTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun cancel(scanId: String) {
        viewModelScope.launch {
            librarySyncController.cancel(scanId)
        }
    }

    fun pause(scanId: String) {
        viewModelScope.launch {
            librarySyncController.pause(scanId)
        }
    }

    fun resume(scanId: String) {
        viewModelScope.launch {
            librarySyncController.resume(scanId)
        }
    }

    fun retry(scanId: String) {
        viewModelScope.launch {
            librarySyncController.retry(scanId)
        }
    }
}

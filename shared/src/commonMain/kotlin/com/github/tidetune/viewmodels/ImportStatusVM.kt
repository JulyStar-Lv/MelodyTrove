package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.domain.importing.RemoteLibraryImportCoordinator
import com.github.tidetune.singleton.ImportStatusRepository
import kotlinx.coroutines.launch

class ImportStatusVM(
    importStatusRepository: ImportStatusRepository,
    private val importCoordinator: RemoteLibraryImportCoordinator,
) : ViewModel() {
    val recentJobs = importStatusRepository.recentJobs

    fun cancel(scanId: String) {
        viewModelScope.launch {
            importCoordinator.cancelImport(scanId)
        }
    }
}

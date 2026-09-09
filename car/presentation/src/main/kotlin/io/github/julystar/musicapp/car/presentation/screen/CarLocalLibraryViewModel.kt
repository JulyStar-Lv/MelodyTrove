package io.github.julystar.musicapp.car.presentation.screen

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.julystar.musicapp.core.domain.repository.PermissionChecker
import io.github.julystar.musicapp.core.domain.repository.StorageRepository
import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncController
import io.github.julystar.musicapp.service.librarysync.domain.SourceAccountLibrarySyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface CarLocalLibraryState {
    data object Idle : CarLocalLibraryState

    data object AwaitingPermission : CarLocalLibraryState

    data object Scanning : CarLocalLibraryState

    data class Complete(val result: SourceAccountLibrarySyncResult) : CarLocalLibraryState

    data class Failed(val message: String) : CarLocalLibraryState
}

fun interface CarLocalLibraryImporter {
    suspend fun importMusicFolder(): SourceAccountLibrarySyncResult
}

class DefaultCarLocalLibraryImporter(
    private val storageRepository: StorageRepository,
    private val syncController: SourceAccountLibrarySyncController,
    private val musicDirectory: () -> String = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
    },
) : CarLocalLibraryImporter {
    override suspend fun importMusicFolder(): SourceAccountLibrarySyncResult {
        storageRepository.reload()
        val localAccount = storageRepository.storageAccounts
            .first { accounts -> accounts.any { it.isLocal } }
            .first { it.isLocal }
        storageRepository.replaceAccountRootPaths(
            accountId = localAccount.accountId,
            rootPaths = listOf(musicDirectory()),
        )
        return syncController.sync(localAccount.accountId)
    }
}

class CarLocalLibraryViewModel(
    private val permissionChecker: PermissionChecker,
    private val importer: CarLocalLibraryImporter,
) : ViewModel() {
    private val mutableState = MutableStateFlow<CarLocalLibraryState>(CarLocalLibraryState.Idle)
    private var importJob: Job? = null
    private var importAfterPermission = false

    val state: StateFlow<CarLocalLibraryState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            permissionChecker.havePermission.collectLatest { granted ->
                if (granted && importAfterPermission) {
                    importAfterPermission = false
                    startImport()
                }
            }
        }
    }

    fun importLocalMusic() {
        if (importJob?.isActive == true) return
        if (!permissionChecker.havePermission.value) {
            importAfterPermission = true
            mutableState.value = CarLocalLibraryState.AwaitingPermission
            permissionChecker.requestStoragePermission()
            return
        }
        startImport()
    }

    private fun startImport() {
        if (importJob?.isActive == true) return
        importJob = viewModelScope.launch {
            mutableState.value = CarLocalLibraryState.Scanning
            mutableState.value = try {
                CarLocalLibraryState.Complete(importer.importMusicFolder())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                CarLocalLibraryState.Failed(error.message ?: "无法扫描本地音乐")
            }
        }
    }
}

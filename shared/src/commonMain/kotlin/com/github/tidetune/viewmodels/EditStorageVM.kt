package com.github.tidetune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.domain.importing.RemoteLibraryImportCoordinator
import com.github.tidetune.singleton.ImportRepository
import com.github.tidetune.singleton.StorageRepository
import com.github.tidetune.singleton.ToastRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uniffi.tidetune_core.ArgUpsertStorage
import uniffi.tidetune_core.OneDriveDrive
import uniffi.tidetune_core.StorageConnectionTestResult
import uniffi.tidetune_core.StorageId
import uniffi.tidetune_core.StorageType


data class Validated(
    val addrEmpty: Boolean = false,
    val aliasEmpty: Boolean = false,
    val usernameEmpty: Boolean = false,
    val passwordEmpty: Boolean = false,
) {
    fun valid(): Boolean {
        return !addrEmpty && !aliasEmpty && !usernameEmpty && !passwordEmpty
    }
}

private fun defaultArgUpsertStorage(): ArgUpsertStorage {
    return ArgUpsertStorage(
        id = null,
        addr = "",
        alias = "",
        username = "",
        password = "",
        isAnonymous = true,
        typ = StorageType.WEBDAV,
    )
}


class EditStorageVM constructor(
    private val storageRepository: StorageRepository,
    private val toastRepository: ToastRepository,
    private val importRepository: ImportRepository,
    private val libraryImportCoordinator: RemoteLibraryImportCoordinator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _title = MutableStateFlow("")
    private val _musicCount = MutableStateFlow(0uL)
    private val _form = MutableStateFlow(defaultArgUpsertStorage())
    private var _formBackups = HashMap<StorageType, ArgUpsertStorage>()

    private val _validated = MutableStateFlow(Validated())
    private val _removeModalOpen = MutableStateFlow(false)
    private val _testResult = MutableStateFlow(StorageConnectionTestResult.NONE)
    private var _testJob: Job? = null
    private val _oneDriveDrives = MutableStateFlow<List<OneDriveDrive>>(emptyList())
    private val _oneDriveDrivesLoading = MutableStateFlow(false)
    private var _oneDriveDriveJob: Job? = null

    val form = _form.asStateFlow()
    val musicCount = _musicCount.asStateFlow()
    val title = _title.asStateFlow()
    val validated = _validated.asStateFlow()

    val removeModalOpen = _removeModalOpen.asStateFlow()
    val isCreated = form.map { form -> form.id == null }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    val testResult = _testResult.asStateFlow()
    val oneDriveDrives = _oneDriveDrives.asStateFlow()
    val oneDriveDrivesLoading = _oneDriveDrivesLoading.asStateFlow()

    init {
        viewModelScope.launch {
            storageRepository.oauthRefreshToken.collect {
                    refreshToken ->
                updateForm { storage ->
                    if (storage.typ == StorageType.ONE_DRIVE) {
                        storage.password = refreshToken
                    }
                    storage
                }
                if (refreshToken.isNotBlank()) {
                    loadOneDriveDrives(refreshToken)
                }
            }
        }

        _form.value = defaultArgUpsertStorage()
        _title.value = ""
        _musicCount.value = 0u

        val id: Long? = savedStateHandle["id"]
        val storage = storageRepository.storages.value.find { v -> id != null && v.id == StorageId(id) }
        if (storage != null) {
            _form.value = ArgUpsertStorage(
                id = storage.id,
                addr = storage.addr,
                alias = storage.alias,
                username = storage.username,
                password = "",
                isAnonymous = storage.isAnonymous,
                typ = storage.typ
            )
            _title.value = VImportStorageEntry(storage).name
            _musicCount.value = storage.musicCount
            viewModelScope.launch {
                storageRepository.loadCredential(storage.id)?.let { credential ->
                    updateForm { current ->
                        current.username = credential.username
                        current.password = credential.secret
                        current.isAnonymous = credential.isAnonymous
                        current
                    }
                    if (storage.typ == StorageType.ONE_DRIVE) {
                        loadOneDriveDrives(credential.secret)
                    }
                }
            }
        }
    }

    fun test() {
        resetTestResult()
        if (!validate()) {
            return
        }
        _testResult.value = StorageConnectionTestResult.TESTING

        _testJob = viewModelScope.launch {
            _testResult.value = storageRepository.test(form.value)
            sendTestToast()

            delay(5000)
            resetTestResult()
        }
    }

    private fun sendTestToast() {
        val testing = _testResult.value
        if (testing == StorageConnectionTestResult.NONE || testing == StorageConnectionTestResult.TESTING) {
            return;
        }

        when (testing) {
            StorageConnectionTestResult.SUCCESS -> {
                toastRepository.emitToast("Success")
            }
            StorageConnectionTestResult.TIMEOUT -> {
                toastRepository.emitToast("Error: Timeout")
            }
            StorageConnectionTestResult.UNAUTHORIZED -> {
                toastRepository.emitToast("Error: Unauthorized")
            }
            StorageConnectionTestResult.OTHER_ERROR -> {
                toastRepository.emitToast("Error: Other error")
            }
            else -> {}
        }
    }


    fun openRemoveModal() {
        _removeModalOpen.value = true
    }

    fun closeRemoveModal() {
        _removeModalOpen.value = false
    }

    fun updateForm(block: (form: ArgUpsertStorage) -> ArgUpsertStorage) {
        _form.value = block(form.value.copy())
    }

    fun changeType(typ: StorageType) {
        _formBackups.set(_form.value.typ, _form.value.copy())

        val backup = _formBackups.get(typ)
        if (backup != null) {
            _form.value = backup
        } else {
            val newForm = ArgUpsertStorage(
                id = _form.value.id,
                addr = "",
                alias = _form.value.alias,
                username = "",
                password = "",
                isAnonymous = false,
                typ = typ
            )
            _form.value = newForm
        }
        _validated.value = Validated()
    }

    private fun validate(): Boolean {
        val f = form.value
        _validated.value = Validated(
            addrEmpty = if (
                f.typ == StorageType.WEBDAV || f.typ == StorageType.ONE_DRIVE
            ) {
                f.addr.isBlank()
            } else {
                false
            },
            aliasEmpty = if (f.typ == StorageType.WEBDAV) { false } else { f.alias.isBlank() },
            usernameEmpty = if (f.typ == StorageType.WEBDAV) { !f.isAnonymous && f.username.isBlank() } else { false },
            passwordEmpty = if (f.typ == StorageType.WEBDAV) { !f.isAnonymous && f.password.isBlank() } else { f.password.isBlank() },
        )
        return _validated.value.valid()
    }

    fun remove() {
        val id = _form.value.id

        if (id != null) {
            viewModelScope.launch {
                storageRepository.remove(id)
            }
        }
    }

    fun prepareImportLibraryFolder() {
        importRepository.prepareCurrentDirectory { storageId, path, remoteId ->
            viewModelScope.launch {
                toastRepository.emitToast("Importing library folder...")
                val result = runCatching {
                    val selectedStorage = storageRepository.storages.value
                        .firstOrNull { it.id == storageId }
                        ?: error("Selected storage is no longer available")
                    if (selectedStorage.typ == StorageType.ONE_DRIVE) {
                        libraryImportCoordinator.syncOneDriveFolder(
                            storageId = storageId.value,
                            selectedFolderRemoteId = requireNotNull(remoteId) {
                                "OneDrive folder has no DriveItem ID"
                            },
                            selectedFolderCanonicalPath = path,
                            selectedFolderDisplayPath = path,
                        )
                    } else {
                        libraryImportCoordinator.scanAndImportFolder(
                            storageId = storageId.value,
                            selectedFolderRemoteId = remoteId,
                            selectedFolderCanonicalPath = path,
                            selectedFolderDisplayPath = path,
                        )
                    }
                }
                result.onSuccess { value ->
                    toastRepository.emitToast(
                        "Library import completed: ${value.importedCount} imported, " +
                            "${value.skippedCount} skipped, ${value.failedCount} failed"
                    )
                    storageRepository.reload()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        toastRepository.emitToast("Library import cancelled")
                    } else {
                        toastRepository.emitToast(
                            "Library import failed: ${error.message ?: "unknown error"}"
                        )
                    }
                }
            }
        }
    }

    suspend fun startOneDriveOAuth(): String = storageRepository.startOneDriveOAuth()

    fun selectOneDriveDrive(drive: OneDriveDrive) {
        updateForm { storage ->
            storage.addr = drive.id
            storage
        }
    }

    fun disconnectOneDrive() {
        _oneDriveDriveJob?.cancel()
        _oneDriveDrives.value = emptyList()
        _oneDriveDrivesLoading.value = false
        updateForm { storage ->
            storage.addr = ""
            storage.password = ""
            storage
        }
    }

    suspend fun finish(): Boolean {
        if (!validate()) {
            return false
        }

        storageRepository.upsertStorage(_form.value)
        return true
    }

    private fun resetTestResult() {
        _testJob?.cancel()
        _testJob = null
        _testResult.value = StorageConnectionTestResult.NONE
    }

    private fun loadOneDriveDrives(refreshToken: String) {
        _oneDriveDriveJob?.cancel()
        _oneDriveDriveJob = viewModelScope.launch {
            _oneDriveDrivesLoading.value = true
            try {
                val result = storageRepository.listOneDriveDrives(refreshToken)
                val drives = result.drives
                _oneDriveDrives.value = drives
                if (result.refreshToken != refreshToken) {
                    updateForm { storage ->
                        storage.password = result.refreshToken
                        storage
                    }
                    form.value.id?.let { storageId ->
                        storageRepository.updateOneDriveRefreshToken(
                            storageId,
                            result.refreshToken,
                        )
                    }
                }
                val selected = form.value.addr
                if (drives.isNotEmpty() && drives.none { it.id == selected }) {
                    selectOneDriveDrive(drives.first())
                }
            } catch (error: Exception) {
                _oneDriveDrives.value = emptyList()
                toastRepository.emitToast(
                    "Unable to list OneDrive drives: ${error.message ?: "unknown error"}"
                )
            } finally {
                _oneDriveDrivesLoading.value = false
            }
        }
    }
}

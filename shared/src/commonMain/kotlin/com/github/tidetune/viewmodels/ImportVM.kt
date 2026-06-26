package com.github.tidetune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetune.singleton.ImportRepository
import com.github.tidetune.singleton.ImportSelectionMode
import com.github.tidetune.singleton.PermissionChecker
import com.github.tidetune.singleton.RemoteScannerRepository
import com.github.tidetune.singleton.StorageRepository
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.tidetune_core.CurrentStorageStateType
import uniffi.tidetune_core.ListStorageEntryChildrenResp
import uniffi.tidetune_core.Storage
import uniffi.tidetune_core.StorageEntry
import uniffi.tidetune_core.StorageId
import uniffi.tidetune_core.StorageType
import com.github.tidetune.utils.decodeUrlComponent

data class SplitPathItem(
    val path: String,
    val name: String,
)

private fun defaultSplitPaths(): List<SplitPathItem> {
    return listOf()
}

class ImportVM constructor(
    private val storageRepository: StorageRepository,
    private val importRepository: ImportRepository,
    private val permissionRepository: PermissionChecker,
    private val remoteScannerRepository: RemoteScannerRepository,
) : ViewModel() {
    private val _currentPath = MutableStateFlow("/")
    private val _splitPaths = _currentPath.map { path ->
        val components = path.split('/').filter { it.isNotEmpty() }
        val splitPaths = mutableListOf<SplitPathItem>()

        var currentPath = ""
        for (component in components) {
            currentPath = if (currentPath == "/") {
                "/$component"
            } else {
                "$currentPath/$component"
            }
            val name = decodeUrlComponent(component)
            splitPaths.add(SplitPathItem(currentPath, name))
        }

        splitPaths
    }.stateIn(viewModelScope, SharingStarted.Lazily, defaultSplitPaths())
    private val _selected = MutableStateFlow(persistentHashSetOf<String>())
    private val _entries = MutableStateFlow(listOf<StorageEntry>())
    private val _selectedStorageId = MutableStateFlow(storageRepository.storages.value.firstOrNull()?.id)
    private val _loadState = MutableStateFlow(CurrentStorageStateType.LOADING)
    private val _disabledToggleAll = _entries.map { entries ->
        entries.all { it.isDir }
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    private val _undoStack = MutableStateFlow(persistentListOf<String>())
    private val directoryRemoteIds = mutableMapOf<String, String?>()

    val splitPaths = _splitPaths
    val selectedCount = _selected.combine(_entries) { selected, entries ->
        entries.count { entry -> selected.contains(entry.path) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val entries = _entries.asStateFlow()
    val selected = _selected.asStateFlow()
    val allowTypes = importRepository.allowTypes
    val selectionMode = importRepository.selectionMode
    val selectedStorageId = _selectedStorageId.asStateFlow()
    val loadState = _loadState.asStateFlow()
    val disabledToggleAll = _disabledToggleAll
    val canUndo =
        _undoStack.map {
            undoStack -> undoStack.isNotEmpty()
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)


    init {
        viewModelScope.launch {
            storageRepository.storages.collect { storages ->
                val storage = storages.find { storage -> storage.id == _selectedStorageId.value }
                if (storage == null) {
                    _selectedStorageId.value = storageRepository.storages.value.firstOrNull()?.id
                }

                reload()
            }
        }
        viewModelScope.launch {
            reload()
        }
        viewModelScope.launch {
            permissionRepository.havePermission.collect {
                reload()
            }
        }
    }

    fun clickEntry(entry: StorageEntry) {
        if (entry.isDir) {
            directoryRemoteIds[entry.path] = entry.remoteId
            navigateDir(entry.path)
        } else if (
            selectionMode.value == ImportSelectionMode.Entries &&
            allowTypes.value.contains(entry.entryTyp())
        ) {
            toggleSelect(entry.path)
        }
    }

    fun navigateDir(path: String) {
        pushCurrentToUndoStack()
        navigateDirImpl(path)
    }

    private fun toggleSelect(path: String) {
        val selected = _selected.value
        val next = {
            if (selected.contains(path)) {
                selected.remove(path)
            } else {
                selected.add(path)
            }
        }()
        _selected.value = next
    }

    fun finish() {
        val v = _entries.value.filter { entry -> _selected.value.contains(entry.path) }
        importRepository.onFinish(v)
    }

    fun finishCurrentDirectory() {
        val storage = currentStorage() ?: return
        importRepository.onFinishCurrentDirectory(
            storageId = storage.id,
            path = currentPath(),
            remoteId = currentDirectoryRemoteId(storage),
        )
    }

    fun requestPermission() {
        permissionRepository.requestStoragePermission()
    }

    fun selectStorage(storageId: StorageId) {
        _selectedStorageId.value = storageId
        _undoStack.value = persistentListOf()
        directoryRemoteIds.clear()

        navigateDirImpl("/")
    }

    fun toggleAll() {
        if (selectionMode.value != ImportSelectionMode.Entries) return

        val allSelected = _selected.value.size == _entries.value.size
        if (allSelected) {
            _selected.update { selected ->
                selected.clear()
            }
        } else {
            _selected.update { selected ->
                selected.clear().addAll(_entries.value.map { it.path })
            }
        }
    }

    fun reload() {
        val storage = currentStorage() ?: return

        if (storage.typ == StorageType.LOCAL && !permissionRepository.havePermission.value) {
            _loadState.value = CurrentStorageStateType.NEED_PERMISSION
            return
        }

        _loadState.value = CurrentStorageStateType.LOADING
        _entries.value = emptyList()

        viewModelScope.launch {
            val resp = remoteScannerRepository.listDirectory(
                storageId = storage.id,
                path = currentPath(),
            )

            when (resp) {
                is ListStorageEntryChildrenResp.Ok -> {
                    _loadState.value = CurrentStorageStateType.OK
                    _entries.value = resp.v1
                }

                ListStorageEntryChildrenResp.AuthenticationFailed -> {
                    _loadState.value = CurrentStorageStateType.AUTHENTICATION_FAILED
                }

                ListStorageEntryChildrenResp.Timeout -> {
                    _loadState.value = CurrentStorageStateType.TIMEOUT
                }

                ListStorageEntryChildrenResp.Unknown -> {
                    _loadState.value = CurrentStorageStateType.UNKNOWN_ERROR
                }
            }
        }
    }

    fun undo() {
        val current = popCurrentFromUndoStack()
        if (current != null) {
            navigateDirImpl(current)
        }
    }

    private fun currentPath(): String {
        val p = _splitPaths.value.lastOrNull()?.path

        if (p == null) {
            return "/"
        }
        return p
    }

    private fun currentStorage(): Storage? {
        val storage = storageRepository.storages.value.find { storage -> storage.id == _selectedStorageId.value }
        return storage
    }

    private fun currentDirectoryRemoteId(storage: Storage): String? {
        if (currentPath() == "/" && storage.typ == StorageType.ONE_DRIVE) {
            return "root"
        }
        return directoryRemoteIds[currentPath()]
    }

    private fun pushCurrentToUndoStack() {
        val currentUndoStack = _undoStack.value
        val nextUndoStack = currentUndoStack.add(currentPath())
        _undoStack.value = nextUndoStack
    }

    private fun popCurrentFromUndoStack(): String? {
        val currentUndoStack = _undoStack.value
        val current = currentUndoStack.lastOrNull()
        if (current != null) {
            val next = currentUndoStack.removeAt(currentUndoStack.lastIndex)
            _undoStack.value = next
        }
        return current
    }


    private fun navigateDirImpl(path: String) {
        _currentPath.value = path
        _selected.update { selected ->
            selected.clear()
        }

        reload()
    }
}

class VImportStorageEntry(private val storage: Storage) {
    val id: StorageId
        get() = storage.id

    val isLocal: Boolean
        get() = storage.typ == StorageType.LOCAL

    val name: String
        get() {
            if (storage.alias != "") {
                return storage.alias
            }
            return storage.addr
        }

    val subtitle: String
        get() {
            if (storage.alias != "") {
                return storage.addr
            }
            return ""
        }
}

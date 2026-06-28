package com.github.tidetunes.feature.importing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.core.domain.model.StorageAccountInfo
import com.github.tidetunes.core.domain.repository.StorageRepository
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.domain.repository.PermissionChecker
import com.github.tidetunes.source.api.ImportRepository
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.feature.importing.presentation.ImportAction
import com.github.tidetunes.feature.importing.presentation.ImportEvent
import com.github.tidetunes.feature.importing.presentation.ImportLoadState
import com.github.tidetunes.feature.importing.presentation.ImportState
import com.github.tidetunes.feature.importing.presentation.ImportStorageAccountUi
import com.github.tidetunes.feature.importing.presentation.importState
import com.github.tidetunes.feature.importing.presentation.toImportLoadState
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.MusicSourceRegistry
import com.github.tidetunes.source.api.SourceDirectorySelection
import com.github.tidetunes.source.api.SourceListResult
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.SourceNodeType
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitPathItem(
    val path: String,
    val name: String,
)


private fun decodeUrlComponent(value: String): String {
    val decoded = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        if (value[index] != '%' || index + 2 >= value.length) {
            decoded.append(value[index])
            index += 1
            continue
        }
        val bytes = mutableListOf<Byte>()
        while (index + 2 < value.length && value[index] == '%') {
            val byte = value.substring(index + 1, index + 3).toIntOrNull(16) ?: break
            bytes += byte.toByte()
            index += 3
        }
        if (bytes.isEmpty()) {
            decoded.append('%')
            index += 1
        } else {
            decoded.append(bytes.toByteArray().decodeToString())
        }
    }
    return decoded.toString()
}

private fun defaultSplitPaths(): List<SplitPathItem> {
    return listOf()
}

private data class ImportBrowseState(
    val splitPaths: List<SplitPathItem>,
    val entries: List<SourceNode>,
    val selectedPaths: ImmutableSet<String>,
)

private data class ImportSelectionState(
    val selectedCount: Int,
    val allowNodeTypes: List<SourceNodeType>,
    val selectionMode: ImportSelectionMode,
)

private data class ImportChromeState(
    val selectedStorageAccountId: SourceAccountId?,
    val loadState: ImportLoadState,
    val canUndo: Boolean,
    val disabledToggleAll: Boolean,
)

class ImportVM constructor(
    private val storageRepository: StorageRepository,
    private val importRepository: ImportRepository,
    private val permissionRepository: PermissionChecker,
    private val sourceRegistry: MusicSourceRegistry,
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
    private val _entries = MutableStateFlow(listOf<SourceNode>())
    private val _selectedStorageAccountId = MutableStateFlow(
        storageRepository.storageAccounts.value.firstOrNull()?.accountId
    )
    private val _loadState = MutableStateFlow(ImportLoadState.Loading)
    private val _disabledToggleAll = _entries.map { entries ->
        entries.all { entry -> entry.type == SourceNodeType.Folder }
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    private val _undoStack = MutableStateFlow(persistentListOf<String>())
    private val _events = Channel<ImportEvent>(Channel.BUFFERED)
    private val directoryRemoteIds = mutableMapOf<String, String?>()

    val splitPaths = _splitPaths
    val selectedCount = _selected.combine(_entries) { selected, entries ->
        entries.count { entry -> selected.contains(entry.path) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val entries = _entries.asStateFlow()
    val selected = _selected.asStateFlow()
    private val allowTypes = importRepository.allowTypes
    val allowNodeTypes: StateFlow<List<SourceNodeType>> = allowTypes
    val selectionMode = importRepository.selectionMode
    val selectedStorageAccountId = _selectedStorageAccountId.asStateFlow()
    val loadState = _loadState.asStateFlow()
    val disabledToggleAll = _disabledToggleAll
    val canUndo =
        _undoStack.map {
            undoStack -> undoStack.isNotEmpty()
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val events = _events.receiveAsFlow()
    val state = combine(
        combine(_splitPaths, _entries, _selected) { splitPaths, entries, selectedPaths ->
            ImportBrowseState(
                splitPaths = splitPaths,
                entries = entries,
                selectedPaths = selectedPaths,
            )
        },
        combine(selectedCount, allowNodeTypes, selectionMode) { selectedCount, allowNodeTypes, selectionMode ->
            ImportSelectionState(
                selectedCount = selectedCount,
                allowNodeTypes = allowNodeTypes,
                selectionMode = selectionMode,
            )
        },
        combine(_selectedStorageAccountId, _loadState, canUndo, disabledToggleAll) {
                selectedStorageAccountId,
                loadState,
                canUndo,
                disabledToggleAll ->
            ImportChromeState(
                selectedStorageAccountId = selectedStorageAccountId,
                loadState = loadState,
                canUndo = canUndo,
                disabledToggleAll = disabledToggleAll,
            )
        },
        storageRepository.storageAccounts.map { accounts ->
            accounts.map { it.toImportStorageAccountUi() }
        },
    ) { browse, selection, chrome, storageAccounts ->
        importState(
            splitPaths = browse.splitPaths,
            entries = browse.entries,
            selectedPaths = browse.selectedPaths,
            selectedCount = selection.selectedCount,
            allowNodeTypes = selection.allowNodeTypes,
            storageAccounts = storageAccounts,
            selectedStorageAccountId = chrome.selectedStorageAccountId,
            loadState = chrome.loadState,
            selectionMode = selection.selectionMode,
            canUndo = chrome.canUndo,
            disabledToggleAll = chrome.disabledToggleAll,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, ImportState())


    init {
        viewModelScope.launch {
            storageRepository.storageAccounts.collect { accounts ->
                val account = accounts.find { account ->
                    account.accountId == _selectedStorageAccountId.value
                }
                if (account == null) {
                    _selectedStorageAccountId.value =
                        storageRepository.storageAccounts.value.firstOrNull()?.accountId
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

    fun onAction(action: ImportAction) {
        when (action) {
            ImportAction.NavigateBack -> {
                if (canUndo.value) {
                    undo()
                } else {
                    sendEvent(ImportEvent.NavigateBack)
                }
            }

            ImportAction.ToggleAll -> toggleAll()
            ImportAction.FinishSelection -> {
                finish()
                sendEvent(ImportEvent.NavigateBack)
            }

            ImportAction.FinishCurrentDirectory -> {
                finishCurrentDirectory()
                sendEvent(ImportEvent.NavigateBack)
            }

            ImportAction.RecoverFromLoadError -> {
                if (_loadState.value == ImportLoadState.NeedsPermission) {
                    requestPermission()
                } else {
                    reload()
                }
            }

            is ImportAction.SelectStorage -> selectStorage(action.accountId)
            is ImportAction.OpenPath -> navigateDir(action.path)
            is ImportAction.OpenEntry -> clickEntry(action.entry)
        }
    }

    fun clickEntry(entry: SourceNode) {
        if (entry.type == SourceNodeType.Folder) {
            directoryRemoteIds[entry.path] = entry.remoteId
            navigateDir(entry.path)
        } else if (
            selectionMode.value == ImportSelectionMode.Entries &&
            allowNodeTypes.value.contains(entry.type)
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
        val account = currentAccount() ?: return
        val entries = _entries.value
            .filter { entry -> _selected.value.contains(entry.path) }
            .map { entry ->
                SourceNodeSelection(
                    sourceId = account.sourceId,
                    accountId = account.accountId,
                    node = entry,
                )
            }
        importRepository.onFinish(entries)
    }

    fun finishCurrentDirectory() {
        val account = currentAccount() ?: return
        importRepository.onFinishCurrentDirectory(
            SourceDirectorySelection(
                sourceId = account.sourceId,
                accountId = account.accountId,
                path = currentPath(),
                remoteId = currentDirectoryRemoteId(account),
            )
        )
    }

    fun requestPermission() {
        permissionRepository.requestStoragePermission()
    }

    fun selectStorage(accountId: SourceAccountId) {
        _selectedStorageAccountId.value = accountId
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
        val account = currentAccount() ?: return

        if (account.isLocal && !permissionRepository.havePermission.value) {
            _loadState.value = ImportLoadState.NeedsPermission
            return
        }

        _loadState.value = ImportLoadState.Loading
        _entries.value = emptyList()

        viewModelScope.launch {
            val source = sourceRegistry.sourceOrNull(account.sourceId)
            if (source == null) {
                _loadState.value = ImportLoadState.UnknownError
                return@launch
            }

            when (
                val result = source.list(
                    accountId = account.accountId,
                    directoryId = currentPath(),
                )
            ) {
                is SourceListResult.Success -> {
                    _loadState.value = ImportLoadState.Ready
                    _entries.value = result.nodes
                }

                is SourceListResult.Failure -> {
                    _loadState.value = result.reason.toImportLoadState()
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

    private fun currentAccount(): StorageAccountInfo? {
        return storageRepository.storageAccounts.value.find { account ->
            account.accountId == _selectedStorageAccountId.value
        }
    }

    private fun currentDirectoryRemoteId(account: StorageAccountInfo): String? {
        if (currentPath() == "/" && account.isOneDrive) {
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

    private fun sendEvent(event: ImportEvent) {
        _events.trySend(event)
    }
}

private fun StorageAccountInfo.toImportStorageAccountUi(): ImportStorageAccountUi {
    return ImportStorageAccountUi(
        accountId = accountId,
        isLocal = isLocal,
        name = title,
        subtitle = subtitle,
    )
}

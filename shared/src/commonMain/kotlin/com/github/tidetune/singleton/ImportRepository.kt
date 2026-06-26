package com.github.tidetune.singleton

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uniffi.tidetune_core.StorageEntry
import uniffi.tidetune_core.StorageEntryType
import uniffi.tidetune_core.StorageId


typealias ImportHandler = (entries: List<StorageEntry>) -> Unit
typealias DirectoryImportHandler = (
    storageId: StorageId,
    path: String,
    remoteId: String?,
) -> Unit

enum class ImportSelectionMode {
    Entries,
    CurrentDirectory,
}


object RouteImportType {
    val Music = "Music"
    val Lyric = "Lyric"
    val EditPlaylist = "EditPlaylist"
    val EditPlaylistCover = "EditPlaylistCover"
    val LibraryFolder = "LibraryFolder"
}

class ImportRepository() {
    private val _allowTypes = MutableStateFlow(listOf<StorageEntryType>())
    private val _selectionMode = MutableStateFlow(ImportSelectionMode.Entries)
    private var _importCallback: ((List<StorageEntry>) -> Unit)? = null
    private var _directoryImportCallback: DirectoryImportHandler? = null

    val allowTypes = _allowTypes.asStateFlow()
    val selectionMode = _selectionMode.asStateFlow()

    fun prepare(types: List<StorageEntryType>, block: ImportHandler) {
        _allowTypes.value = types
        _selectionMode.value = ImportSelectionMode.Entries
        _importCallback = block
        _directoryImportCallback = null
    }

    fun prepareCurrentDirectory(block: DirectoryImportHandler) {
        _allowTypes.value = emptyList()
        _selectionMode.value = ImportSelectionMode.CurrentDirectory
        _importCallback = null
        _directoryImportCallback = block
    }

    fun onFinish(entries: List<StorageEntry>) {
        val c = _importCallback
        _importCallback = null
        if (c != null) {
            c(entries)
        }
    }

    fun onFinishCurrentDirectory(
        storageId: StorageId,
        path: String,
        remoteId: String?,
    ) {
        val c = _directoryImportCallback
        _directoryImportCallback = null
        if (c != null) {
            c(storageId, path, remoteId)
        }
    }
}

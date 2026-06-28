package com.github.tidetunes.feature.importing.data

import com.github.tidetunes.source.api.SourceDirectorySelection
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.ImportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.github.tidetunes.core.domain.model.ImportSelectionMode


typealias ImportHandler = (entries: List<SourceNodeSelection>) -> Unit
typealias DirectoryImportHandler = (selection: SourceDirectorySelection) -> Unit



object RouteImportType {
    val Music = "Music"
    val Lyric = "Lyric"
    val EditPlaylist = "EditPlaylist"
    val EditPlaylistCover = "EditPlaylistCover"
    val LibraryFolder = "LibraryFolder"
}

class ImportRepositoryImpl : ImportRepository {
    private val _allowTypes = MutableStateFlow(listOf<SourceNodeType>())
    private val _selectionMode = MutableStateFlow(ImportSelectionMode.Entries)
    private var _importCallback: ((List<SourceNodeSelection>) -> Unit)? = null
    private var _directoryImportCallback: ((SourceDirectorySelection) -> Unit)? = null

    override val allowTypes = _allowTypes.asStateFlow()
    override val selectionMode = _selectionMode.asStateFlow()

    override fun prepare(types: List<SourceNodeType>, block: (List<SourceNodeSelection>) -> Unit) {
        _allowTypes.value = types
        _selectionMode.value = ImportSelectionMode.Entries
        _importCallback = block
        _directoryImportCallback = null
    }

    override fun prepareCurrentDirectory(block: (SourceDirectorySelection) -> Unit) {
        _allowTypes.value = emptyList()
        _selectionMode.value = ImportSelectionMode.CurrentDirectory
        _importCallback = null
        _directoryImportCallback = block
    }

    override fun onFinish(entries: List<SourceNodeSelection>) {
        val c = _importCallback
        _importCallback = null
        if (c != null) {
            c(entries)
        }
    }

    override fun onFinishCurrentDirectory(selection: SourceDirectorySelection) {
        val c = _directoryImportCallback
        _directoryImportCallback = null
        if (c != null) {
            c(selection)
        }
    }
}

package com.github.tidetunes.source.api

import com.github.tidetunes.core.domain.model.ImportSelectionMode
import kotlinx.coroutines.flow.StateFlow

interface ImportRepository {
    val allowTypes: StateFlow<List<SourceNodeType>>
    val selectionMode: StateFlow<ImportSelectionMode>

    fun prepare(types: List<SourceNodeType>, block: (List<SourceNodeSelection>) -> Unit)
    fun prepareCurrentDirectory(block: (SourceDirectorySelection) -> Unit)
    fun onFinish(entries: List<SourceNodeSelection>)
    fun onFinishCurrentDirectory(selection: SourceDirectorySelection)
}

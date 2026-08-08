package io.github.julystar.musicapp.source.api

import io.github.julystar.musicapp.core.domain.model.ImportSelectionMode
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import kotlinx.coroutines.flow.StateFlow

interface ImportRepository {
    val allowTypes: StateFlow<List<SourceNodeType>>
    val selectionMode: StateFlow<ImportSelectionMode>
    val currentDirectoryAccountId: StateFlow<SourceAccountId?>

    fun prepare(types: List<SourceNodeType>, block: (List<SourceNodeSelection>) -> Unit)
    fun prepareCurrentDirectory(
        accountId: SourceAccountId? = null,
        block: (SourceDirectorySelection) -> Unit,
    )
    fun onFinish(entries: List<SourceNodeSelection>)
    fun onFinishCurrentDirectory(selection: SourceDirectorySelection)
}

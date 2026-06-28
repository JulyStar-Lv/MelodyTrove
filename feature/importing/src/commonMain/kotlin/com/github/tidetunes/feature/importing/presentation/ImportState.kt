package com.github.tidetunes.feature.importing.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ImportState(
    val splitPaths: ImmutableList<ImportPathUi> = persistentListOf(),
    val entries: ImmutableList<SourceNode> = persistentListOf(),
    val selectedPaths: ImmutableSet<String> = persistentHashSetOf(),
    val selectedCount: Int = 0,
    val allowNodeTypes: ImmutableList<SourceNodeType> = persistentListOf(),
    val storageAccounts: ImmutableList<ImportStorageAccountUi> = persistentListOf(),
    val selectedStorageAccountId: SourceAccountId? = null,
    val loadState: ImportLoadState = ImportLoadState.Loading,
    val selectionMode: ImportSelectionMode = ImportSelectionMode.Entries,
    val canUndo: Boolean = false,
    val disabledToggleAll: Boolean = true,
)

@Immutable
data class ImportPathUi(
    val path: String,
    val name: String,
)

@Immutable
data class ImportStorageAccountUi(
    val accountId: SourceAccountId,
    val isLocal: Boolean,
    val name: String,
    val subtitle: String,
)

enum class ImportLoadState {
    Loading,
    Ready,
    AuthenticationFailed,
    Timeout,
    UnknownError,
    NeedsPermission,
}

sealed interface ImportAction {
    data object NavigateBack : ImportAction
    data object ToggleAll : ImportAction
    data object FinishSelection : ImportAction
    data object FinishCurrentDirectory : ImportAction
    data object RecoverFromLoadError : ImportAction
    data class SelectStorage(val accountId: SourceAccountId) : ImportAction
    data class OpenPath(val path: String) : ImportAction
    data class OpenEntry(val entry: SourceNode) : ImportAction
}

sealed interface ImportEvent {
    data object NavigateBack : ImportEvent
}


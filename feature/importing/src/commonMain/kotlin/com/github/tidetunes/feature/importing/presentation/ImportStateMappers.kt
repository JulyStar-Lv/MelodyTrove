package com.github.tidetunes.feature.importing.presentation

import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.source.api.SourceListFailureReason
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeType
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList

fun importState(
    splitPaths: List<SplitPathItem>,
    entries: List<SourceNode>,
    selectedPaths: ImmutableSet<String>,
    selectedCount: Int,
    allowNodeTypes: List<SourceNodeType>,
    storageAccounts: List<ImportStorageAccountUi>,
    selectedStorageAccountId: com.github.tidetunes.core.domain.model.SourceAccountId?,
    loadState: ImportLoadState,
    selectionMode: ImportSelectionMode,
    canUndo: Boolean,
    disabledToggleAll: Boolean,
): ImportState {
    return ImportState(
        splitPaths = splitPaths.map { item ->
            ImportPathUi(
                path = item.path,
                name = item.name,
            )
        }.toPersistentList(),
        entries = entries.toPersistentList(),
        selectedPaths = selectedPaths,
        selectedCount = selectedCount,
        allowNodeTypes = allowNodeTypes.toPersistentList(),
        storageAccounts = storageAccounts.toPersistentList(),
        selectedStorageAccountId = selectedStorageAccountId,
        loadState = loadState,
        selectionMode = selectionMode,
        canUndo = canUndo,
        disabledToggleAll = disabledToggleAll,
    )
}

fun SourceListFailureReason.toImportLoadState(): ImportLoadState {
    return when (this) {
        SourceListFailureReason.Unauthorized -> ImportLoadState.AuthenticationFailed
        SourceListFailureReason.Timeout -> ImportLoadState.Timeout
        SourceListFailureReason.PermissionDenied,
        SourceListFailureReason.NotFound,
        SourceListFailureReason.InvalidAddress,
        SourceListFailureReason.UnsupportedSecurityPolicy,
        SourceListFailureReason.UnsupportedAccount,
        SourceListFailureReason.Unavailable,
        SourceListFailureReason.Unknown -> ImportLoadState.UnknownError
    }
}

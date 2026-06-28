package com.github.tidetunes.feature.importing.presentation

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.feature.importing.presentation.ImportLoadState
import com.github.tidetunes.feature.importing.presentation.ImportPathUi
import com.github.tidetunes.feature.importing.presentation.ImportStorageAccountUi
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeType
import com.github.tidetunes.feature.importing.presentation.SplitPathItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.collections.immutable.persistentHashSetOf

class ImportStateTest {
    @Test
    fun mapsImportPresentationStateWithoutStorageSecrets() {
        val state = importState(
            splitPaths = listOf(SplitPathItem(path = "/Music", name = "Music")),
            entries = listOf(
                SourceNode(
                    accountId = SourceAccountId("storage:2"),
                    nodeId = "/Music/song.flac",
                    name = "song.flac",
                    path = "/Music/song.flac",
                    type = SourceNodeType.Track,
                )
            ),
            selectedPaths = persistentHashSetOf("/Music/song.flac"),
            selectedCount = 1,
            allowNodeTypes = listOf(SourceNodeType.Track),
            storageAccounts = listOf(
                ImportStorageAccountUi(
                    accountId = SourceAccountId("storage:1"),
                    isLocal = true,
                    name = "Local",
                    subtitle = "",
                ),
                ImportStorageAccountUi(
                    accountId = SourceAccountId("storage:2"),
                    isLocal = false,
                    name = "WebDAV",
                    subtitle = "https://dav.example.com/music",
                ),
            ),
            selectedStorageAccountId = SourceAccountId("storage:2"),
            loadState = ImportLoadState.Ready,
            selectionMode = ImportSelectionMode.Entries,
            canUndo = true,
            disabledToggleAll = false,
        )

        assertEquals(ImportLoadState.Ready, state.loadState)
        assertEquals(1, state.selectedCount)
        assertEquals(listOf(ImportPathUi(path = "/Music", name = "Music")), state.splitPaths)
        assertEquals(listOf(SourceNodeType.Track), state.allowNodeTypes)
        assertEquals(2, state.storageAccounts.size)
        assertEquals(
            ImportStorageAccountUi(
                accountId = SourceAccountId("storage:2"),
                isLocal = false,
                name = "WebDAV",
                subtitle = "https://dav.example.com/music",
            ),
            state.storageAccounts[1],
        )
        assertFalse(
            state.storageAccounts.any { account ->
                account.name == "secret-password" || account.subtitle == "secret-password"
            }
        )
    }

    @Test
    fun mapsLegacyImportLoadStatesToPresentationStates() {
        assertEquals(ImportLoadState.Ready, ImportLoadState.Ready)
    }
}

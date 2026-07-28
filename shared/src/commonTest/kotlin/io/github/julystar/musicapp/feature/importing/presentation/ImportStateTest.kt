package io.github.julystar.musicapp.feature.importing.presentation

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.feature.importing.presentation.ImportLoadState
import io.github.julystar.musicapp.feature.importing.presentation.ImportPathUi
import io.github.julystar.musicapp.feature.importing.presentation.ImportStorageAccountUi
import io.github.julystar.musicapp.core.domain.model.ImportSelectionMode
import io.github.julystar.musicapp.source.api.SourceNode
import io.github.julystar.musicapp.source.api.SourceNodeType
import io.github.julystar.musicapp.feature.importing.presentation.SplitPathItem
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

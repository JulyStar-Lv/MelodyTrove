package com.github.tidetune.singleton

import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.tidetune_core.StorageEntryType
import uniffi.tidetune_core.StorageId

class ImportRepositoryTest {
    @Test
    fun currentDirectoryImportUsesDirectoryModeAndCallback() {
        val repository = ImportRepository()
        var selected: Triple<Long, String, String?>? = null

        repository.prepareCurrentDirectory { storageId, path, remoteId ->
            selected = Triple(storageId.value, path, remoteId)
        }

        assertEquals(ImportSelectionMode.CurrentDirectory, repository.selectionMode.value)
        assertEquals(emptyList(), repository.allowTypes.value)

        repository.onFinishCurrentDirectory(StorageId(9), "/Music", "folder-id")

        assertEquals(Triple(9L, "/Music", "folder-id"), selected)
    }

    @Test
    fun entryImportResetsDirectoryMode() {
        val repository = ImportRepository()

        repository.prepareCurrentDirectory { _, _, _ -> }
        repository.prepare(listOf(StorageEntryType.MUSIC)) {}

        assertEquals(ImportSelectionMode.Entries, repository.selectionMode.value)
        assertEquals(listOf(StorageEntryType.MUSIC), repository.allowTypes.value)
    }
}

package com.github.tidetunes.service.librarysync.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.ImportJobEntity
import com.github.tidetunes.database.ImportJobWithFolder
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomLibrarySyncTaskRepositoryTest {
    @Test
    fun mapsImportJobWithFolderToLibrarySyncTask() {
        val task = jobWithFolder(
            status = "COMPLETED_WITH_ERRORS",
            errorMessage = "",
        ).toLibrarySyncTask()

        assertEquals("scan-1", task.id)
        assertEquals(SourceAccountId("storage:42"), task.accountId)
        assertEquals(7L, task.selectedFolderId)
        assertEquals("folder-42", task.selectedFolderRemoteId)
        assertEquals("/Music", task.folderPath)
        assertEquals("Music", task.folderDisplayPath)
        assertEquals(LibrarySyncStatus.CompletedWithErrors, task.status)
        assertEquals(10, task.scannedCount)
        assertEquals(8, task.importedCount)
        assertEquals(2, task.skippedCount)
        assertEquals(1, task.failedCount)
        assertEquals("/Music/A.flac", task.checkpoint)
        assertEquals(null, task.errorMessage)
        assertTrue(task.hasError)
    }

    @Test
    fun mapsUnknownPersistedStatusToUnknownDomainStatus() {
        assertEquals(
            LibrarySyncStatus.Unknown,
            "LEGACY_UNKNOWN".toLibrarySyncStatus(),
        )
    }

    @Test
    fun mapsPausedPersistedStatusToPausedDomainStatus() {
        assertEquals(
            LibrarySyncStatus.Paused,
            "PAUSED".toLibrarySyncStatus(),
        )
    }

    private fun jobWithFolder(
        status: String,
        errorMessage: String?,
    ): ImportJobWithFolder {
        return ImportJobWithFolder(
            job = ImportJobEntity(
                id = "scan-1",
                libraryRootId = 7,
                status = status,
                scannedCount = 10,
                importedCount = 8,
                skippedCount = 2,
                failedCount = 1,
                checkpoint = "/Music/A.flac",
                errorMessage = errorMessage,
                createdAt = 100,
                updatedAt = 200,
            ),
            sourceAccountId = 42,
            providerRootId = "folder-42",
            canonicalPath = "/Music",
            displayName = "Music",
        )
    }
}

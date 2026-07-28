package io.github.julystar.musicapp.service.librarysync.data

import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import io.github.julystar.musicapp.core.domain.model.DuplicateTrackPolicy
import io.github.julystar.musicapp.core.domain.model.MetadataScanMode
import io.github.julystar.musicapp.core.domain.model.MissingFilePolicy
import io.github.julystar.musicapp.database.ImportJobEntity
import io.github.julystar.musicapp.database.ImportJobWithFolder
import io.github.julystar.musicapp.service.librarysync.domain.LibrarySyncStatus
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
        assertEquals(11, task.scannedCount)
        assertEquals(8, task.importedCount)
        assertEquals(2, task.skippedCount)
        assertEquals(1, task.failedCount)
        assertEquals(11, task.processedCount)
        assertEquals(0, task.pendingCount)
        assertEquals(10, task.successfulCount)
        assertEquals("/Music/A.flac", task.checkpoint)
        assertEquals(null, task.errorMessage)
        assertEquals(MetadataScanMode.Standard, task.metadataScanMode)
        assertEquals(3u, task.metadataConcurrency)
        assertEquals(40, task.importBatchSize)
        assertEquals(false, task.scanRules.scanSubdirectories)
        assertEquals(15_000, task.scanRules.minDurationMs)
        assertEquals(false, task.scanRules.ignoreHiddenFiles)
        assertEquals(setOf("cache", "trash"), task.scanRules.ignoredDirectoryNames)
        assertEquals(MissingFilePolicy.RemoveOnScan, task.scanRules.missingFilePolicy)
        assertEquals(DuplicateTrackPolicy.KeepAll, task.scanRules.duplicateTrackPolicy)
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
                scannedCount = 11,
                importedCount = 8,
                skippedCount = 2,
                failedCount = 1,
                checkpoint = "/Music/A.flac",
                errorMessage = errorMessage,
                createdAt = 100,
                updatedAt = 200,
                metadataScanMode = "Standard",
                metadataConcurrency = 3,
                importBatchSize = 40,
                scanSubdirectories = false,
                ignoreShortAudio = true,
                minDurationMs = 15_000,
                ignoreHiddenFiles = false,
                ignoredDirectoryNames = "cache|trash",
                missingFilePolicy = "RemoveOnScan",
                duplicateTrackPolicy = "KeepAll",
            ),
            sourceAccountId = 42,
            providerRootId = "folder-42",
            canonicalPath = "/Music",
            displayName = "Music",
        )
    }
}

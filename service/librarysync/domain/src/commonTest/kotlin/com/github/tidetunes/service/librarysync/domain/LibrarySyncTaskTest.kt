package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LibrarySyncTaskTest {
    @Test
    fun runningQueuedAndPausedStatusesAreActive() {
        assertTrue(LibrarySyncStatus.Queued.isActive)
        assertTrue(LibrarySyncStatus.Running.isActive)
        assertTrue(LibrarySyncStatus.Paused.isActive)
        assertFalse(LibrarySyncStatus.Completed.isActive)
        assertFalse(LibrarySyncStatus.CompletedWithErrors.isActive)
        assertFalse(LibrarySyncStatus.Failed.isActive)
        assertFalse(LibrarySyncStatus.Cancelled.isActive)
        assertFalse(LibrarySyncStatus.Unknown.isActive)
    }

    @Test
    fun taskFlagsErrorsFromFailedCountOrMessage() {
        assertTrue(task(failedCount = 1, errorMessage = null).hasError)
        assertTrue(task(failedCount = 0, errorMessage = "timeout").hasError)
        assertFalse(task(failedCount = 0, errorMessage = null).hasError)
    }

    @Test
    fun taskExposesDerivedProgressCountsForUi() {
        val task = task(
            scannedCount = 500,
            importedCount = 320,
            skippedCount = 120,
            failedCount = 5,
        )

        assertEquals(445, task.processedCount)
        assertEquals(55, task.pendingCount)
        assertEquals(440, task.successfulCount)
        assertTrue(task.hasProgress)
    }

    @Test
    fun taskFlagsResumeAndRetryStates() {
        assertTrue(task(status = LibrarySyncStatus.Paused).canResume)
        assertFalse(task(status = LibrarySyncStatus.Running).canResume)

        assertTrue(task(status = LibrarySyncStatus.Failed).canRetry)
        assertTrue(task(status = LibrarySyncStatus.Cancelled).canRetry)
        assertTrue(task(status = LibrarySyncStatus.CompletedWithErrors).canRetry)
        assertFalse(task(status = LibrarySyncStatus.Completed).canRetry)
    }

    @Test
    fun taskRejectsInvalidCountersAndPaths() {
        assertFailsWith<IllegalArgumentException> {
            task(id = "")
        }
        assertFailsWith<IllegalArgumentException> {
            task(folderPath = "")
        }
        assertFailsWith<IllegalArgumentException> {
            task(scannedCount = -1)
        }
    }

    private fun task(
        id: String = "scan-1",
        folderPath: String = "/Music",
        status: LibrarySyncStatus = LibrarySyncStatus.Running,
        scannedCount: Long = 1,
        importedCount: Long = 1,
        skippedCount: Long = 0,
        failedCount: Long = 0,
        errorMessage: String? = null,
    ): LibrarySyncTask {
        return LibrarySyncTask(
            id = id,
            accountId = SourceAccountId("storage:42"),
            selectedFolderId = 7,
            selectedFolderRemoteId = "folder-42",
            folderPath = folderPath,
            folderDisplayPath = folderPath,
            status = status,
            scannedCount = scannedCount,
            importedCount = importedCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            checkpoint = null,
            errorMessage = errorMessage,
            createdAtEpochMs = 100,
            updatedAtEpochMs = 200,
        )
    }
}

package com.github.tidetune.singleton

import com.github.tidetune.database.ImportJobEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImportStatusRepositoryTest {
    @Test
    fun mapsImportJobStatsForUi() {
        val item = job(
            status = "COMPLETED",
            failedCount = 0,
            errorMessage = "",
        ).toImportStatusItem()

        assertEquals("job-1", item.id)
        assertEquals("COMPLETED", item.status)
        assertEquals(10, item.scannedCount)
        assertEquals(8, item.importedCount)
        assertEquals(2, item.skippedCount)
        assertEquals(0, item.failedCount)
        assertEquals("/Music", item.checkpoint)
        assertFalse(item.hasError)
        assertFalse(item.isActive)
    }

    @Test
    fun flagsFailureWhenFailedCountOrMessageExists() {
        assertTrue(job(failedCount = 1, errorMessage = null).toImportStatusItem().hasError)
        assertTrue(job(failedCount = 0, errorMessage = "timeout").toImportStatusItem().hasError)
    }

    @Test
    fun marksRunningJobAsActive() {
        val item = job(
            status = "RUNNING",
            failedCount = 0,
            errorMessage = null,
        ).toImportStatusItem()

        assertTrue(item.isActive)
    }

    private fun job(
        status: String = "FAILED",
        failedCount: Long,
        errorMessage: String?,
    ) = ImportJobEntity(
        id = "job-1",
        selectedFolderId = 7,
        status = status,
        scannedCount = 10,
        importedCount = 8,
        skippedCount = 2,
        failedCount = failedCount,
        checkpoint = "/Music",
        errorMessage = errorMessage,
        createdAt = 100,
        updatedAt = 200,
    )
}

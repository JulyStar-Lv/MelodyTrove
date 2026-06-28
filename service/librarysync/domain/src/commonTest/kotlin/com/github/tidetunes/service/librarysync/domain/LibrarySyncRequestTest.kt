package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LibrarySyncRequestTest {
    @Test
    fun requestKeepsBoundedDefaultsAlignedWithLegacyImporter() {
        val request = request()

        assertEquals(4u, request.metadataConcurrency)
        assertEquals(DEFAULT_LIBRARY_SYNC_BATCH_SIZE, request.importBatchSize)
    }

    @Test
    fun requestRejectsInvalidPathScanIdAndImportSettings() {
        assertFailsWith<IllegalArgumentException> {
            request(selectedFolderCanonicalPath = "")
        }
        assertFailsWith<IllegalArgumentException> {
            request(scanId = "")
        }
        assertFailsWith<IllegalArgumentException> {
            request(metadataConcurrency = 0u)
        }
        assertFailsWith<IllegalArgumentException> {
            request(metadataConcurrency = 17u)
        }
        assertFailsWith<IllegalArgumentException> {
            request(importBatchSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            request(importBatchSize = 501)
        }
    }

    private fun request(
        selectedFolderCanonicalPath: String = "/Music",
        scanId: String? = null,
        metadataConcurrency: UInt = 4u,
        importBatchSize: Int = DEFAULT_LIBRARY_SYNC_BATCH_SIZE,
    ): LibrarySyncRequest {
        return LibrarySyncRequest(
            accountId = SourceAccountId("storage:42"),
            selectedFolderRemoteId = "folder-42",
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
    }
}

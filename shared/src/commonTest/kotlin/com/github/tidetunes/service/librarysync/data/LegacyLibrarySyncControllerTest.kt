package com.github.tidetunes.service.librarysync.data

import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.domain.importing.RemoteLibraryImportResult
import com.github.tidetunes.service.librarysync.domain.DEFAULT_LIBRARY_SYNC_BATCH_SIZE
import com.github.tidetunes.service.librarysync.domain.DEFAULT_LIBRARY_SYNC_METADATA_CONCURRENCY
import com.github.tidetunes.service.librarysync.domain.LibrarySyncRequest
import com.github.tidetunes.service.librarysync.domain.LibrarySyncStatus
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTask
import com.github.tidetunes.service.librarysync.domain.LibrarySyncTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType

class LegacyLibrarySyncControllerTest {
    @Test
    fun webDavStorageUsesLegacyFolderScan() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
        )

        val result = controller.syncFolder(
            request(
                selectedFolderRemoteId = null,
                scanId = "scan-42",
                metadataConcurrency = 2u,
                importBatchSize = 25,
            )
        )

        assertEquals(1, importer.scanCalls.size)
        assertEquals(emptyList(), importer.oneDriveCalls)
        assertEquals(42L, importer.scanCalls.single().storageId)
        assertEquals(null, importer.scanCalls.single().selectedFolderRemoteId)
        assertEquals("/Music", importer.scanCalls.single().selectedFolderCanonicalPath)
        assertEquals("scan-42", importer.scanCalls.single().scanId)
        assertEquals(2u, importer.scanCalls.single().metadataConcurrency)
        assertEquals(25, importer.scanCalls.single().importBatchSize)
        assertEquals(7L, result.importedCount)
    }

    @Test
    fun localStorageUsesLegacyFolderScan() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.LOCAL),
        )

        controller.syncFolder(request(selectedFolderRemoteId = null))

        assertEquals(1, importer.scanCalls.size)
        assertEquals(emptyList(), importer.oneDriveCalls)
        assertEquals(42L, importer.scanCalls.single().storageId)
    }

    @Test
    fun oneDriveStorageUsesLegacyIncrementalSync() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.ONE_DRIVE),
        )

        val result = controller.syncFolder(
            request(
                selectedFolderRemoteId = "drive-item-42",
                selectedFolderDisplayPath = "Music",
            )
        )

        assertEquals(emptyList(), importer.scanCalls)
        assertEquals(1, importer.oneDriveCalls.size)
        assertEquals(42L, importer.oneDriveCalls.single().storageId)
        assertEquals("drive-item-42", importer.oneDriveCalls.single().selectedFolderRemoteId)
        assertEquals("/Music", importer.oneDriveCalls.single().selectedFolderCanonicalPath)
        assertEquals("Music", importer.oneDriveCalls.single().selectedFolderDisplayPath)
        assertEquals(7L, result.importedCount)
    }

    @Test
    fun oneDriveRequiresFolderRemoteId() = runBlocking {
        val controller = controller(
            importer = FakeLegacyLibrarySyncImporter(),
            storage = storage(id = 42, typ = StorageType.ONE_DRIVE),
        )

        assertFailsWith<IllegalArgumentException> {
            controller.syncFolder(request(selectedFolderRemoteId = null))
        }
        Unit
    }

    @Test
    fun unsupportedOrMissingStorageFailsBeforeImporting() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
        )

        assertFailsWith<IllegalStateException> {
            controller.syncFolder(request(accountId = SourceAccountId("unsupported:42")))
        }
        assertFailsWith<IllegalStateException> {
            controller.syncFolder(request(accountId = SourceAccountId("storage:99")))
        }
        assertEquals(emptyList(), importer.scanCalls)
        assertEquals(emptyList(), importer.oneDriveCalls)
    }

    @Test
    fun activeTaskForAccountBlocksBeforeImporting() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val taskRepository = FakeLibrarySyncTaskRepository(
            activeAccounts = setOf(SourceAccountId("storage:42")),
        )
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        assertFailsWith<IllegalStateException> {
            controller.syncFolder(request())
        }
        assertEquals(
            listOf(ActiveCheck(SourceAccountId("storage:42"), null)),
            taskRepository.activeChecks,
        )
        assertEquals(emptyList(), importer.scanCalls)
        assertEquals(emptyList(), importer.oneDriveCalls)
    }

    @Test
    fun cancelDelegatesToLegacyImporter() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val taskRepository = FakeLibrarySyncTaskRepository()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        assertTrue(controller.cancel("scan-42"))
        assertEquals(listOf("scan-42"), importer.cancelCalls)
        assertEquals(listOf("scan-42"), taskRepository.markCancelledCalls)
    }

    @Test
    fun pauseDelegatesToImporterAndMarksTaskPaused() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val taskRepository = FakeLibrarySyncTaskRepository()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        assertTrue(controller.pause("scan-42"))
        assertEquals(listOf("scan-42"), importer.pauseCalls)
        assertEquals(listOf("scan-42"), taskRepository.markPausedCalls)
    }

    @Test
    fun pauseDoesNotMarkPersistedTaskWhenImporterCannotPause() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter(pauseResult = false)
        val taskRepository = FakeLibrarySyncTaskRepository()
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        assertEquals(false, controller.pause("scan-42"))
        assertEquals(listOf("scan-42"), importer.pauseCalls)
        assertEquals(emptyList(), taskRepository.markPausedCalls)
    }

    @Test
    fun cancelMarksPausedPersistedTaskCancelledWhenImporterHasNoActiveSession() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter(cancelResult = false)
        val taskRepository = FakeLibrarySyncTaskRepository(
            tasksById = mapOf(
                "scan-42" to task(
                    id = "scan-42",
                    status = LibrarySyncStatus.Paused,
                )
            ),
        )
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        assertTrue(controller.cancel("scan-42"))
        assertEquals(listOf("scan-42"), importer.cancelCalls)
        assertEquals(listOf("scan-42"), taskRepository.markCancelledCalls)
    }

    @Test
    fun resumeStartsPausedTaskWithSameScanIdAndExcludesCurrentTaskFromActiveCheck() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val taskRepository = FakeLibrarySyncTaskRepository(
            tasksById = mapOf(
                "scan-42" to task(
                    id = "scan-42",
                    status = LibrarySyncStatus.Paused,
                )
            ),
        )
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        val result = controller.resume("scan-42")

        assertEquals("scan-42", result?.scanId)
        assertEquals(1, importer.scanCalls.size)
        assertEquals("scan-42", importer.scanCalls.single().scanId)
        assertEquals("folder-42", importer.scanCalls.single().selectedFolderRemoteId)
        assertEquals("/Music", importer.scanCalls.single().selectedFolderCanonicalPath)
        assertEquals(
            listOf(ActiveCheck(SourceAccountId("storage:42"), "scan-42")),
            taskRepository.activeChecks,
        )
    }

    @Test
    fun retryStartsFailedTaskWithSameScanId() = runBlocking {
        val importer = FakeLegacyLibrarySyncImporter()
        val taskRepository = FakeLibrarySyncTaskRepository(
            tasksById = mapOf(
                "scan-42" to task(
                    id = "scan-42",
                    selectedFolderRemoteId = null,
                    status = LibrarySyncStatus.Failed,
                )
            ),
        )
        val controller = controller(
            importer = importer,
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = taskRepository,
        )

        val result = controller.retry("scan-42")

        assertEquals("scan-42", result?.scanId)
        assertEquals(1, importer.scanCalls.size)
        assertEquals("scan-42", importer.scanCalls.single().scanId)
        assertEquals(null, importer.scanCalls.single().selectedFolderRemoteId)
    }

    @Test
    fun resumeAndRetryIgnoreUnsupportedTaskStates() = runBlocking {
        val controller = controller(
            importer = FakeLegacyLibrarySyncImporter(),
            storage = storage(id = 42, typ = StorageType.WEBDAV),
            taskRepository = FakeLibrarySyncTaskRepository(
                tasksById = mapOf(
                    "running" to task(id = "running", status = LibrarySyncStatus.Running),
                    "completed" to task(id = "completed", status = LibrarySyncStatus.Completed),
                ),
            ),
        )

        assertEquals(null, controller.resume("running"))
        assertEquals(null, controller.retry("completed"))
        assertEquals(null, controller.resume("missing"))
        assertEquals(null, controller.retry("missing"))
    }

    private fun controller(
        importer: FakeLegacyLibrarySyncImporter,
        storage: Storage,
        taskRepository: FakeLibrarySyncTaskRepository = FakeLibrarySyncTaskRepository(),
    ): LegacyLibrarySyncController {
        return LegacyLibrarySyncController(
            importer = importer,
            storageProvider = LegacyLibrarySyncStorageProvider { storageId ->
                storage.takeIf { it.id == storageId }
            },
            taskRepository = taskRepository,
        )
    }

    private fun request(
        accountId: SourceAccountId = SourceAccountId("storage:42"),
        selectedFolderRemoteId: String? = "folder-42",
        selectedFolderCanonicalPath: String = "/Music",
        selectedFolderDisplayPath: String? = null,
        scanId: String? = null,
        metadataConcurrency: UInt = DEFAULT_LIBRARY_SYNC_METADATA_CONCURRENCY,
        importBatchSize: Int = DEFAULT_LIBRARY_SYNC_BATCH_SIZE,
    ): LibrarySyncRequest {
        return LibrarySyncRequest(
            accountId = accountId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
    }

    private fun storage(
        id: Long,
        typ: StorageType,
    ): Storage {
        return Storage(
            id = StorageId(id),
            addr = "",
            alias = "Storage",
            username = "",
            password = "",
            isAnonymous = true,
            typ = typ,
            musicCount = 0u,
        )
    }

    private fun task(
        id: String = "scan-42",
        selectedFolderRemoteId: String? = "folder-42",
        status: LibrarySyncStatus,
    ): LibrarySyncTask {
        return LibrarySyncTask(
            id = id,
            accountId = SourceAccountId("storage:42"),
            selectedFolderId = 7,
            selectedFolderRemoteId = selectedFolderRemoteId,
            folderPath = "/Music",
            folderDisplayPath = "Music",
            status = status,
            scannedCount = 10,
            importedCount = 8,
            skippedCount = 2,
            failedCount = 1,
            checkpoint = "/Music/A.flac",
            errorMessage = null,
            createdAtEpochMs = 100,
            updatedAtEpochMs = 200,
        )
    }
}

private class FakeLibrarySyncTaskRepository(
    private val activeAccounts: Set<SourceAccountId> = emptySet(),
    private val tasksById: Map<String, LibrarySyncTask> = emptyMap(),
    private val markPausedResult: Boolean = true,
    private val markCancelledResult: Boolean = true,
) : LibrarySyncTaskRepository {
    val activeChecks = mutableListOf<ActiveCheck>()
    val markPausedCalls = mutableListOf<String>()
    val markCancelledCalls = mutableListOf<String>()
    private val tasks = MutableStateFlow(emptyList<LibrarySyncTask>())

    override fun observeRecentTasks(limit: Int): Flow<List<LibrarySyncTask>> {
        return tasks
    }

    override fun observeActiveTasks(): Flow<List<LibrarySyncTask>> {
        return tasks
    }

    override suspend fun getTask(id: String): LibrarySyncTask? {
        return tasksById[id]
    }

    override suspend fun hasActiveTask(
        accountId: SourceAccountId,
        excludingTaskId: String?,
    ): Boolean {
        activeChecks += ActiveCheck(accountId, excludingTaskId)
        return accountId in activeAccounts
    }

    override suspend fun markPaused(id: String): Boolean {
        markPausedCalls += id
        return markPausedResult
    }

    override suspend fun markCancelled(id: String): Boolean {
        markCancelledCalls += id
        return markCancelledResult
    }
}

private data class ActiveCheck(
    val accountId: SourceAccountId,
    val excludingTaskId: String?,
)

private class FakeLegacyLibrarySyncImporter(
    private val cancelResult: Boolean = true,
    private val pauseResult: Boolean = true,
) : LegacyLibrarySyncImporter {
    val cancelCalls = mutableListOf<String>()
    val pauseCalls = mutableListOf<String>()
    val scanCalls = mutableListOf<ScanCall>()
    val oneDriveCalls = mutableListOf<OneDriveCall>()

    override suspend fun cancelImport(scanId: String): Boolean {
        cancelCalls += scanId
        return cancelResult
    }

    override suspend fun pauseImport(scanId: String): Boolean {
        pauseCalls += scanId
        return pauseResult
    }

    override suspend fun syncOneDriveFolder(
        storageId: Long,
        selectedFolderRemoteId: String,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult {
        oneDriveCalls += OneDriveCall(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
        return result(scanId)
    }

    override suspend fun scanAndImportFolder(
        storageId: Long,
        selectedFolderRemoteId: String?,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String?,
        scanId: String?,
        metadataConcurrency: UInt,
        importBatchSize: Int,
    ): RemoteLibraryImportResult {
        scanCalls += ScanCall(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
        return result(scanId)
    }

    private fun result(scanId: String?): RemoteLibraryImportResult {
        return RemoteLibraryImportResult(
            scanId = scanId ?: "scan-result",
            selectedFolderId = 3,
            scannedCount = 11,
            changedCount = 9,
            skippedCount = 2,
            importedCount = 7,
            failedCount = 0,
        )
    }
}

private data class ScanCall(
    val storageId: Long,
    val selectedFolderRemoteId: String?,
    val selectedFolderCanonicalPath: String,
    val selectedFolderDisplayPath: String?,
    val scanId: String?,
    val metadataConcurrency: UInt,
    val importBatchSize: Int,
)

private data class OneDriveCall(
    val storageId: Long,
    val selectedFolderRemoteId: String,
    val selectedFolderCanonicalPath: String,
    val selectedFolderDisplayPath: String?,
    val scanId: String?,
    val metadataConcurrency: UInt,
    val importBatchSize: Int,
)

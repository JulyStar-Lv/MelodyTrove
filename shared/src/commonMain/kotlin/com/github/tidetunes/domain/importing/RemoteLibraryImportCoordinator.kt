package com.github.tidetunes.domain.importing

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.database.AlbumArtistCrossRef
import com.github.tidetunes.database.AlbumEntity
import com.github.tidetunes.database.ArtworkEntity
import com.github.tidetunes.database.ArtistEntity
import com.github.tidetunes.database.GenreEntity
import com.github.tidetunes.database.ImportJobEntity
import com.github.tidetunes.database.LibraryRootEntity
import com.github.tidetunes.database.LyricsEntity
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.ProviderTypes
import com.github.tidetunes.database.RawMetadataEntity
import com.github.tidetunes.database.SourceAccountEntity
import com.github.tidetunes.database.SourceItemEntity
import com.github.tidetunes.database.SourceItemTypes
import com.github.tidetunes.database.SourceSyncCursorEntity
import com.github.tidetunes.database.SyncDao
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackArtistCrossRef
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.database.TrackGenreCrossRef
import com.github.tidetunes.database.TrackSourceRefEntity
import com.github.tidetunes.platform.currentTimeMillis
import com.github.tidetunes.source.storage.MetadataRepository
import com.github.tidetunes.source.storage.RemoteScannerRepository
import com.github.tidetunes.core.data.StorageRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uniffi.tidetunes_core.RemoteArtwork
import uniffi.tidetunes_core.RemoteMetadata
import uniffi.tidetunes_core.RemoteMusicScanSession
import uniffi.tidetunes_core.OneDriveDeltaItem
import uniffi.tidetunes_core.OneDriveDeltaPageResult
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageId

data class RemoteLibraryImportRequest(
    val storageId: Long,
    val selectedFolderRemoteId: String?,
    val selectedFolderCanonicalPath: String,
    val selectedFolderDisplayPath: String? = null,
    val entries: List<StorageEntry>,
    val scanId: String? = null,
    val metadataConcurrency: UInt = 4u,
    val importBatchSize: Int = DEFAULT_IMPORT_BATCH_SIZE,
)

data class RemoteLibraryImportResult(
    val scanId: String,
    val selectedFolderId: Long,
    val scannedCount: Long,
    val changedCount: Long,
    val skippedCount: Long,
    val importedCount: Long,
    val failedCount: Long,
)

class RemoteLibraryImportCoordinator(
    private val database: TideTunesDatabase,
    private val trackDao: TrackDao,
    private val metadataDao: MetadataDao,
    private val syncDao: SyncDao,
    private val metadataRepository: MetadataRepository,
    private val remoteScannerRepository: RemoteScannerRepository,
    private val storageRepository: StorageRepositoryImpl,
) {
    private val activeOperationsMutex = Mutex()
    private val activeOperations = mutableMapOf<String, ActiveImportOperation>()

    suspend fun cancelImport(scanId: String): Boolean {
        val operation = activeOperationsMutex.withLock { activeOperations[scanId] }
        operation?.cancel()
        return operation != null
    }

    suspend fun pauseImport(scanId: String): Boolean {
        val operation = activeOperationsMutex.withLock { activeOperations[scanId] }
        operation?.pause()
        return operation != null
    }

    suspend fun scanAndInitializeOneDriveFolder(
        storageId: Long,
        selectedFolderRemoteId: String,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String? = null,
        scanId: String? = null,
        metadataConcurrency: UInt = 4u,
        importBatchSize: Int = DEFAULT_IMPORT_BATCH_SIZE,
    ): RemoteLibraryImportResult {
        validateImportSettings(metadataConcurrency, importBatchSize)
        val request = RemoteLibraryImportRequest(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            entries = emptyList(),
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
        val (execution, operation) = startTrackedImport(request)
        var currentJob = execution.job

        return try {
            operation.throwIfStopRequested()
            val delta = readOneDriveDelta(
                storageId = storageId,
                rootRemoteId = selectedFolderRemoteId,
                cursor = null,
                latestOnly = false,
                operation = operation,
            )
            check(!delta.resyncRequired && delta.deltaLink != null) {
                "OneDrive did not return a complete initial delta snapshot"
            }
            check(!requiresOneDriveResync(delta.items)) {
                "OneDrive initial delta contained a file without a canonical path"
            }
            val snapshotRequest = request.copy(
                entries = delta.items
                    .asSequence()
                    .filter { !it.deleted && it.isSupportedMusicFile() }
                    .map { it.toStorageEntry(storageId) }
                    .toList(),
            )
            val result = runCompleteSnapshotImport(
                request = snapshotRequest,
                deltaLink = delta.deltaLink,
                execution = execution,
                operation = operation,
                currentJob = currentJob,
            )
            result.second.also { currentJob = it }
            result.first
        } catch (error: Throwable) {
            markImportStopOrFailure(
                error = error,
                operation = operation,
                root = execution.libraryRoot,
                job = currentJob,
            )
            throw error
        } finally {
            unregisterActiveOperation(execution, operation)
        }
    }

    suspend fun syncOneDriveFolder(
        storageId: Long,
        selectedFolderRemoteId: String,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String? = null,
        scanId: String? = null,
        metadataConcurrency: UInt = 4u,
        importBatchSize: Int = DEFAULT_IMPORT_BATCH_SIZE,
    ): RemoteLibraryImportResult {
        validateImportSettings(metadataConcurrency, importBatchSize)
        val canonicalPath = normalizeRemotePath(selectedFolderCanonicalPath)
        val root = database.libraryRootDao().findByPath(storageId, canonicalPath)
            ?: return scanAndInitializeOneDriveFolder(
                storageId = storageId,
                selectedFolderRemoteId = selectedFolderRemoteId,
                selectedFolderCanonicalPath = canonicalPath,
                selectedFolderDisplayPath = selectedFolderDisplayPath,
                scanId = scanId,
                metadataConcurrency = metadataConcurrency,
                importBatchSize = importBatchSize,
            )
        val cursor = syncDao.getCursor(root.id)?.cursorValue ?: root.syncCursor
            ?: return scanAndInitializeOneDriveFolder(
                storageId = storageId,
                selectedFolderRemoteId = selectedFolderRemoteId,
                selectedFolderCanonicalPath = canonicalPath,
                selectedFolderDisplayPath = selectedFolderDisplayPath,
                scanId = scanId,
                metadataConcurrency = metadataConcurrency,
                importBatchSize = importBatchSize,
            )
        val request = RemoteLibraryImportRequest(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = canonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            entries = emptyList(),
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
        val (execution, operation) = startTrackedImport(request)
        var currentJob = execution.job
        var changedCount = 0L
        return try {
            operation.throwIfStopRequested()
            val delta = readOneDriveDelta(
                storageId = storageId,
                rootRemoteId = selectedFolderRemoteId,
                cursor = cursor,
                latestOnly = false,
                operation = operation,
            )
            if (delta.resyncRequired || requiresOneDriveResync(delta.items)) {
                val snapshot = readOneDriveDelta(
                    storageId = storageId,
                    rootRemoteId = selectedFolderRemoteId,
                    cursor = null,
                    latestOnly = false,
                    operation = operation,
                )
                check(!snapshot.resyncRequired && snapshot.deltaLink != null) {
                    "OneDrive did not return a complete resync delta snapshot"
                }
                check(!requiresOneDriveResync(snapshot.items)) {
                    "OneDrive resync delta contained a file without a canonical path"
                }
                val snapshotRequest = request.copy(
                    entries = snapshot.items
                        .asSequence()
                        .filter { !it.deleted && it.isSupportedMusicFile() }
                        .map { it.toStorageEntry(storageId) }
                        .toList(),
                )
                val result = runCompleteSnapshotImport(
                    request = snapshotRequest,
                    deltaLink = snapshot.deltaLink,
                    execution = execution,
                    operation = operation,
                    currentJob = currentJob,
                )
                currentJob = result.second
                return result.first
            }
            val nextDeltaLink = requireNotNull(delta.deltaLink) {
                "OneDrive delta pagination completed without a deltaLink"
            }
            operation.throwIfStopRequested()
            val existingByRemoteId = delta.items
                .map { it.remoteId }
                .distinct()
                .chunked(MAX_REMOTE_ID_QUERY_SIZE)
                .flatMap { database.sourceItemDao().findByProviderItemIds(storageId, it) }
                .mapNotNull { item -> item.providerItemId?.let { it to item } }
                .toMap()
            operation.throwIfStopRequested()
            val deletedRemoteIds = delta.items.mapNotNull { item ->
                val existing = existingByRemoteId[item.remoteId]
                if (item.deleted || (existing != null && !item.isSupportedMusicFile())) {
                    item.remoteId
                } else {
                    null
                }
            }.distinct()
            if (deletedRemoteIds.isNotEmpty()) {
                val now = currentTimeMillis()
                val deletedCount = deletedRemoteIds
                    .chunked(MAX_REMOTE_ID_QUERY_SIZE)
                    .sumOf {
                        database.sourceItemDao().markDeletedByProviderItemIds(storageId, it, now)
                    }
                val deletedSourceItemIds = deletedRemoteIds
                    .chunked(MAX_REMOTE_ID_QUERY_SIZE)
                    .flatMap { ids ->
                        database.sourceItemDao()
                            .findByProviderItemIds(storageId, ids)
                    }
                    .map { it.id }
                if (deletedSourceItemIds.isNotEmpty()) {
                    database.trackSourceRefDao()
                        .markUnavailableBySourceItemIds(deletedSourceItemIds, now)
                }
                changedCount += deletedCount
                currentJob = currentJob.copy(
                    scannedCount = currentJob.scannedCount + deletedRemoteIds.size,
                    updatedAt = now,
                )
                syncDao.upsertJob(currentJob)
            }
            operation.throwIfStopRequested()

            val entries = delta.items
                .asSequence()
                .filter { !it.deleted && it.isSupportedMusicFile() }
                .map { it.toStorageEntry(storageId) }
                .toList()
            entries.chunked(importBatchSize).forEach { batch ->
                operation.throwIfStopRequested()
                val batchResult = importBatch(
                    request = request,
                    execution = execution,
                    currentJob = currentJob,
                    entries = batch,
                )
                currentJob = batchResult.job
                changedCount += batchResult.changedCount
                operation.throwIfStopRequested()
            }
            operation.throwIfStopRequested()
            currentJob = completeDeltaImport(
                execution = execution,
                currentJob = currentJob,
                deltaLink = nextDeltaLink,
            )
            importResult(execution, currentJob, changedCount)
        } catch (error: Throwable) {
            markImportStopOrFailure(
                error = error,
                operation = operation,
                root = execution.libraryRoot,
                job = currentJob,
            )
            throw error
        } finally {
            unregisterActiveOperation(execution, operation)
        }
    }

    suspend fun scanAndImportFolder(
        storageId: Long,
        selectedFolderRemoteId: String?,
        selectedFolderCanonicalPath: String,
        selectedFolderDisplayPath: String? = null,
        scanId: String? = null,
        metadataConcurrency: UInt = 4u,
        importBatchSize: Int = DEFAULT_IMPORT_BATCH_SIZE,
        deltaLink: String? = null,
    ): RemoteLibraryImportResult {
        validateImportSettings(metadataConcurrency, importBatchSize)
        val request = RemoteLibraryImportRequest(
            storageId = storageId,
            selectedFolderRemoteId = selectedFolderRemoteId,
            selectedFolderCanonicalPath = selectedFolderCanonicalPath,
            selectedFolderDisplayPath = selectedFolderDisplayPath,
            entries = emptyList(),
            scanId = scanId,
            metadataConcurrency = metadataConcurrency,
            importBatchSize = importBatchSize,
        )
        val (execution, operation) = startTrackedImport(request)
        var currentJob = execution.job
        var scanSession: RemoteMusicScanSession? = null

        return try {
            val previousCursor = syncDao.getCursor(execution.libraryRoot.id)
            val seenPaths = mutableSetOf<String>()
            var changedCount = 0L
            operation.throwIfStopRequested()
            val session = remoteScannerRepository.startMusicFolderScan(
                storageId = StorageId(storageId),
                path = selectedFolderCanonicalPath,
            )
            scanSession = session
            operation.attachScanSession(session)
            while (true) {
                operation.throwIfStopRequested()
                val scanBatch = session.nextBatch(importBatchSize.toUInt())
                if (scanBatch.cancelled) {
                    throw ImportCancelledException()
                }
                operation.throwIfStopRequested()
                val entries = prepareMusicEntries(storageId, scanBatch.entries)
                    .filter { seenPaths.add(normalizeRemotePath(it.path)) }
                if (entries.isNotEmpty()) {
                    val batchResult = importBatch(
                        request = request,
                        execution = execution,
                        currentJob = currentJob,
                        entries = entries,
                    )
                    currentJob = batchResult.job
                    changedCount += batchResult.changedCount
                    operation.throwIfStopRequested()
                }
                if (scanBatch.done) break
            }
            if (session.isCancelled()) {
                throw ImportCancelledException()
            }
            operation.throwIfStopRequested()

            currentJob = completeImport(
                execution = execution,
                previousCursor = previousCursor,
                currentJob = currentJob,
                deltaLink = deltaLink ?: execution.libraryRoot.syncCursor,
            )
            importResult(execution, currentJob, changedCount)
        } catch (error: Throwable) {
            markImportStopOrFailure(
                error = error,
                operation = operation,
                root = execution.libraryRoot,
                job = currentJob,
            )
            throw error
        } finally {
            withContext(NonCancellable) {
                unregisterActiveOperation(execution, operation)
                scanSession?.cancel()
                scanSession?.close()
            }
        }
    }

    /**
     * Imports a complete snapshot for one selected library folder.
     *
     * The caller must pass every current music file under the selected folder. Files
     * already in Room but missing from this snapshot are marked deleted.
     */
    suspend fun importCompleteSnapshot(
        request: RemoteLibraryImportRequest,
        deltaLink: String? = null,
    ): RemoteLibraryImportResult {
        validateImportSettings(request.metadataConcurrency, request.importBatchSize)
        val (execution, operation) = startTrackedImport(request)
        var currentJob = execution.job

        return try {
            val result = runCompleteSnapshotImport(
                request = request,
                deltaLink = deltaLink,
                execution = execution,
                operation = operation,
                currentJob = currentJob,
            )
            currentJob = result.second
            result.first
        } catch (error: Throwable) {
            markImportStopOrFailure(
                error = error,
                operation = operation,
                root = execution.libraryRoot,
                job = currentJob,
            )
            throw error
        } finally {
            unregisterActiveOperation(execution, operation)
        }
    }

    private suspend fun runCompleteSnapshotImport(
        request: RemoteLibraryImportRequest,
        deltaLink: String?,
        execution: ImportExecution,
        operation: ActiveImportOperation,
        currentJob: ImportJobEntity,
    ): Pair<RemoteLibraryImportResult, ImportJobEntity> {
        operation.throwIfStopRequested()
        val previousCursor = syncDao.getCursor(execution.libraryRoot.id)
        operation.throwIfStopRequested()
        val musicEntries = prepareMusicEntries(request.storageId, request.entries)
        var changedCount = 0L
        var job = currentJob
        musicEntries.chunked(request.importBatchSize).forEach { batch ->
            operation.throwIfStopRequested()
            val batchResult = importBatch(
                request = request,
                execution = execution,
                currentJob = job,
                entries = batch,
            )
            job = batchResult.job
            changedCount += batchResult.changedCount
            operation.throwIfStopRequested()
        }

        operation.throwIfStopRequested()
        job = completeImport(
            execution = execution,
            previousCursor = previousCursor,
            currentJob = job,
            deltaLink = deltaLink ?: execution.libraryRoot.syncCursor,
        )
        return importResult(execution, job, changedCount) to job
    }

    private suspend fun importBatch(
        request: RemoteLibraryImportRequest,
        execution: ImportExecution,
        currentJob: ImportJobEntity,
        entries: List<StorageEntry>,
    ): ImportBatchResult {
        val now = currentTimeMillis()
        val batchPaths = entries.map { normalizeRemotePath(it.path) }
        val existing = database.sourceItemDao()
            .findByPaths(request.storageId, batchPaths)
            .associateBy { it.canonicalPath }
        val remoteIds = entries.mapNotNull { it.remoteId }.distinct()
        val existingByRemoteId = if (remoteIds.isEmpty()) {
            emptyMap()
        } else {
            database.sourceItemDao()
                .findByProviderItemIds(request.storageId, remoteIds)
                .mapNotNull { item -> item.providerItemId?.let { it to item } }
                .toMap()
        }
        val plan = planRemoteLibraryImport(
            storageId = request.storageId,
            libraryRootId = execution.libraryRoot.id,
            scanId = execution.scanId,
            now = now,
            entries = entries,
            existing = existing,
            existingByRemoteId = existingByRemoteId,
        )
        val metadataResults = if (plan.metadataEntries.isEmpty()) {
            emptyList()
        } else {
            metadataRepository.readBatch(
                entries = plan.metadataEntries,
                concurrency = request.metadataConcurrency,
            )
        }
        val metadataByPath = metadataResults
            .mapNotNull { result ->
                val metadata = result.metadata ?: return@mapNotNull null
                normalizeRemotePath(result.entry.path) to metadata
            }
            .toMap()
        val metadataReturnedPaths = metadataResults
            .map { normalizeRemotePath(it.entry.path) }
            .toSet()
        val batchFailedCount = plan.unreadableChangedCount +
            plan.metadataEntries.count {
                normalizeRemotePath(it.path) !in metadataReturnedPaths
            } +
            metadataResults.count { it.metadata == null }
        lateinit var updatedJob: ImportJobEntity

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                database.sourceItemDao().applyScanBatch(
                    changedItems = plan.changedItems,
                    unchangedIds = plan.unchangedFileIds,
                    scanId = execution.scanId,
                    now = now,
                )
                val sourceRows = if (plan.changedItems.isEmpty()) {
                    emptyMap()
                } else {
                    database.sourceItemDao()
                        .findByPaths(
                            request.storageId,
                            plan.changedItems.mapNotNull { it.canonicalPath },
                        )
                        .associateBy { it.canonicalPath }
                }
                val trackMetadata = plan.changedEntries.mapNotNull { entry ->
                    val path = normalizeRemotePath(entry.path)
                    val metadata = metadataByPath[path] ?: return@mapNotNull null
                    val sourceItem = sourceRows[path] ?: return@mapNotNull null
                    SourceImportRow(entry, metadata, sourceItem)
                }
                val albumsByName = ensureAlbums(trackMetadata.map { it.metadata })
                val artistsByName = ensureArtists(trackMetadata.map { it.metadata })
                val genresByName = ensureGenres(trackMetadata.map { it.metadata })
                val existingRefsBySourceItemId = if (trackMetadata.isEmpty()) {
                    emptyMap()
                } else {
                    database.trackSourceRefDao()
                        .findBySourceItemIds(trackMetadata.map { it.sourceItem.id })
                        .associateBy { it.sourceItemId }
                }
                val existingTracksById = if (existingRefsBySourceItemId.isEmpty()) {
                    emptyMap()
                } else {
                    trackDao.findByIds(existingRefsBySourceItemId.values.map { it.trackId })
                        .associateBy { it.id }
                }
                val trackContexts = trackMetadata.map { row ->
                    val entry = row.entry
                    val metadata = row.metadata
                    val sourceItem = row.sourceItem
                    val existingTrack = existingRefsBySourceItemId[sourceItem.id]
                        ?.let { existingTracksById[it.trackId] }
                        ?: findCanonicalTrack(metadata, sourceItem)
                    val track = buildTrackEntity(
                        entry = entry,
                        metadata = metadata,
                        sourceItem = sourceItem,
                        now = now,
                        existingTrack = existingTrack,
                        albumId = metadata.album
                            ?.let(::normalizeMetadataName)
                            ?.let(albumsByName::get)
                            ?.id,
                    )
                    TrackMetadataContext(track, metadata, sourceItem)
                }
                val tracks = trackContexts.map { it.track }
                trackDao.upsertAll(tracks)
                val sourceRefs = trackContexts.map { context ->
                    buildTrackSourceRefEntity(
                        track = context.track,
                        sourceItem = context.sourceItem,
                        metadata = context.metadata,
                        now = now,
                        existingRef = existingRefsBySourceItemId[context.sourceItem.id],
                    )
                }
                if (sourceRefs.isNotEmpty()) {
                    database.trackSourceRefDao().upsertAll(sourceRefs)
                }
                val trackIds = tracks.map { it.id }
                if (trackIds.isNotEmpty()) {
                    metadataDao.deleteTrackArtistsForTracks(trackIds)
                    metadataDao.deleteTrackGenresForTracks(trackIds)
                    metadataDao.deleteLyricsForTracks(trackIds)
                    metadataDao.deleteRawMetadataForTracks(trackIds)
                }
                val trackArtists = trackContexts.flatMap { context ->
                    context.metadata.trackArtists().mapIndexedNotNull { position, name ->
                        artistsByName[normalizeMetadataName(name)]?.let { artist ->
                            TrackArtistCrossRef(
                                trackId = context.track.id,
                                artistId = artist.id,
                                position = position,
                            )
                        }
                    }
                }
                if (trackArtists.isNotEmpty()) {
                    metadataDao.upsertTrackArtists(trackArtists)
                }
                val trackGenres = trackContexts.mapNotNull { context ->
                    val genreName = context.metadata.genre ?: return@mapNotNull null
                    genresByName[normalizeMetadataName(genreName)]?.let { genre ->
                        TrackGenreCrossRef(
                            trackId = context.track.id,
                            genreId = genre.id,
                        )
                    }
                }
                if (trackGenres.isNotEmpty()) {
                    metadataDao.upsertTrackGenres(trackGenres)
                }
                val albumIds = trackContexts.mapNotNull { it.track.albumId }.distinct()
                if (albumIds.isNotEmpty()) {
                    metadataDao.deleteAlbumArtistsForAlbums(albumIds)
                }
                val albumArtists = trackContexts.mapNotNull { context ->
                    val albumId = context.track.albumId ?: return@mapNotNull null
                    val albumArtist = context.metadata.albumArtist ?: return@mapNotNull null
                    artistsByName[normalizeMetadataName(albumArtist)]?.let { artist ->
                        AlbumArtistCrossRef(
                            albumId = albumId,
                            artistId = artist.id,
                            position = 0,
                        )
                    }
                }.distinctBy { it.albumId to it.artistId }
                if (albumArtists.isNotEmpty()) {
                    metadataDao.upsertAlbumArtists(albumArtists)
                }
                val artwork = trackContexts
                    .mapNotNull { context ->
                        buildArtworkEntity(
                            trackId = context.track.id,
                            albumId = context.track.albumId,
                            artwork = context.metadata.artwork ?: return@mapNotNull null,
                        )
                    }
                    .distinctBy { it.contentHash }
                if (artwork.isNotEmpty()) {
                    metadataDao.upsertArtwork(artwork)
                }
                val lyrics = trackContexts.mapNotNull { context ->
                    buildLyricsEntity(context.track.id, context.metadata, now)
                }
                if (lyrics.isNotEmpty()) {
                    metadataDao.upsertLyrics(lyrics)
                }
                val rawMetadata = trackContexts.flatMap { context ->
                    buildRawMetadataEntities(context.track.id, context.metadata)
                }
                if (rawMetadata.isNotEmpty()) {
                    metadataDao.upsertRawMetadata(rawMetadata)
                }
                updatedJob = currentJob.copy(
                    scannedCount = currentJob.scannedCount + entries.size,
                    importedCount = currentJob.importedCount + tracks.size,
                    skippedCount = currentJob.skippedCount + plan.metadataSkippedCount,
                    failedCount = currentJob.failedCount + batchFailedCount,
                    checkpoint = entries.lastOrNull()?.path,
                    errorMessage = null,
                    updatedAt = now,
                )
                syncDao.upsertJob(updatedJob)
            }
        }

        return ImportBatchResult(
            job = updatedJob,
            changedCount = plan.changedCount.toLong(),
        )
    }

    private suspend fun findCanonicalTrack(
        metadata: RemoteMetadata,
        sourceItem: SourceItemEntity,
    ): TrackEntity? {
        val recordingId = metadata.musicbrainzRecordingId?.takeIf { it.isNotBlank() }
        if (recordingId != null) {
            trackDao.findByMusicBrainzRecordingId(recordingId).singleOrNull()?.let { return it }
        }

        val durationMs = metadata.durationMs.toLongOrNull() ?: return null
        val isrc = metadata.isrc?.takeIf { it.isNotBlank() }
        if (isrc != null) {
            trackDao.findByIsrcWithinDuration(
                isrc = isrc,
                minDurationMs = durationMs - DURATION_MATCH_TOLERANCE_MS,
                maxDurationMs = durationMs + DURATION_MATCH_TOLERANCE_MS,
            ).singleOrNull()?.let { return it }
        }

        val title = metadata.title?.takeIf { it.isNotBlank() }
            ?: sourceItem.displayName.substringBeforeLast('.')
        if (title.hasVersionToken()) return null

        return trackDao.findByStrictMetadata(
            titleKey = title.normalizedMatchKey(),
            artistKey = metadata.artist.normalizedMatchKey(),
            albumKey = metadata.album.normalizedMatchKey(),
            minDurationMs = durationMs - DURATION_MATCH_TOLERANCE_MS,
            maxDurationMs = durationMs + DURATION_MATCH_TOLERANCE_MS,
        ).singleOrNull()
    }

    private suspend fun ensureAlbums(
        metadata: List<RemoteMetadata>,
    ): Map<String, AlbumEntity> {
        val values = metadata.mapNotNull { item ->
            val name = item.album?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
            AlbumEntity(
                name = name,
                normalizedName = normalizeMetadataName(name),
                sortName = null,
                year = item.date?.yearPrefix(),
                artworkId = null,
            )
        }.distinctBy { it.normalizedName }
        if (values.isEmpty()) return emptyMap()
        metadataDao.insertAlbums(values)
        return metadataDao.findAlbumsByNormalizedNames(values.map { it.normalizedName })
            .associateBy { it.normalizedName }
    }

    private suspend fun ensureArtists(
        metadata: List<RemoteMetadata>,
    ): Map<String, ArtistEntity> {
        val names = metadata
            .flatMap { it.trackArtists() + listOfNotNull(it.albumArtist) }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy(::normalizeMetadataName)
        if (names.isEmpty()) return emptyMap()
        metadataDao.insertArtists(
            names.map { name ->
                ArtistEntity(
                    name = name,
                    normalizedName = normalizeMetadataName(name),
                    sortName = null,
                )
            }
        )
        return metadataDao.findArtistsByNormalizedNames(names.map(::normalizeMetadataName))
            .associateBy { it.normalizedName }
    }

    private suspend fun ensureGenres(
        metadata: List<RemoteMetadata>,
    ): Map<String, GenreEntity> {
        val names = metadata
            .mapNotNull { it.genre?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy(::normalizeMetadataName)
        if (names.isEmpty()) return emptyMap()
        metadataDao.insertGenres(
            names.map { name ->
                GenreEntity(
                    name = name,
                    normalizedName = normalizeMetadataName(name),
                )
            }
        )
        return metadataDao.findGenresByNormalizedNames(names.map(::normalizeMetadataName))
            .associateBy { it.normalizedName }
    }

    private suspend fun completeImport(
        execution: ImportExecution,
        previousCursor: SourceSyncCursorEntity?,
        currentJob: ImportJobEntity,
        deltaLink: String?,
    ): ImportJobEntity {
        val now = currentTimeMillis()
        val completedJob = currentJob.copy(
            status = if (currentJob.failedCount == 0L) {
                ImportJobStatus.COMPLETED
            } else {
                ImportJobStatus.COMPLETED_WITH_ERRORS
            },
            updatedAt = now,
        )
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                database.sourceItemDao().markMissingDeleted(
                    libraryRootId = execution.libraryRoot.id,
                    scanId = execution.scanId,
                    now = now,
                )
                database.trackSourceRefDao()
                    .markUnavailableForDeletedSourceItems(execution.libraryRoot.id, now)
                syncDao.upsertCursor(
                    SourceSyncCursorEntity(
                        id = previousCursor?.id ?: 0,
                        sourceAccountId = execution.libraryRoot.sourceAccountId,
                        libraryRootId = execution.libraryRoot.id,
                        cursorType = "delta",
                        cursorValue = deltaLink,
                        lastScanId = execution.scanId,
                        lastSyncAt = now,
                    )
                )
                syncDao.upsertJob(completedJob)
                database.libraryRootDao().upsert(
                    execution.libraryRoot.copy(
                        syncCursor = deltaLink,
                        syncStatus = if (completedJob.failedCount == 0L) {
                            LibraryRootSyncStatus.SYNCED
                        } else {
                            LibraryRootSyncStatus.SYNCED_WITH_ERRORS
                        },
                        lastSyncAt = now,
                        updatedAt = now,
                    )
                )
            }
        }
        return completedJob
    }

    private suspend fun completeDeltaImport(
        execution: ImportExecution,
        currentJob: ImportJobEntity,
        deltaLink: String,
    ): ImportJobEntity {
        val now = currentTimeMillis()
        val previousCursor = syncDao.getCursor(execution.libraryRoot.id)
        val completedJob = currentJob.copy(
            status = if (currentJob.failedCount == 0L) {
                ImportJobStatus.COMPLETED
            } else {
                ImportJobStatus.COMPLETED_WITH_ERRORS
            },
            updatedAt = now,
        )
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                syncDao.upsertCursor(
                    SourceSyncCursorEntity(
                        id = previousCursor?.id ?: 0,
                        sourceAccountId = execution.libraryRoot.sourceAccountId,
                        libraryRootId = execution.libraryRoot.id,
                        cursorType = "delta",
                        cursorValue = deltaLink,
                        lastScanId = execution.scanId,
                        lastSyncAt = now,
                    )
                )
                syncDao.upsertJob(completedJob)
                database.libraryRootDao().upsert(
                    execution.libraryRoot.copy(
                        syncCursor = deltaLink,
                        syncStatus = if (completedJob.failedCount == 0L) {
                            LibraryRootSyncStatus.SYNCED
                        } else {
                            LibraryRootSyncStatus.SYNCED_WITH_ERRORS
                        },
                        lastSyncAt = now,
                        updatedAt = now,
                    )
                )
            }
        }
        return completedJob
    }

    private suspend fun readOneDriveDelta(
        storageId: Long,
        rootRemoteId: String,
        cursor: String?,
        latestOnly: Boolean,
        operation: ActiveImportOperation? = null,
    ): OneDriveDeltaSnapshot {
        val items = mutableListOf<OneDriveDeltaItem>()
        var nextCursor = cursor
        var pageCount = 0
        while (true) {
            operation?.throwIfStopRequested()
            pageCount += 1
            check(pageCount <= MAX_DELTA_PAGES) {
                "OneDrive delta exceeded the $MAX_DELTA_PAGES page safety limit"
            }
            when (
                val result = remoteScannerRepository.getOneDriveDeltaPage(
                    storageId = StorageId(storageId),
                    rootRemoteId = rootRemoteId,
                    cursor = nextCursor,
                    latestOnly = latestOnly && pageCount == 1,
                )
            ) {
                is OneDriveDeltaPageResult.Page -> {
                    operation?.throwIfStopRequested()
                    result.v1.refreshToken?.let { refreshToken ->
                        storageRepository.updateOneDriveRefreshToken(
                            StorageId(storageId),
                            refreshToken,
                        )
                    }
                    operation?.throwIfStopRequested()
                    items += result.v1.items
                    check(items.size <= MAX_DELTA_ITEMS) {
                        "OneDrive delta exceeded the $MAX_DELTA_ITEMS item safety limit"
                    }
                    val nextLink = result.v1.nextLink
                    if (nextLink != null) {
                        nextCursor = nextLink
                        continue
                    }
                    return OneDriveDeltaSnapshot(
                        items = items,
                        deltaLink = result.v1.deltaLink,
                        resyncRequired = false,
                    )
                }
                OneDriveDeltaPageResult.ResyncRequired -> {
                    operation?.throwIfStopRequested()
                    return OneDriveDeltaSnapshot(
                        items = emptyList(),
                        deltaLink = null,
                        resyncRequired = true,
                    )
                }
            }
        }
    }

    private fun importResult(
        execution: ImportExecution,
        job: ImportJobEntity,
        changedCount: Long,
    ): RemoteLibraryImportResult {
        return RemoteLibraryImportResult(
            scanId = execution.scanId,
            selectedFolderId = execution.libraryRoot.id,
            scannedCount = job.scannedCount,
            changedCount = changedCount,
            skippedCount = job.skippedCount,
            importedCount = job.importedCount,
            failedCount = job.failedCount,
        )
    }

    private suspend fun startImport(request: RemoteLibraryImportRequest): ImportExecution {
        val startedAt = currentTimeMillis()
        ensureSourceAccount(request.storageId, startedAt)
        val libraryRoot = ensureLibraryRoot(request, startedAt)
        val scanId = request.scanId ?: "scan-${libraryRoot.id}-$startedAt"
        val job = ImportJobEntity(
            id = scanId,
            libraryRootId = libraryRoot.id,
            status = ImportJobStatus.RUNNING,
            scannedCount = 0,
            importedCount = 0,
            skippedCount = 0,
            failedCount = 0,
            checkpoint = null,
            errorMessage = null,
            createdAt = startedAt,
            updatedAt = startedAt,
        )
        syncDao.upsertJob(job)
        return ImportExecution(libraryRoot, scanId, job)
    }

    private suspend fun startTrackedImport(
        request: RemoteLibraryImportRequest,
    ): Pair<ImportExecution, ActiveImportOperation> {
        val execution = startImport(request)
        return try {
            execution to registerActiveOperation(execution)
        } catch (error: Throwable) {
            markImportFailed(execution.libraryRoot, execution.job, error)
            throw error
        }
    }

    private suspend fun markImportFailed(
        root: LibraryRootEntity,
        job: ImportJobEntity,
        error: Throwable,
    ) {
        val now = currentTimeMillis()
        syncDao.upsertJob(
            job.copy(
                status = ImportJobStatus.FAILED,
                errorMessage = error.message?.take(512),
                updatedAt = now,
            )
        )
        database.libraryRootDao().upsert(
            root.copy(
                syncStatus = LibraryRootSyncStatus.FAILED,
                lastSyncAt = now,
                updatedAt = now,
            )
        )
    }

    private suspend fun markImportCancelled(
        root: LibraryRootEntity,
        job: ImportJobEntity,
    ) {
        val now = currentTimeMillis()
        syncDao.upsertJob(
            job.copy(
                status = ImportJobStatus.CANCELLED,
                errorMessage = null,
                updatedAt = now,
            )
        )
        database.libraryRootDao().upsert(
            root.copy(
                syncStatus = LibraryRootSyncStatus.CANCELLED,
                lastSyncAt = now,
                updatedAt = now,
            )
        )
    }

    private suspend fun markImportPaused(
        root: LibraryRootEntity,
        job: ImportJobEntity,
    ) {
        val now = currentTimeMillis()
        syncDao.upsertJob(
            job.copy(
                status = ImportJobStatus.PAUSED,
                errorMessage = null,
                updatedAt = now,
            )
        )
        database.libraryRootDao().upsert(
            root.copy(
                syncStatus = LibraryRootSyncStatus.PAUSED,
                lastSyncAt = now,
                updatedAt = now,
            )
        )
    }

    private suspend fun markImportStopOrFailure(
        error: Throwable,
        operation: ActiveImportOperation,
        root: LibraryRootEntity,
        job: ImportJobEntity,
    ) {
        withContext(NonCancellable) {
            if (error is CancellationException || error is ImportCancelledException) {
                if (operation.isPauseRequested()) {
                    markImportPaused(root, job)
                } else {
                    markImportCancelled(root, job)
                }
            } else {
                markImportFailed(root, job, error)
            }
        }
    }

    private suspend fun registerActiveOperation(
        execution: ImportExecution,
    ): ActiveImportOperation {
        return activeOperationsMutex.withLock {
            check(execution.scanId !in activeOperations) {
                "scan ${execution.scanId} is already active"
            }
            ActiveImportOperation().also { operation ->
                activeOperations[execution.scanId] = operation
            }
        }
    }

    private suspend fun unregisterActiveOperation(
        execution: ImportExecution,
        operation: ActiveImportOperation,
    ) {
        activeOperationsMutex.withLock {
            if (activeOperations[execution.scanId] === operation) {
                activeOperations.remove(execution.scanId)
            }
        }
    }

    private suspend fun ensureSourceAccount(storageId: Long, now: Long): SourceAccountEntity {
        val existing = database.sourceAccountDao().get(storageId)
        val account = SourceAccountEntity(
            id = storageId,
            providerType = existing?.providerType ?: ProviderTypes.WebDav,
            displayName = existing?.displayName ?: "Source $storageId",
            endpoint = existing?.endpoint,
            externalAccountId = existing?.externalAccountId,
            credentialRef = existing?.credentialRef ?: "storage-$storageId",
            priority = existing?.priority ?: 0,
            enabled = existing?.enabled ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        database.sourceAccountDao().upsert(account)
        return database.sourceAccountDao().get(storageId)
            ?: error("source account was not persisted")
    }

    private suspend fun ensureLibraryRoot(
        request: RemoteLibraryImportRequest,
        now: Long,
    ): LibraryRootEntity {
        val canonicalPath = normalizeRemotePath(request.selectedFolderCanonicalPath)
        val existing = database.libraryRootDao()
            .findByPath(request.storageId, canonicalPath)
            ?: request.selectedFolderRemoteId?.let { remoteId ->
                database.libraryRootDao().findByProviderRootId(request.storageId, remoteId)
            }
        val root = LibraryRootEntity(
            id = existing?.id ?: 0,
            sourceAccountId = request.storageId,
            providerRootId = request.selectedFolderRemoteId ?: existing?.providerRootId,
            canonicalPath = canonicalPath,
            displayName = request.selectedFolderDisplayPath ?: existing?.displayName ?: canonicalPath,
            syncStatus = LibraryRootSyncStatus.RUNNING,
            syncCursor = existing?.syncCursor,
            lastSyncAt = existing?.lastSyncAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        database.libraryRootDao().upsert(root)
        return database.libraryRootDao().findByPath(request.storageId, canonicalPath)
            ?: root.providerRootId?.let { database.libraryRootDao().findByProviderRootId(request.storageId, it) }
            ?: error("library root was not persisted")
    }
}

private data class ImportExecution(
    val libraryRoot: LibraryRootEntity,
    val scanId: String,
    val job: ImportJobEntity,
)

private data class ImportBatchResult(
    val job: ImportJobEntity,
    val changedCount: Long,
)

private data class TrackMetadataContext(
    val track: TrackEntity,
    val metadata: RemoteMetadata,
    val sourceItem: SourceItemEntity,
)

private data class SourceImportRow(
    val entry: StorageEntry,
    val metadata: RemoteMetadata,
    val sourceItem: SourceItemEntity,
)

private data class OneDriveDeltaSnapshot(
    val items: List<OneDriveDeltaItem>,
    val deltaLink: String?,
    val resyncRequired: Boolean,
)

private class ImportCancelledException : CancellationException("remote scan cancelled")

internal class ActiveImportOperation {
    private val mutex = Mutex()
    private var stopRequest: ActiveImportStopRequest? = null
    private var scanSession: RemoteMusicScanSession? = null

    suspend fun cancel() {
        requestStop(ActiveImportStopRequest.Cancel)
    }

    suspend fun pause() {
        requestStop(ActiveImportStopRequest.Pause)
    }

    suspend fun attachScanSession(session: RemoteMusicScanSession) {
        val shouldCancel = mutex.withLock {
            scanSession = session
            stopRequest != null
        }
        if (shouldCancel) {
            session.cancel()
        }
    }

    suspend fun throwIfStopRequested() {
        if (mutex.withLock { stopRequest != null }) {
            throw ImportCancelledException()
        }
    }

    suspend fun isPauseRequested(): Boolean {
        return mutex.withLock { stopRequest == ActiveImportStopRequest.Pause }
    }

    private suspend fun requestStop(request: ActiveImportStopRequest) {
        val session = mutex.withLock {
            if (stopRequest == null) {
                stopRequest = request
            }
            scanSession
        }
        session?.cancel()
    }
}

internal enum class ActiveImportStopRequest {
    Cancel,
    Pause,
}

internal fun requiresOneDriveResync(items: List<OneDriveDeltaItem>): Boolean {
    return items.any { item ->
        !item.deleted && !item.isDir && item.path == null
    }
}

internal fun OneDriveDeltaItem.isSupportedMusicFile(): Boolean {
    if (isDir || deleted) return false
    val fileName = name ?: path?.substringAfterLast('/') ?: return false
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in supportedMusicExtensions
}

internal fun OneDriveDeltaItem.toStorageEntry(storageId: Long): StorageEntry {
    val canonicalPath = requireNotNull(path) {
        "OneDrive delta item $remoteId has no path"
    }
    return StorageEntry(
        storageId = StorageId(storageId),
        name = name ?: canonicalPath.substringAfterLast('/'),
        path = canonicalPath,
        size = size,
        isDir = isDir,
        remoteId = remoteId,
        parentRemoteId = parentRemoteId,
        mimeType = mimeType,
        etag = etag,
        ctag = ctag,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )
}

internal fun prepareMusicEntries(
    storageId: Long,
    entries: List<StorageEntry>,
): List<StorageEntry> {
    return entries
        .asSequence()
        .filter { it.storageId.value == storageId }
        .filter(::isSupportedMusicEntry)
        .distinctBy { normalizeRemotePath(it.path) }
        .sortedBy { normalizeRemotePath(it.path) }
        .toList()
}

private fun validateImportSettings(
    metadataConcurrency: UInt,
    importBatchSize: Int,
) {
    require(metadataConcurrency in 1u..16u) {
        "metadata concurrency must be between 1 and 16"
    }
    require(importBatchSize in 1..MAX_IMPORT_BATCH_SIZE) {
        "import batch size must be between 1 and $MAX_IMPORT_BATCH_SIZE"
    }
}

internal data class RemoteLibraryImportPlan(
    val changedEntries: List<StorageEntry>,
    val metadataEntries: List<StorageEntry>,
    val changedItems: List<SourceItemEntity>,
    val unchangedFileIds: List<Long>,
    val changedCount: Int,
    val metadataSkippedCount: Int,
    val unreadableChangedCount: Int,
)

internal fun planRemoteLibraryImport(
    storageId: Long,
    libraryRootId: Long,
    scanId: String,
    now: Long,
    entries: List<StorageEntry>,
    existing: Map<String?, SourceItemEntity>,
    existingByRemoteId: Map<String, SourceItemEntity> = emptyMap(),
): RemoteLibraryImportPlan {
    val changedEntries = mutableListOf<StorageEntry>()
    val metadataEntries = mutableListOf<StorageEntry>()
    val changedItems = mutableListOf<SourceItemEntity>()
    val unchangedFileIds = mutableListOf<Long>()
    var changedCount = 0
    var metadataSkippedCount = 0
    var unreadableChangedCount = 0

    entries.forEach { entry ->
        val canonicalPath = normalizeRemotePath(entry.path)
        val previous = existing[canonicalPath]
            ?: entry.remoteId?.let(existingByRemoteId::get)
        val sameRemoteIdentity = previous?.providerItemId == null ||
            entry.remoteId == null ||
            previous.providerItemId == entry.remoteId
        val sameCanonicalPath = previous?.canonicalPath == canonicalPath
        if (previous != null && sameRemoteIdentity && sameCanonicalPath && previous.hasSameSourceContent(entry)) {
            unchangedFileIds.add(previous.id)
            metadataSkippedCount += 1
            return@forEach
        }
        if (
            previous != null &&
            previous.providerItemId != null &&
            previous.providerItemId == entry.remoteId &&
            previous.hasSameSourceRevision(entry)
        ) {
            buildSourceItemEntity(
                entry = entry,
                libraryRootId = libraryRootId,
                scanId = scanId,
                now = now,
                existing = previous,
            )?.let(changedItems::add)
            changedCount += 1
            metadataSkippedCount += 1
            return@forEach
        }

        changedCount += 1
        changedEntries.add(entry)
        val sourceItem = buildSourceItemEntity(
            entry = entry,
            libraryRootId = libraryRootId,
            scanId = scanId,
            now = now,
            existing = previous,
        )
        if (sourceItem == null) {
            unreadableChangedCount += 1
        } else {
            changedItems.add(sourceItem)
        }
        val size = entry.size
        if (sourceItem != null && (size == null || size == 0uL)) {
            unreadableChangedCount += 1
        } else if (sourceItem != null) {
            metadataEntries.add(entry)
        }
    }

    return RemoteLibraryImportPlan(
        changedEntries = changedEntries,
        metadataEntries = metadataEntries,
        changedItems = changedItems,
        unchangedFileIds = unchangedFileIds,
        changedCount = changedCount,
        metadataSkippedCount = metadataSkippedCount,
        unreadableChangedCount = unreadableChangedCount,
    )
}

internal fun buildTrackEntity(
    entry: StorageEntry,
    metadata: RemoteMetadata,
    sourceItem: SourceItemEntity,
    now: Long,
    existingTrack: TrackEntity? = null,
    albumId: Long? = null,
): TrackEntity {
    return TrackEntity(
        id = existingTrack?.id
            ?: stableTrackId(entry.storageId.value, normalizeRemotePath(entry.path)),
        title = metadata.title?.takeIf { it.isNotBlank() }
            ?: sourceItem.displayName.substringBeforeLast('.'),
        sortTitle = null,
        albumId = albumId,
        albumArtist = metadata.albumArtist,
        composer = metadata.composer,
        comment = metadata.comment,
        grouping = metadata.grouping,
        durationMs = metadata.durationMs.toLongOrNull(),
        discNumber = metadata.discNumber?.toInt(),
        discTotal = metadata.discTotal?.toInt(),
        trackNumber = metadata.trackNumber?.toInt(),
        trackTotal = metadata.trackTotal?.toInt(),
        year = metadata.date?.yearPrefix(),
        date = metadata.date,
        sampleRate = metadata.sampleRate?.toInt(),
        bitRate = (metadata.audioBitrate ?: metadata.overallBitrate)?.toInt(),
        bitsPerSample = metadata.bitDepth?.toInt(),
        channels = metadata.channels?.toInt(),
        channelLayout = metadata.channelLayout,
        codec = metadata.codec,
        container = metadata.container,
        lossless = metadata.lossless,
        createdAt = existingTrack?.createdAt ?: now,
        updatedAt = now,
        artist = metadata.artist,
        lyricist = metadata.lyricist,
        conductor = metadata.conductor,
        copyright = metadata.copyright,
        publisher = metadata.publisher,
        originalReleaseDate = metadata.originalReleaseDate,
        bpm = metadata.bpm,
        musicalKey = metadata.musicalKey,
        isrc = metadata.isrc,
        musicBrainzRecordingId = metadata.musicbrainzRecordingId,
        musicBrainzTrackId = metadata.musicbrainzTrackId,
        musicBrainzReleaseId = metadata.musicbrainzReleaseId,
        musicBrainzReleaseGroupId = metadata.musicbrainzReleaseGroupId,
        musicBrainzArtistId = metadata.musicbrainzArtistId,
        musicBrainzReleaseArtistId = metadata.musicbrainzReleaseArtistId,
        musicBrainzWorkId = metadata.musicbrainzWorkId,
        replayGainTrackGain = metadata.replayGainTrackGain,
        replayGainTrackPeak = metadata.replayGainTrackPeak,
        replayGainAlbumGain = metadata.replayGainAlbumGain,
        replayGainAlbumPeak = metadata.replayGainAlbumPeak,
    )
}

internal fun buildTrackSourceRefEntity(
    track: TrackEntity,
    sourceItem: SourceItemEntity,
    metadata: RemoteMetadata,
    now: Long,
    existingRef: TrackSourceRefEntity? = null,
): TrackSourceRefEntity {
    return TrackSourceRefEntity(
        trackId = track.id,
        sourceItemId = sourceItem.id,
        role = existingRef?.role ?: "primary",
        matchMethod = existingRef?.matchMethod ?: "source_identity",
        matchConfidence = existingRef?.matchConfidence ?: 100,
        isPreferred = existingRef?.isPreferred ?: true,
        isAvailable = !sourceItem.isDeleted,
        isDownloaded = existingRef?.isDownloaded ?: false,
        playable = true,
        downloadable = true,
        codec = metadata.codec ?: track.codec,
        container = metadata.container ?: track.container,
        bitRate = (metadata.audioBitrate ?: metadata.overallBitrate)?.toInt() ?: track.bitRate,
        sampleRate = metadata.sampleRate?.toInt() ?: track.sampleRate,
        bitsPerSample = metadata.bitDepth?.toInt() ?: track.bitsPerSample,
        channels = metadata.channels?.toInt() ?: track.channels,
        lossless = metadata.lossless ?: track.lossless,
        createdAt = existingRef?.createdAt ?: now,
        updatedAt = now,
    )
}

internal fun buildArtworkEntity(
    trackId: Long,
    albumId: Long?,
    artwork: RemoteArtwork,
): ArtworkEntity {
    return ArtworkEntity(
        trackId = if (albumId == null) trackId else null,
        albumId = albumId,
        contentHash = artwork.contentHash,
        localPath = artwork.localPath,
        thumbnailPath = artwork.thumbnailPath,
        width = artwork.width?.toInt(),
        height = artwork.height?.toInt(),
        mimeType = artwork.mimeType,
        pictureType = artwork.pictureType,
    )
}

internal fun buildLyricsEntity(
    trackId: Long,
    metadata: RemoteMetadata,
    now: Long,
): LyricsEntity? {
    val embedded = metadata.lyrics ?: return null
    return LyricsEntity(
        trackId = trackId,
        format = if (embedded.synchronized) "LRC" else "TEXT",
        language = embedded.language,
        synchronized = embedded.synchronized,
        content = embedded.content,
        sourcePath = null,
        updatedAt = now,
    )
}

internal fun buildRawMetadataEntities(
    trackId: Long,
    metadata: RemoteMetadata,
): List<RawMetadataEntity> {
    return metadata.rawMetadata.map { raw ->
        RawMetadataEntity(
            trackId = trackId,
            tagKey = raw.key,
            value = raw.value,
            locale = raw.locale,
            description = raw.description,
        )
    }
}

private fun buildSourceItemEntity(
    entry: StorageEntry,
    libraryRootId: Long,
    scanId: String,
    now: Long,
    existing: SourceItemEntity?,
): SourceItemEntity? {
    val size = entry.size
    if (size != null && size > Long.MAX_VALUE.toULong()) return null
    val canonicalPath = normalizeRemotePath(entry.path)
    val fileName = entry.name.ifBlank { canonicalPath.substringAfterLast('/').ifBlank { canonicalPath } }
    return SourceItemEntity(
        id = existing?.id ?: 0,
        sourceAccountId = entry.storageId.value,
        libraryRootId = libraryRootId,
        itemType = SourceItemTypes.Track,
        providerItemId = entry.remoteId,
        parentProviderItemId = entry.parentRemoteId,
        canonicalPath = canonicalPath,
        displayPath = canonicalPath,
        displayName = fileName,
        mimeType = entry.mimeType,
        sizeBytes = size?.toLong(),
        etag = entry.etag,
        revision = entry.ctag,
        createdAtRemote = entry.createdAt,
        modifiedAtRemote = entry.modifiedAt,
        contentHash = null,
        audioFingerprint = null,
        isDeleted = false,
        firstSyncedAt = existing?.firstSyncedAt ?: now,
        lastSyncedAt = now,
        lastSeenScanId = scanId,
    )
}

private fun SourceItemEntity.hasSameSourceContent(entry: StorageEntry): Boolean {
    val size = entry.size?.toLongOrNull()
    if (sizeBytes != size) return false
    val entryEtag = entry.etag
    return if (!entryEtag.isNullOrBlank() && !etag.isNullOrBlank()) {
        etag == entryEtag
    } else {
        modifiedAtRemote == entry.modifiedAt
    }
}

private fun SourceItemEntity.hasSameSourceRevision(entry: StorageEntry): Boolean {
    val entryEtag = entry.etag
    val entryRevision = entry.ctag
    return (!entryEtag.isNullOrBlank() && etag == entryEtag) ||
        (!entryRevision.isNullOrBlank() && revision == entryRevision)
}

internal fun isSupportedMusicEntry(entry: StorageEntry): Boolean {
    if (entry.isDir) return false
    val extension = entry.name
        .ifBlank { entry.path }
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return extension in supportedMusicExtensions
}

internal fun normalizeRemotePath(path: String): String {
    if (path.isBlank()) return "/"
    val normalized = path.replace('\\', '/')
    return if (normalized.startsWith('/')) normalized else "/$normalized"
}

internal fun stableTrackId(storageId: Long, canonicalPath: String): Long {
    var hash = -3_750_763_034_362_895_579L
    val value = "track:$storageId:${normalizeRemotePath(canonicalPath)}"
    value.forEach { ch ->
        hash = hash xor ch.code.toLong()
        hash *= 1_099_511_628_211L
    }
    val positive = hash and Long.MAX_VALUE
    return if (positive == 0L) 1L else positive
}

private fun String.yearPrefix(): Int? {
    if (length < 4) return null
    return take(4).toIntOrNull()
}

private fun RemoteMetadata.trackArtists(): List<String> {
    return artists
        .ifEmpty { listOfNotNull(artist) }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(::normalizeMetadataName)
}

private fun normalizeMetadataName(value: String): String {
    return value.trim().lowercase()
}

private fun String?.normalizedMatchKey(): String {
    return this?.trim()?.lowercase()?.replace(Regex("\\s+"), " ") ?: ""
}

private fun String.hasVersionToken(): Boolean {
    val value = normalizedMatchKey()
    return versionTokens.any { token -> value.contains(token) }
}

private fun ULong.toLongOrNull(): Long? {
    if (this > Long.MAX_VALUE.toULong()) return null
    return toLong()
}

private object ImportJobStatus {
    const val PAUSED = "PAUSED"
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}

private object LibraryRootSyncStatus {
    const val PAUSED = "PAUSED"
    const val RUNNING = "RUNNING"
    const val SYNCED = "SYNCED"
    const val SYNCED_WITH_ERRORS = "SYNCED_WITH_ERRORS"
    const val CANCELLED = "CANCELLED"
    const val FAILED = "FAILED"
}

private val supportedMusicExtensions = setOf(
    "mp3",
    "flac",
    "m4a",
    "mp4",
    "aac",
    "ogg",
    "oga",
    "opus",
    "wav",
    "aif",
    "aiff",
)

internal const val DEFAULT_IMPORT_BATCH_SIZE = 100
private const val DURATION_MATCH_TOLERANCE_MS = 2_000L
private const val MAX_IMPORT_BATCH_SIZE = 500
private const val MAX_REMOTE_ID_QUERY_SIZE = 500
private const val MAX_DELTA_PAGES = 1_000
private const val MAX_DELTA_ITEMS = 100_000

private val versionTokens = listOf(
    "live",
    "remaster",
    "remix",
    "acoustic",
    "instrumental",
    "karaoke",
    "demo",
    "radio edit",
    "extended mix",
    "cover",
)

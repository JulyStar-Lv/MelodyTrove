package com.github.tidetunes.domain.importing

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.github.tidetunes.database.AlbumArtistCrossRef
import com.github.tidetunes.database.AlbumEntity
import com.github.tidetunes.database.ArtworkEntity
import com.github.tidetunes.database.ArtistEntity
import com.github.tidetunes.database.GenreEntity
import com.github.tidetunes.database.ImportJobEntity
import com.github.tidetunes.database.LyricsEntity
import com.github.tidetunes.database.MetadataDao
import com.github.tidetunes.database.RawMetadataEntity
import com.github.tidetunes.database.RemoteFileDao
import com.github.tidetunes.database.RemoteFileEntity
import com.github.tidetunes.database.SelectedFolderDao
import com.github.tidetunes.database.SelectedFolderEntity
import com.github.tidetunes.database.SyncCursorEntity
import com.github.tidetunes.database.SyncDao
import com.github.tidetunes.database.TideTunesDatabase
import com.github.tidetunes.database.TrackDao
import com.github.tidetunes.database.TrackArtistCrossRef
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.database.TrackGenreCrossRef
import com.github.tidetunes.database.hasSameRemoteContent
import com.github.tidetunes.database.hasSameRemoteRevision
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
    private val selectedFolderDao: SelectedFolderDao,
    private val remoteFileDao: RemoteFileDao,
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
                folder = execution.folder,
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
        val folder = selectedFolderDao.findByPath(storageId, canonicalPath)
            ?: return scanAndInitializeOneDriveFolder(
                storageId = storageId,
                selectedFolderRemoteId = selectedFolderRemoteId,
                selectedFolderCanonicalPath = canonicalPath,
                selectedFolderDisplayPath = selectedFolderDisplayPath,
                scanId = scanId,
                metadataConcurrency = metadataConcurrency,
                importBatchSize = importBatchSize,
            )
        val cursor = syncDao.getCursor(folder.id)?.deltaLink ?: folder.deltaLink
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
                .flatMap { remoteFileDao.findByRemoteIds(storageId, it) }
                .mapNotNull { file -> file.remoteId?.let { it to file } }
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
                val deletedCount = deletedRemoteIds
                    .chunked(MAX_REMOTE_ID_QUERY_SIZE)
                    .sumOf { remoteFileDao.markDeletedByRemoteIds(storageId, it) }
                changedCount += deletedCount
                currentJob = currentJob.copy(
                    scannedCount = currentJob.scannedCount + deletedRemoteIds.size,
                    updatedAt = currentTimeMillis(),
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
                folder = execution.folder,
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
            val previousCursor = syncDao.getCursor(execution.folder.id)
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
                deltaLink = deltaLink ?: execution.folder.deltaLink,
            )
            importResult(execution, currentJob, changedCount)
        } catch (error: Throwable) {
            markImportStopOrFailure(
                error = error,
                operation = operation,
                folder = execution.folder,
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
                folder = execution.folder,
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
        val previousCursor = syncDao.getCursor(execution.folder.id)
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
            deltaLink = deltaLink ?: execution.folder.deltaLink,
        )
        return importResult(execution, job, changedCount) to job
    }

    private suspend fun importBatch(
        request: RemoteLibraryImportRequest,
        execution: ImportExecution,
        currentJob: ImportJobEntity,
        entries: List<StorageEntry>,
    ): ImportBatchResult {
        val batchPaths = entries.map { normalizeRemotePath(it.path) }
        val existing = remoteFileDao
            .findByPaths(request.storageId, batchPaths)
            .associateBy { it.canonicalPath }
        val remoteIds = entries.mapNotNull { it.remoteId }.distinct()
        val existingByRemoteId = if (remoteIds.isEmpty()) {
            emptyMap()
        } else {
            remoteFileDao
                .findByRemoteIds(request.storageId, remoteIds)
                .mapNotNull { file -> file.remoteId?.let { it to file } }
                .toMap()
        }
        val plan = planRemoteLibraryImport(
            storageId = request.storageId,
            selectedFolderId = execution.folder.id,
            scanId = execution.scanId,
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
        val now = currentTimeMillis()
        lateinit var updatedJob: ImportJobEntity

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                remoteFileDao.applyScanBatch(
                    changedFiles = plan.changedFiles,
                    unchangedIds = plan.unchangedFileIds,
                    scanId = execution.scanId,
                )
                val remoteRows = if (plan.changedFiles.isEmpty()) {
                    emptyMap()
                } else {
                    remoteFileDao
                        .findByPaths(
                            request.storageId,
                            plan.changedFiles.map { it.canonicalPath },
                        )
                        .associateBy { it.canonicalPath }
                }
                val existingTracksByRemoteFileId = if (remoteRows.isEmpty()) {
                    emptyMap()
                } else {
                    trackDao
                        .findByRemoteFileIds(remoteRows.values.map { it.id })
                        .mapNotNull { track ->
                            track.remoteFileId?.let { remoteFileId -> remoteFileId to track }
                        }
                        .toMap()
                }
                val trackMetadata = plan.changedEntries.mapNotNull { entry ->
                    val path = normalizeRemotePath(entry.path)
                    val metadata = metadataByPath[path] ?: return@mapNotNull null
                    val remoteFile = remoteRows[path] ?: return@mapNotNull null
                    Triple(entry, metadata, remoteFile)
                }
                val albumsByName = ensureAlbums(trackMetadata.map { it.second })
                val artistsByName = ensureArtists(trackMetadata.map { it.second })
                val genresByName = ensureGenres(trackMetadata.map { it.second })
                val trackContexts = trackMetadata.map { (entry, metadata, remoteFile) ->
                    val track = buildTrackEntity(
                        entry = entry,
                        metadata = metadata,
                        remoteFile = remoteFile,
                        now = now,
                        existingTrack = existingTracksByRemoteFileId[remoteFile.id],
                        albumId = metadata.album
                            ?.let(::normalizeMetadataName)
                            ?.let(albumsByName::get)
                            ?.id,
                    )
                    TrackMetadataContext(track, metadata)
                }
                val tracks = trackContexts.map { it.track }
                trackDao.upsertAll(tracks)
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
                val tracksByRemoteFileId = tracks
                    .mapNotNull { track ->
                        track.remoteFileId?.let { remoteFileId -> remoteFileId to track }
                    }
                    .toMap()
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
                val lyrics = trackMetadata.mapNotNull { (_, metadata, remoteFile) ->
                    val track = tracksByRemoteFileId[remoteFile.id] ?: return@mapNotNull null
                    buildLyricsEntity(track.id, metadata, now)
                }
                if (lyrics.isNotEmpty()) {
                    metadataDao.upsertLyrics(lyrics)
                }
                val rawMetadata = trackMetadata.flatMap { (_, metadata, remoteFile) ->
                    val track = tracksByRemoteFileId[remoteFile.id]
                        ?: return@flatMap emptyList()
                    buildRawMetadataEntities(track.id, metadata)
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
        previousCursor: SyncCursorEntity?,
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
                remoteFileDao.markMissingDeleted(execution.folder.id, execution.scanId)
                syncDao.upsertCursor(
                    SyncCursorEntity(
                        id = previousCursor?.id ?: 0,
                        selectedFolderId = execution.folder.id,
                        deltaLink = deltaLink,
                        continuationToken = null,
                        lastScanId = execution.scanId,
                        lastSyncAt = now,
                    )
                )
                syncDao.upsertJob(completedJob)
                selectedFolderDao.upsert(
                    execution.folder.copy(
                        deltaLink = deltaLink,
                        syncStatus = if (completedJob.failedCount == 0L) {
                            SelectedFolderSyncStatus.SYNCED
                        } else {
                            SelectedFolderSyncStatus.SYNCED_WITH_ERRORS
                        },
                        lastSyncAt = now,
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
        val previousCursor = syncDao.getCursor(execution.folder.id)
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
                    SyncCursorEntity(
                        id = previousCursor?.id ?: 0,
                        selectedFolderId = execution.folder.id,
                        deltaLink = deltaLink,
                        continuationToken = null,
                        lastScanId = execution.scanId,
                        lastSyncAt = now,
                    )
                )
                syncDao.upsertJob(completedJob)
                selectedFolderDao.upsert(
                    execution.folder.copy(
                        deltaLink = deltaLink,
                        syncStatus = if (completedJob.failedCount == 0L) {
                            SelectedFolderSyncStatus.SYNCED
                        } else {
                            SelectedFolderSyncStatus.SYNCED_WITH_ERRORS
                        },
                        lastSyncAt = now,
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
            selectedFolderId = execution.folder.id,
            scannedCount = job.scannedCount,
            changedCount = changedCount,
            skippedCount = job.skippedCount,
            importedCount = job.importedCount,
            failedCount = job.failedCount,
        )
    }

    private suspend fun startImport(request: RemoteLibraryImportRequest): ImportExecution {
        val startedAt = currentTimeMillis()
        val folder = ensureSelectedFolder(request, startedAt)
        val scanId = request.scanId ?: "scan-${folder.id}-$startedAt"
        val job = ImportJobEntity(
            id = scanId,
            selectedFolderId = folder.id,
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
        return ImportExecution(folder, scanId, job)
    }

    private suspend fun startTrackedImport(
        request: RemoteLibraryImportRequest,
    ): Pair<ImportExecution, ActiveImportOperation> {
        val execution = startImport(request)
        return try {
            execution to registerActiveOperation(execution)
        } catch (error: Throwable) {
            markImportFailed(execution.folder, execution.job, error)
            throw error
        }
    }

    private suspend fun markImportFailed(
        folder: SelectedFolderEntity,
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
        selectedFolderDao.upsert(
            folder.copy(
                syncStatus = SelectedFolderSyncStatus.FAILED,
                lastSyncAt = now,
            )
        )
    }

    private suspend fun markImportCancelled(
        folder: SelectedFolderEntity,
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
        selectedFolderDao.upsert(
            folder.copy(
                syncStatus = SelectedFolderSyncStatus.CANCELLED,
                lastSyncAt = now,
            )
        )
    }

    private suspend fun markImportPaused(
        folder: SelectedFolderEntity,
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
        selectedFolderDao.upsert(
            folder.copy(
                syncStatus = SelectedFolderSyncStatus.PAUSED,
                lastSyncAt = now,
            )
        )
    }

    private suspend fun markImportStopOrFailure(
        error: Throwable,
        operation: ActiveImportOperation,
        folder: SelectedFolderEntity,
        job: ImportJobEntity,
    ) {
        withContext(NonCancellable) {
            if (error is CancellationException || error is ImportCancelledException) {
                if (operation.isPauseRequested()) {
                    markImportPaused(folder, job)
                } else {
                    markImportCancelled(folder, job)
                }
            } else {
                markImportFailed(folder, job, error)
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

    private suspend fun ensureSelectedFolder(
        request: RemoteLibraryImportRequest,
        now: Long,
    ): SelectedFolderEntity {
        val canonicalPath = normalizeRemotePath(request.selectedFolderCanonicalPath)
        val existing = selectedFolderDao.findByPath(request.storageId, canonicalPath)
        val folder = SelectedFolderEntity(
            id = existing?.id ?: 0,
            storageId = request.storageId,
            remoteId = request.selectedFolderRemoteId ?: existing?.remoteId,
            canonicalPath = canonicalPath,
            displayPath = request.selectedFolderDisplayPath ?: existing?.displayPath ?: canonicalPath,
            deltaLink = existing?.deltaLink,
            lastSyncAt = existing?.lastSyncAt,
            syncStatus = SelectedFolderSyncStatus.RUNNING,
        )
        selectedFolderDao.upsert(folder)
        return selectedFolderDao.findByPath(request.storageId, canonicalPath)
            ?: error("selected folder was not persisted")
    }
}

private data class ImportExecution(
    val folder: SelectedFolderEntity,
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
    val changedFiles: List<RemoteFileEntity>,
    val unchangedFileIds: List<Long>,
    val changedCount: Int,
    val metadataSkippedCount: Int,
    val unreadableChangedCount: Int,
)

internal fun planRemoteLibraryImport(
    storageId: Long,
    selectedFolderId: Long,
    scanId: String,
    entries: List<StorageEntry>,
    existing: Map<String, RemoteFileEntity>,
    existingByRemoteId: Map<String, RemoteFileEntity> = emptyMap(),
): RemoteLibraryImportPlan {
    val changedEntries = mutableListOf<StorageEntry>()
    val metadataEntries = mutableListOf<StorageEntry>()
    val changedFiles = mutableListOf<RemoteFileEntity>()
    val unchangedFileIds = mutableListOf<Long>()
    var changedCount = 0
    var metadataSkippedCount = 0
    var unreadableChangedCount = 0

    entries.forEach { entry ->
        val canonicalPath = normalizeRemotePath(entry.path)
        val previous = existing[canonicalPath]
            ?: entry.remoteId?.let(existingByRemoteId::get)
        val sameRemoteIdentity = previous?.remoteId == null ||
            entry.remoteId == null ||
            previous.remoteId == entry.remoteId
        if (previous != null && sameRemoteIdentity && previous.hasSameRemoteContent(entry)) {
            unchangedFileIds.add(previous.id)
            metadataSkippedCount += 1
            return@forEach
        }
        if (
            previous != null &&
            previous.remoteId != null &&
            previous.remoteId == entry.remoteId &&
            previous.hasSameRemoteRevision(entry)
        ) {
            buildRemoteFileEntity(
                entry = entry,
                selectedFolderId = selectedFolderId,
                scanId = scanId,
                existing = previous,
            )?.let(changedFiles::add)
            changedCount += 1
            metadataSkippedCount += 1
            return@forEach
        }

        changedCount += 1
        changedEntries.add(entry)
        val remoteFile = buildRemoteFileEntity(
            entry = entry,
            selectedFolderId = selectedFolderId,
            scanId = scanId,
            existing = previous,
        )
        if (remoteFile == null) {
            unreadableChangedCount += 1
        } else {
            changedFiles.add(remoteFile)
        }
        val size = entry.size
        if (remoteFile != null && (size == null || size == 0uL)) {
            unreadableChangedCount += 1
        } else if (remoteFile != null) {
            metadataEntries.add(entry)
        }
    }

    return RemoteLibraryImportPlan(
        changedEntries = changedEntries,
        metadataEntries = metadataEntries,
        changedFiles = changedFiles,
        unchangedFileIds = unchangedFileIds,
        changedCount = changedCount,
        metadataSkippedCount = metadataSkippedCount,
        unreadableChangedCount = unreadableChangedCount,
    )
}

internal fun buildTrackEntity(
    entry: StorageEntry,
    metadata: RemoteMetadata,
    remoteFile: RemoteFileEntity,
    now: Long,
    existingTrack: TrackEntity? = null,
    albumId: Long? = null,
): TrackEntity {
    return TrackEntity(
        id = existingTrack?.id
            ?: stableTrackId(entry.storageId.value, normalizeRemotePath(entry.path)),
        remoteFileId = remoteFile.id,
        sourceStorageId = entry.storageId.value,
        sourcePath = normalizeRemotePath(entry.path),
        title = metadata.title?.takeIf { it.isNotBlank() }
            ?: remoteFile.fileName.substringBeforeLast('.'),
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

private fun buildRemoteFileEntity(
    entry: StorageEntry,
    selectedFolderId: Long,
    scanId: String,
    existing: RemoteFileEntity?,
): RemoteFileEntity? {
    val size = entry.size
    if (size != null && size > Long.MAX_VALUE.toULong()) return null
    val canonicalPath = normalizeRemotePath(entry.path)
    val fileName = entry.name.ifBlank { canonicalPath.substringAfterLast('/').ifBlank { canonicalPath } }
    return RemoteFileEntity(
        id = existing?.id ?: 0,
        storageId = entry.storageId.value,
        selectedFolderId = selectedFolderId,
        remoteId = entry.remoteId,
        parentRemoteId = entry.parentRemoteId,
        canonicalPath = canonicalPath,
        displayPath = canonicalPath,
        fileName = fileName,
        extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase(),
        mimeType = entry.mimeType,
        size = size?.toLong(),
        etag = entry.etag,
        ctag = entry.ctag,
        createdAt = entry.createdAt,
        modifiedAt = entry.modifiedAt,
        contentHash = null,
        isDeleted = false,
        lastSeenScanId = scanId,
    )
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

private object SelectedFolderSyncStatus {
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
private const val MAX_IMPORT_BATCH_SIZE = 500
private const val MAX_REMOTE_ID_QUERY_SIZE = 500
private const val MAX_DELTA_PAGES = 1_000
private const val MAX_DELTA_ITEMS = 100_000

package com.github.tidetunes.domain.importing

import com.github.tidetunes.database.SourceItemEntity
import com.github.tidetunes.database.SourceItemTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import uniffi.tidetunes_core.RemoteEmbeddedLyrics
import uniffi.tidetunes_core.RemoteArtwork
import uniffi.tidetunes_core.RemoteMetadata
import uniffi.tidetunes_core.RemoteRawMetadataEntry
import uniffi.tidetunes_core.OneDriveDeltaItem
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageId

class RemoteLibraryImportCoordinatorTest {
    @Test
    fun activeImportOperationCancelRequestsStopWithoutPauseFlag() = runBlocking {
        val operation = ActiveImportOperation()

        operation.cancel()

        assertFailsWith<CancellationException> {
            operation.throwIfStopRequested()
        }
        assertFalse(operation.isPauseRequested())
    }

    @Test
    fun activeImportOperationPauseRequestsStopWithPauseFlag() = runBlocking {
        val operation = ActiveImportOperation()

        operation.pause()

        assertFailsWith<CancellationException> {
            operation.throwIfStopRequested()
        }
        assertTrue(operation.isPauseRequested())
    }

    @Test
    fun activeImportOperationKeepsFirstStopReason() = runBlocking {
        val cancelled = ActiveImportOperation()
        cancelled.cancel()
        cancelled.pause()

        val paused = ActiveImportOperation()
        paused.pause()
        paused.cancel()

        assertFalse(cancelled.isPauseRequested())
        assertTrue(paused.isPauseRequested())
    }

    @Test
    fun oneDriveDeltaMapsStableIdentityAndParent() {
        val item = deltaItem(
            remoteId = "file-id",
            parentRemoteId = "folder-id",
            path = "/Music/Renamed.flac",
        )

        val entry = item.toStorageEntry(storageId = 9)

        assertEquals(StorageId(9), entry.storageId)
        assertEquals("file-id", entry.remoteId)
        assertEquals("folder-id", entry.parentRemoteId)
        assertEquals("/Music/Renamed.flac", entry.path)
        assertTrue(item.isSupportedMusicFile())
    }

    @Test
    fun oneDriveDeltaOnlyResyncsForLiveFileWithoutPath() {
        assertFalse(
            requiresOneDriveResync(
                listOf(deltaItem(remoteId = "folder", isDir = true))
            )
        )
        assertFalse(
            requiresOneDriveResync(
                listOf(deltaItem(remoteId = "unknown", deleted = true, path = null))
            )
        )
        assertTrue(
            requiresOneDriveResync(
                listOf(deltaItem(remoteId = "live", deleted = false, path = null))
            )
        )
    }

    @Test
    fun preparesLargeSnapshotsInStablePathOrder() {
        val entries = buildList {
            add(entry(path = "/Music/z.flac", name = "z.flac"))
            add(entry(path = "/Music/readme.txt", name = "readme.txt"))
            add(entry(path = "/Music/a.flac", name = "a.flac"))
            add(entry(path = "Music/a.flac", name = "duplicate.flac"))
            repeat(1_005) { index ->
                add(
                    entry(
                        path = "/Music/Album/track-${index.toString().padStart(4, '0')}.mp3",
                        name = "track-$index.mp3",
                    )
                )
            }
        }

        val prepared = prepareMusicEntries(storageId = 1, entries = entries)
        val batches = prepared.chunked(DEFAULT_IMPORT_BATCH_SIZE)

        assertEquals(1_007, prepared.size)
        assertEquals("/Music/Album/track-0000.mp3", prepared.first().path)
        assertEquals("/Music/z.flac", prepared.last().path)
        assertEquals(11, batches.size)
        assertTrue(batches.all { it.size <= DEFAULT_IMPORT_BATCH_SIZE })
    }

    @Test
    fun planSkipsUnchangedFilesAndKeepsChangedMusicOnly() {
        val unchanged = sourceItem(
            id = 11,
            canonicalPath = "/Music/unchanged.flac",
            etag = "\"same\"",
        )
        val plan = planRemoteLibraryImport(
            storageId = 1,
            libraryRootId = 7,
            scanId = "scan-1",
            now = 100,
            entries = listOf(
                entry(path = "/Music/unchanged.flac", name = "unchanged.flac", etag = "\"same\""),
                entry(path = "/Music/changed.mp3", name = "changed.mp3", etag = "\"new\""),
                entry(path = "/Music/readme.txt", name = "readme.txt", etag = "\"new\""),
                entry(path = "/Music/Sub", name = "Sub", isDir = true),
            ).filter(::isSupportedMusicEntry),
            existing = mapOf(unchanged.canonicalPath to unchanged),
        )

        assertEquals(listOf(11L), plan.unchangedFileIds)
        assertEquals(listOf("/Music/changed.mp3"), plan.changedEntries.map { it.path })
        assertEquals(listOf("/Music/changed.mp3"), plan.changedItems.map { it.canonicalPath })
        assertEquals(listOf("/Music/changed.mp3"), plan.metadataEntries.map { it.path })
        assertEquals(1, plan.changedCount)
        assertEquals(1, plan.metadataSkippedCount)
        assertEquals(0, plan.unreadableChangedCount)
    }

    @Test
    fun moveByStableRemoteIdUpdatesPathWithoutMetadataRead() {
        val previous = sourceItem(
            id = 11,
            canonicalPath = "/Music/Old/song.flac",
            etag = "\"same\"",
            remoteId = "drive-item-1",
        )
        val moved = entry(
            path = "/Music/New/song.flac",
            name = "song.flac",
            etag = "\"same\"",
            remoteId = "drive-item-1",
        )

        val plan = planRemoteLibraryImport(
            storageId = 1,
            libraryRootId = 7,
            scanId = "scan-move",
            now = 200,
            entries = listOf(moved),
            existing = emptyMap(),
            existingByRemoteId = mapOf("drive-item-1" to previous),
        )

        assertTrue(plan.changedEntries.isEmpty())
        assertTrue(plan.metadataEntries.isEmpty())
        assertEquals(1, plan.changedCount)
        assertEquals(1, plan.metadataSkippedCount)
        assertEquals(11, plan.changedItems.single().id)
        assertEquals("/Music/New/song.flac", plan.changedItems.single().canonicalPath)
        assertEquals("scan-move", plan.changedItems.single().lastSeenScanId)
    }

    @Test
    fun trackIdIsStableAndPositive() {
        val lhs = stableTrackId(1, "/Music/track.flac")
        val rhs = stableTrackId(1, "Music/track.flac")

        assertEquals(lhs, rhs)
        assertTrue(lhs > 0)
        assertFalse(lhs == stableTrackId(2, "/Music/track.flac"))
    }

    @Test
    fun mapsRemoteMetadataToTrackEntity() {
        val entry = entry(path = "/Music/Song.flac", name = "Song.flac")
        val sourceItem = sourceItem(id = 42, canonicalPath = "/Music/Song.flac")
        val metadata = metadata(
            title = "Metadata Title",
            artist = "Artist",
            albumArtist = "Album Artist",
            date = "2025-01-02",
            trackNumber = 3u,
            trackTotal = 9u,
            discNumber = 1u,
            discTotal = 2u,
            durationMs = 181_000uL,
            sampleRate = 48_000u,
            bitDepth = 24u.toUByte(),
            channels = 2u.toUByte(),
            overallBitrate = 950_000u,
            audioBitrate = 900_000u,
        )
        val track = buildTrackEntity(
            entry = entry,
            metadata = metadata,
            sourceItem = sourceItem,
            now = 1000,
        )
        val ref = buildTrackSourceRefEntity(
            track = track,
            sourceItem = sourceItem,
            metadata = metadata,
            now = 1000,
        )

        assertEquals(track.id, ref.trackId)
        assertEquals(42, ref.sourceItemId)
        assertEquals("source_identity", ref.matchMethod)
        assertEquals(true, ref.isAvailable)
        assertEquals("Metadata Title", track.title)
        assertEquals("Album Artist", track.albumArtist)
        assertEquals(181_000, track.durationMs)
        assertEquals(3, track.trackNumber)
        assertEquals(2025, track.year)
        assertEquals(48_000, track.sampleRate)
        assertEquals(900_000, track.bitRate)
        assertEquals(24, track.bitsPerSample)
        assertEquals(2, track.channels)
        assertEquals("Artist", track.artist)
        assertEquals("Composer", track.composer)
        assertEquals("Lyricist", track.lyricist)
        assertEquals("Conductor", track.conductor)
        assertEquals("Copyright", track.copyright)
        assertEquals("Publisher", track.publisher)
        assertEquals("1999-01-01", track.originalReleaseDate)
        assertEquals(128.5, track.bpm)
        assertEquals("8A", track.musicalKey)
        assertEquals("US-AAA-26-00001", track.isrc)
        assertEquals("recording-id", track.musicBrainzRecordingId)
        assertEquals(-7.25, track.replayGainTrackGain)
        assertEquals("FLAC", track.codec)
        assertEquals(true, track.lossless)
    }

    @Test
    fun metadataRefreshPreservesTrackIdentityAfterMove() {
        val previousTrack = buildTrackEntity(
            entry = entry(path = "/Music/Old.flac", name = "Old.flac"),
            metadata = metadata(title = "Old"),
            sourceItem = sourceItem(id = 42, canonicalPath = "/Music/Old.flac"),
            now = 100,
        )
        val refreshed = buildTrackEntity(
            entry = entry(path = "/Music/New.flac", name = "New.flac"),
            metadata = metadata(title = "New"),
            sourceItem = sourceItem(id = 42, canonicalPath = "/Music/New.flac"),
            now = 200,
            existingTrack = previousTrack,
        )

        assertEquals(previousTrack.id, refreshed.id)
        assertEquals(previousTrack.createdAt, refreshed.createdAt)
        assertEquals(200, refreshed.updatedAt)
        assertEquals("New", refreshed.title)
    }

    @Test
    fun mapsEmbeddedLyricsAndRawTagsForRoom() {
        val metadata = metadata(title = "Song")

        val lyrics = buildLyricsEntity(trackId = 9, metadata = metadata, now = 500)
        val raw = buildRawMetadataEntities(trackId = 9, metadata = metadata)

        assertEquals(9, lyrics?.trackId)
        assertEquals("LRC", lyrics?.format)
        assertEquals("eng", lyrics?.language)
        assertEquals("[00:01.00]Line", lyrics?.content)
        assertEquals(500, lyrics?.updatedAt)
        assertEquals(1, raw.size)
        assertEquals("Composer", raw.single().tagKey)
        assertEquals("Composer", raw.single().value)
    }

    @Test
    fun mapsRemoteArtworkForRoomCacheMetadata() {
        val trackArtwork = buildArtworkEntity(
            trackId = 9,
            albumId = null,
            artwork = artwork(localPath = "/cache/artwork/track.jpg"),
        )
        val albumArtwork = buildArtworkEntity(
            trackId = 9,
            albumId = 90,
            artwork = artwork(localPath = "/cache/artwork/album.png", mimeType = "image/png"),
        )

        assertEquals(9, trackArtwork.trackId)
        assertEquals(null, trackArtwork.albumId)
        assertEquals("/cache/artwork/track.jpg", trackArtwork.localPath)
        assertEquals("image/jpeg", trackArtwork.mimeType)
        assertEquals("CoverFront", trackArtwork.pictureType)
        assertEquals(null, albumArtwork.trackId)
        assertEquals(90, albumArtwork.albumId)
        assertEquals("/cache/artwork/album.png", albumArtwork.localPath)
        assertEquals("image/png", albumArtwork.mimeType)
    }

    private fun sourceItem(
        id: Long,
        canonicalPath: String,
        etag: String? = "\"same\"",
        remoteId: String? = null,
    ) = SourceItemEntity(
        id = id,
        sourceAccountId = 1,
        libraryRootId = 7,
        itemType = SourceItemTypes.Track,
        providerItemId = remoteId,
        parentProviderItemId = null,
        canonicalPath = canonicalPath,
        displayPath = canonicalPath,
        displayName = canonicalPath.substringAfterLast('/'),
        mimeType = "audio/flac",
        sizeBytes = 100,
        etag = etag,
        revision = null,
        createdAtRemote = 10,
        modifiedAtRemote = 20,
        contentHash = null,
        audioFingerprint = null,
        isDeleted = false,
        firstSyncedAt = 1,
        lastSyncedAt = 2,
        lastSeenScanId = "previous",
    )

    private fun entry(
        path: String,
        name: String,
        etag: String? = "\"same\"",
        isDir: Boolean = false,
        remoteId: String? = null,
    ) = StorageEntry(
        storageId = StorageId(1),
        name = name,
        path = path,
        size = if (isDir) null else 100uL,
        isDir = isDir,
        remoteId = remoteId,
        parentRemoteId = null,
        mimeType = if (isDir) null else "audio/flac",
        etag = etag,
        ctag = null,
        createdAt = 10,
        modifiedAt = 20,
    )

    private fun metadata(
        title: String,
        artist: String? = null,
        albumArtist: String? = null,
        date: String? = null,
        trackNumber: UInt? = null,
        trackTotal: UInt? = null,
        discNumber: UInt? = null,
        discTotal: UInt? = null,
        durationMs: ULong = 1uL,
        sampleRate: UInt? = null,
        bitDepth: UByte? = null,
        channels: UByte? = null,
        overallBitrate: UInt? = null,
        audioBitrate: UInt? = null,
        artwork: RemoteArtwork? = null,
    ) = RemoteMetadata(
        title = title,
        artist = artist,
        artists = listOfNotNull(artist),
        albumArtist = albumArtist,
        album = "Album",
        composer = "Composer",
        lyricist = "Lyricist",
        conductor = "Conductor",
        genre = "Jazz",
        grouping = "Suite",
        comment = "Comment",
        copyright = "Copyright",
        publisher = "Publisher",
        date = date,
        originalReleaseDate = "1999-01-01",
        trackNumber = trackNumber,
        trackTotal = trackTotal,
        discNumber = discNumber,
        discTotal = discTotal,
        bpm = 128.5,
        musicalKey = "8A",
        isrc = "US-AAA-26-00001",
        musicbrainzRecordingId = "recording-id",
        musicbrainzTrackId = "track-id",
        musicbrainzReleaseId = "release-id",
        musicbrainzReleaseGroupId = "release-group-id",
        musicbrainzArtistId = "artist-id",
        musicbrainzReleaseArtistId = "release-artist-id",
        musicbrainzWorkId = "work-id",
        replayGainTrackGain = -7.25,
        replayGainTrackPeak = 0.98,
        replayGainAlbumGain = -6.0,
        replayGainAlbumPeak = 0.99,
        lyrics = RemoteEmbeddedLyrics(
            content = "[00:01.00]Line",
            synchronized = true,
            language = "eng",
            description = "main",
        ),
        artwork = artwork,
        rawMetadata = listOf(
            RemoteRawMetadataEntry(
                key = "Composer",
                value = "Composer",
                locale = null,
                description = null,
            )
        ),
        durationMs = durationMs,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        channels = channels,
        channelLayout = "FRONT_LEFT|FRONT_RIGHT",
        overallBitrate = overallBitrate,
        audioBitrate = audioBitrate,
        codec = "FLAC",
        container = "FLAC",
        lossless = true,
    )

    private fun artwork(
        localPath: String,
        mimeType: String = "image/jpeg",
    ) = RemoteArtwork(
        contentHash = "hash-${localPath.substringAfterLast('/')}",
        localPath = localPath,
        thumbnailPath = null,
        width = 512u,
        height = 512u,
        mimeType = mimeType,
        pictureType = "CoverFront",
    )

    private fun deltaItem(
        remoteId: String,
        parentRemoteId: String? = "parent-id",
        path: String? = "/Music/Song.flac",
        isDir: Boolean = false,
        deleted: Boolean = false,
    ) = OneDriveDeltaItem(
        remoteId = remoteId,
        parentRemoteId = parentRemoteId,
        name = path?.substringAfterLast('/'),
        path = path,
        size = if (isDir || deleted) null else 100uL,
        isDir = isDir,
        deleted = deleted,
        mimeType = if (isDir || deleted) null else "audio/flac",
        etag = "\"etag\"",
        ctag = null,
        createdAt = 10,
        modifiedAt = 20,
    )
}

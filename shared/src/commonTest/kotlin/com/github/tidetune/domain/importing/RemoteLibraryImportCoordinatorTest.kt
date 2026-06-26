package com.github.tidetune.domain.importing

import com.github.tidetune.database.RemoteFileEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uniffi.tidetune_core.RemoteEmbeddedLyrics
import uniffi.tidetune_core.RemoteMetadata
import uniffi.tidetune_core.RemoteRawMetadataEntry
import uniffi.tidetune_core.OneDriveDeltaItem
import uniffi.tidetune_core.StorageEntry
import uniffi.tidetune_core.StorageId

class RemoteLibraryImportCoordinatorTest {
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
        val unchanged = remoteFile(
            id = 11,
            canonicalPath = "/Music/unchanged.flac",
            etag = "\"same\"",
        )
        val plan = planRemoteLibraryImport(
            storageId = 1,
            selectedFolderId = 7,
            scanId = "scan-1",
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
        assertEquals(listOf("/Music/changed.mp3"), plan.changedFiles.map { it.canonicalPath })
        assertEquals(listOf("/Music/changed.mp3"), plan.metadataEntries.map { it.path })
        assertEquals(1, plan.changedCount)
        assertEquals(1, plan.metadataSkippedCount)
        assertEquals(0, plan.unreadableChangedCount)
    }

    @Test
    fun moveByStableRemoteIdUpdatesPathWithoutMetadataRead() {
        val previous = remoteFile(
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
            selectedFolderId = 7,
            scanId = "scan-move",
            entries = listOf(moved),
            existing = emptyMap(),
            existingByRemoteId = mapOf("drive-item-1" to previous),
        )

        assertTrue(plan.changedEntries.isEmpty())
        assertTrue(plan.metadataEntries.isEmpty())
        assertEquals(1, plan.changedCount)
        assertEquals(1, plan.metadataSkippedCount)
        assertEquals(11, plan.changedFiles.single().id)
        assertEquals("/Music/New/song.flac", plan.changedFiles.single().canonicalPath)
        assertEquals("scan-move", plan.changedFiles.single().lastSeenScanId)
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
        val remoteFile = remoteFile(id = 42, canonicalPath = "/Music/Song.flac")
        val track = buildTrackEntity(
            entry = entry,
            metadata = metadata(
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
            ),
            remoteFile = remoteFile,
            now = 1000,
        )

        assertEquals(42, track.remoteFileId)
        assertEquals(1, track.sourceStorageId)
        assertEquals("/Music/Song.flac", track.sourcePath)
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
            remoteFile = remoteFile(id = 42, canonicalPath = "/Music/Old.flac"),
            now = 100,
        )
        val refreshed = buildTrackEntity(
            entry = entry(path = "/Music/New.flac", name = "New.flac"),
            metadata = metadata(title = "New"),
            remoteFile = remoteFile(id = 42, canonicalPath = "/Music/New.flac"),
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

    private fun remoteFile(
        id: Long,
        canonicalPath: String,
        etag: String? = "\"same\"",
        remoteId: String? = null,
    ) = RemoteFileEntity(
        id = id,
        storageId = 1,
        selectedFolderId = 7,
        remoteId = remoteId,
        parentRemoteId = null,
        canonicalPath = canonicalPath,
        displayPath = canonicalPath,
        fileName = canonicalPath.substringAfterLast('/'),
        extension = canonicalPath.substringAfterLast('.', ""),
        mimeType = "audio/flac",
        size = 100,
        etag = etag,
        ctag = null,
        createdAt = 10,
        modifiedAt = 20,
        contentHash = null,
        isDeleted = false,
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

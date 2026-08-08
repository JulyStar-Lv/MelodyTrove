package io.github.julystar.musicapp.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackMergeDaoIntegrationTest {
    @Test
    fun selectingPlaybackSourceKeepsExactlyOnePreferredRef() = withDatabase { database ->
        seedSource(database, accountId = 10, rootId = 100, itemId = 1)
        seedSource(database, accountId = 20, rootId = 200, itemId = 2)
        database.trackDao().upsertAll(listOf(track(1, lastPlayedAt = null)))
        database.trackSourceRefDao().upsertAll(
            listOf(sourceRef(1, 1), sourceRef(1, 2))
        )

        assertEquals(
            true,
            database.trackSourceRefDao().selectPreferredSource(
                trackId = 1,
                sourceItemId = 2,
                now = 20,
            ),
        )

        val refs = database.trackSourceRefDao().findByTrackId(1)
        assertEquals(listOf(2L), refs.filter { it.isPreferred }.map { it.sourceItemId })
    }

    @Test
    fun mergeMovesLibraryAndUserReferencesToSurvivingTrack() = withDatabase { database ->
        seedSource(database, accountId = 10, rootId = 100, itemId = 1)
        seedSource(database, accountId = 20, rootId = 200, itemId = 2)
        database.trackDao().upsertAll(
            listOf(track(1, lastPlayedAt = 10), track(2, lastPlayedAt = 20))
        )
        database.trackSourceRefDao().upsertAll(
            listOf(sourceRef(1, 1), sourceRef(2, 2))
        )
        database.playlistDao().upsert(
            PlaylistEntity(
                id = 7,
                title = "Playlist",
                artworkId = null,
                createdAt = 1,
                updatedAt = 1,
                sortOrder = 0,
            )
        )
        database.playlistDao().upsertTracks(
            listOf(
                PlaylistTrackCrossRef(7, 1, sortOrder = 2, addedAt = 2),
                PlaylistTrackCrossRef(7, 2, sortOrder = 1, addedAt = 1),
            )
        )
        database.metadataDao().upsertLyrics(
            listOf(
                LyricsEntity(
                    trackId = 2,
                    format = "TEXT",
                    language = null,
                    synchronized = false,
                    content = "lyrics",
                    sourcePath = null,
                    updatedAt = 2,
                    sourceKind = "Plugin",
                )
            )
        )
        database.metadataDao().upsertRawMetadata(
            listOf(RawMetadataEntity(trackId = 2, tagKey = "tag", value = "value", locale = null, description = null))
        )
        database.metadataDao().upsertArtwork(
            listOf(
                ArtworkEntity(
                    trackId = 2,
                    albumId = null,
                    contentHash = "artwork-2",
                    localPath = "/artwork-2.jpg",
                    thumbnailPath = null,
                    width = null,
                    height = null,
                    mimeType = "image/jpeg",
                    pictureType = "CoverFront",
                )
            )
        )
        database.listeningStatisticsDao().insertHistory(
            ListeningHistoryEntity(
                trackId = 2,
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationMs = 180_000,
                listenedMs = 60_000,
                playedAtEpochMs = 20,
            )
        )

        database.trackMergeDao().mergeTracks(
            targetTrackId = 1,
            sourceTrackIds = listOf(2),
            matchMethod = "strict_metadata",
            matchConfidence = 80,
            lastPlayedAt = 20,
            now = 30,
        )

        assertNull(database.trackDao().get(2))
        assertEquals(20L, assertNotNull(database.trackDao().get(1)).lastPlayedAt)
        val sourceRefs = database.trackSourceRefDao().findByTrackId(1)
        assertEquals(setOf(1L, 2L), sourceRefs.mapTo(mutableSetOf()) { it.sourceItemId })
        assertEquals("alternate", sourceRefs.single { it.sourceItemId == 2L }.role)
        val playlistTracks = database.playlistDao().observeTracks(7).first()
        assertEquals(listOf(1L), playlistTracks.map { it.trackId })
        assertEquals(1L, playlistTracks.single().sortOrder)
        assertEquals(1, database.metadataDao().getLyricsCandidates(1).size)
        assertEquals(1, database.metadataDao().rawMetadataForTrack(1).size)
        assertEquals("artwork-2", database.metadataDao().getArtworkForTrack(1)?.contentHash)
        assertEquals(1L, database.listeningStatisticsDao().observeHistory().first().single().trackId)
    }

    private fun withDatabase(block: suspend (AppDatabase) -> Unit) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase> {
            AppDatabaseConstructor.initialize()
        }
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private suspend fun seedSource(
        database: AppDatabase,
        accountId: Long,
        rootId: Long,
        itemId: Long,
    ) {
        database.sourceAccountDao().upsert(
            SourceAccountEntity(
                id = accountId,
                providerType = ProviderTypes.WebDav,
                displayName = "Source $accountId",
                endpoint = "https://example.invalid/$accountId",
                externalAccountId = null,
                credentialRef = "credential-$accountId",
                priority = 0,
                enabled = true,
                createdAt = 1,
                updatedAt = 1,
            )
        )
        database.libraryRootDao().upsert(
            LibraryRootEntity(
                id = rootId,
                sourceAccountId = accountId,
                providerRootId = "root-$rootId",
                canonicalPath = "/Music",
                displayName = "Music",
                syncStatus = "SYNCED",
                syncCursor = null,
                lastSyncAt = 1,
                createdAt = 1,
                updatedAt = 1,
            )
        )
        database.sourceItemDao().upsertAll(
            listOf(
                SourceItemEntity(
                    id = itemId,
                    sourceAccountId = accountId,
                    libraryRootId = rootId,
                    itemType = SourceItemTypes.Track,
                    providerItemId = "item-$itemId",
                    parentProviderItemId = null,
                    canonicalPath = "/Music/song-$itemId.flac",
                    displayPath = "/Music/song-$itemId.flac",
                    displayName = "song-$itemId.flac",
                    mimeType = "audio/flac",
                    sizeBytes = 1_000,
                    etag = "etag-$itemId",
                    revision = null,
                    createdAtRemote = 1,
                    modifiedAtRemote = 1,
                    contentHash = null,
                    audioFingerprint = null,
                    isDeleted = false,
                    firstSyncedAt = 1,
                    lastSyncedAt = 1,
                    lastSeenScanId = "scan",
                )
            )
        )
    }

    private fun track(id: Long, lastPlayedAt: Long?) = TrackEntity(
        id = id,
        title = "Song",
        sortTitle = null,
        albumId = null,
        albumArtist = null,
        composer = null,
        comment = null,
        grouping = null,
        durationMs = 180_000,
        discNumber = null,
        discTotal = null,
        trackNumber = null,
        trackTotal = null,
        year = null,
        date = null,
        sampleRate = null,
        bitRate = null,
        bitsPerSample = null,
        channels = null,
        channelLayout = null,
        codec = "FLAC",
        container = "FLAC",
        lossless = true,
        createdAt = id,
        updatedAt = id,
        lastPlayedAt = lastPlayedAt,
        artist = "Artist",
    )

    private fun sourceRef(trackId: Long, sourceItemId: Long) = TrackSourceRefEntity(
        trackId = trackId,
        sourceItemId = sourceItemId,
        role = "primary",
        matchMethod = "source_identity",
        matchConfidence = 100,
        isPreferred = true,
        isAvailable = true,
        isDownloaded = false,
        playable = true,
        downloadable = true,
        codec = "FLAC",
        container = "FLAC",
        bitRate = null,
        sampleRate = null,
        bitsPerSample = null,
        channels = 2,
        lossless = true,
        createdAt = 1,
        updatedAt = 1,
    )
}

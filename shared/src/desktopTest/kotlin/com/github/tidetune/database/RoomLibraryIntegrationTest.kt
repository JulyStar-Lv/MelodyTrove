package com.github.tidetune.database

import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.tidetune.singleton.RoomLibraryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import uniffi.tidetune_core.ArgCreatePlaylist
import uniffi.tidetune_core.LyricLoadState
import uniffi.tidetune_core.MusicId
import uniffi.tidetune_core.StorageEntry
import uniffi.tidetune_core.StorageEntryLoc
import uniffi.tidetune_core.StorageId
import uniffi.tidetune_core.ToAddMusicEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomLibraryIntegrationTest {
    @Test
    fun migrationOneToTwoAddsExtendedTrackMetadataColumns() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execute("CREATE TABLE track (id INTEGER NOT NULL PRIMARY KEY)")
            MIGRATION_1_2.migrate(connection)

            val columns = buildSet {
                connection.prepare("PRAGMA table_info(track)").use { statement ->
                    while (statement.step()) {
                        add(statement.getText(1))
                    }
                }
            }
            assertTrue("artist" in columns)
            assertTrue("lyricist" in columns)
            assertTrue("musicBrainzRecordingId" in columns)
            assertTrue("replayGainAlbumPeak" in columns)
        } finally {
            connection.close()
        }
    }

    @Test
    fun migrationTwoToThreeAddsRoomOnlyPlaybackColumns() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execute("CREATE TABLE track (id INTEGER NOT NULL PRIMARY KEY)")
            connection.execute("CREATE TABLE playlist (id INTEGER NOT NULL PRIMARY KEY)")
            MIGRATION_2_3.migrate(connection)

            val trackColumns = columns(connection, "track")
            val playlistColumns = columns(connection, "playlist")
            assertTrue("sourceStorageId" in trackColumns)
            assertTrue("sourcePath" in trackColumns)
            assertTrue("coverStorageId" in playlistColumns)
            assertTrue("coverPath" in playlistColumns)
        } finally {
            connection.close()
        }
    }

    @Test
    fun roomLibraryStoreCreatesPlaylistTracksAndRemoteLocWithoutLegacyDatabase() =
        withDatabase { database ->
            seedStorageAndFolder(database)
            val store = roomLibraryStore(database)
            val cover = StorageEntryLoc(StorageId(1), "/Music/cover.jpg")

            val playlist = assertNotNull(
                store.createPlaylist(
                    ArgCreatePlaylist(
                        title = "Room Playlist",
                        cover = cover,
                        entries = listOf(
                            ToAddMusicEntry(
                                entry = storageEntry(path = "/Music/Track.flac", name = "Track.flac"),
                                name = "Display Title",
                            )
                        ),
                    )
                )
            )

            assertEquals("Room Playlist", playlist.abstr.meta.title)
            assertEquals(cover, playlist.abstr.meta.cover)
            assertEquals(1uL, playlist.abstr.musicCount)

            val musicId = playlist.musics.single().meta.id
            val track = assertNotNull(database.trackDao().get(musicId.value))
            assertNull(track.remoteFileId)
            assertEquals(1, track.sourceStorageId)
            assertEquals("/Music/Track.flac", track.sourcePath)

            val music = assertNotNull(store.getMusic(musicId))
            assertEquals(StorageEntryLoc(StorageId(1), "/Music/Track.flac"), music.loc)
            assertEquals("Display Title", music.meta.title)
        }

    @Test
    fun roomLibraryStoreUpdatesDurationAndRemovesLyricsInRoom() = withDatabase { database ->
        val store = roomLibraryStore(database)
        database.trackDao().upsertAll(
            listOf(
                track(
                    id = 201,
                    remoteFileId = null,
                    sourceStorageId = 1,
                    sourcePath = "/Music/song.flac",
                )
            )
        )
        database.metadataDao().upsertLyrics(
            listOf(
                LyricsEntity(
                    trackId = 201,
                    format = "LRC",
                    language = "eng",
                    synchronized = true,
                    content = "[00:01.00]Line",
                    sourcePath = null,
                    updatedAt = 1,
                )
            )
        )

        assertEquals(LyricLoadState.LOADED, assertNotNull(store.getMusic(MusicId(201))).lyric?.loadedState)

        store.removeLyric(MusicId(201))
        store.updateDuration(MusicId(201), -10)

        assertEquals(LyricLoadState.MISSING, assertNotNull(store.getMusic(MusicId(201))).lyric?.loadedState)
        assertEquals(0, database.trackDao().get(201)?.durationMs)
        assertNull(database.metadataDao().getLyrics(201))
    }

    @Test
    fun upsertMoveDeleteAndRestoreStayConsistent() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        val remoteFileDao = database.remoteFileDao()
        val trackDao = database.trackDao()
        val insertedId = remoteFileDao.upsertAll(
            listOf(remoteFile(folderId = folderId, path = "/Music/Old/song.flac")),
        ).single()
        val inserted = assertNotNull(remoteFileDao.findByPath(1, "/Music/Old/song.flac"))
        assertEquals(insertedId, inserted.id)

        trackDao.upsertAll(listOf(track(id = 101, remoteFileId = inserted.id)))
        assertEquals(1, trackDao.count())

        remoteFileDao.upsertAll(
            listOf(
                inserted.copy(
                    canonicalPath = "/Music/New/song.flac",
                    displayPath = "/Music/New/song.flac",
                    lastSeenScanId = "scan-move",
                ),
            ),
        )

        assertNull(remoteFileDao.findByPath(1, "/Music/Old/song.flac"))
        val moved = assertNotNull(remoteFileDao.findByPath(1, "/Music/New/song.flac"))
        assertEquals(inserted.id, moved.id)
        assertEquals(101, trackDao.findByRemoteFileIds(listOf(moved.id)).single().id)

        remoteFileDao.markMissingDeleted(folderId, "scan-other")
        assertTrue(trackDao.page(limit = 10, offset = 0).isEmpty())

        remoteFileDao.markSeen(listOf(moved.id), "scan-restored")
        assertEquals(listOf(101L), trackDao.page(limit = 10, offset = 0).map { it.id })
    }

    @Test
    fun deltaDeletionUsesStableRemoteIdAndCursorAdvancesTransactionally() =
        withDatabase { database ->
            val folderId = seedStorageAndFolder(database)
            val remoteFileDao = database.remoteFileDao()
            val syncDao = database.syncDao()
            val fileId = remoteFileDao.upsertAll(
                listOf(
                    remoteFile(
                        folderId = folderId,
                        path = "/Music/Before.flac",
                        remoteId = "drive-item-1",
                    ),
                ),
            ).single()
            database.trackDao().upsertAll(listOf(track(id = 101, remoteFileId = fileId)))

            database.useWriterConnection { connection ->
                connection.immediateTransaction {
                    assertEquals(
                        1,
                        remoteFileDao.markDeletedByRemoteIds(
                            storageId = 1,
                            remoteIds = listOf("drive-item-1"),
                        ),
                    )
                    syncDao.upsertCursor(
                        SyncCursorEntity(
                            selectedFolderId = folderId,
                            deltaLink = "https://graph.microsoft.com/delta/final",
                            continuationToken = null,
                            lastScanId = "delta-1",
                            lastSyncAt = 100,
                        ),
                    )
                }
            }

            assertTrue(database.trackDao().page(limit = 10, offset = 0).isEmpty())
            assertEquals(
                "https://graph.microsoft.com/delta/final",
                syncDao.getCursor(folderId)?.deltaLink,
            )
        }

    @Test
    fun writerTransactionRollsBackOnFailure() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        val failure = runCatching {
            database.useWriterConnection { connection ->
                connection.immediateTransaction {
                    database.remoteFileDao().upsertAll(
                        listOf(remoteFile(folderId = folderId, path = "/Music/rollback.flac")),
                    )
                    error("force rollback")
                }
            }
        }.exceptionOrNull()

        assertIs<IllegalStateException>(failure)
        assertNull(database.remoteFileDao().findByPath(1, "/Music/rollback.flac"))
        assertEquals(0, database.remoteFileDao().countForFolder(folderId))
    }

    @Test
    fun lyricsAndRawMetadataCanBeReplacedTransactionally() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        database.remoteFileDao().upsertAll(
            listOf(remoteFile(id = 1, folderId = folderId, path = "/Music/song.flac")),
        )
        database.trackDao().upsertAll(listOf(track(id = 1, remoteFileId = 1)))
        val metadataDao = database.metadataDao()

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                metadataDao.upsertLyrics(
                    listOf(
                        LyricsEntity(
                            trackId = 1,
                            format = "TEXT",
                            language = "eng",
                            synchronized = false,
                            content = "Old",
                            sourcePath = null,
                            updatedAt = 1,
                        ),
                    ),
                )
                metadataDao.upsertRawMetadata(
                    listOf(
                        RawMetadataEntity(
                            trackId = 1,
                            tagKey = "Composer",
                            value = "Old",
                            locale = null,
                            description = null,
                        ),
                    ),
                )
            }
        }
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                metadataDao.deleteLyricsForTracks(listOf(1))
                metadataDao.deleteRawMetadataForTracks(listOf(1))
                metadataDao.upsertLyrics(
                    listOf(
                        LyricsEntity(
                            trackId = 1,
                            format = "LRC",
                            language = "zho",
                            synchronized = true,
                            content = "[00:01.00]New",
                            sourcePath = null,
                            updatedAt = 2,
                        ),
                    ),
                )
                metadataDao.upsertRawMetadata(
                    listOf(
                        RawMetadataEntity(
                            trackId = 1,
                            tagKey = "Composer",
                            value = "New",
                            locale = "zho",
                            description = "main",
                        ),
                    ),
                )
            }
        }

        assertEquals("[00:01.00]New", metadataDao.getLyrics(1)?.content)
        assertEquals("New", metadataDao.rawMetadataForTrack(1).single().value)
    }

    @Test
    fun normalizedDimensionsAndRelationshipsPersist() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        database.remoteFileDao().upsertAll(
            listOf(remoteFile(id = 1, folderId = folderId, path = "/Music/song.flac")),
        )
        val metadataDao = database.metadataDao()
        metadataDao.insertAlbums(
            listOf(
                AlbumEntity(
                    name = "Album",
                    normalizedName = "album",
                    sortName = null,
                    year = 2026,
                    artworkId = null,
                ),
            ),
        )
        metadataDao.insertArtists(
            listOf(
                ArtistEntity(name = "Primary", normalizedName = "primary", sortName = null),
                ArtistEntity(name = "Guest", normalizedName = "guest", sortName = null),
                ArtistEntity(name = "Album Artist", normalizedName = "album artist", sortName = null),
            ),
        )
        metadataDao.insertGenres(
            listOf(GenreEntity(name = "Jazz", normalizedName = "jazz")),
        )
        val album = metadataDao.findAlbumsByNormalizedNames(listOf("album")).single()
        val artists = metadataDao.findArtistsByNormalizedNames(
            listOf("primary", "guest", "album artist"),
        ).associateBy { it.normalizedName }
        val genre = metadataDao.findGenresByNormalizedNames(listOf("jazz")).single()
        database.trackDao().upsertAll(
            listOf(track(id = 1, remoteFileId = 1).copy(albumId = album.id)),
        )
        metadataDao.upsertTrackArtists(
            listOf(
                TrackArtistCrossRef(1, artists.getValue("primary").id, 0),
                TrackArtistCrossRef(1, artists.getValue("guest").id, 1),
            ),
        )
        metadataDao.upsertAlbumArtists(
            listOf(AlbumArtistCrossRef(album.id, artists.getValue("album artist").id, 0)),
        )
        metadataDao.upsertTrackGenres(
            listOf(TrackGenreCrossRef(1, genre.id)),
        )

        assertEquals(listOf("Primary", "Guest"), metadataDao.artistNamesForTrack(1))
        assertEquals(listOf("Album Artist"), metadataDao.artistNamesForAlbum(album.id))
        assertEquals(listOf("Jazz"), metadataDao.genreNamesForTrack(1))
    }

    @Test
    fun importsAndPagesFiftyThousandTracksWithinBoundedTime() = withDatabase { database ->
        val folderId = seedStorageAndFolder(database)
        val total = 50_000
        val batchSize = 500
        val startedAt = System.nanoTime()

        repeat(total / batchSize) { batchIndex ->
            val first = batchIndex * batchSize + 1
            val files = (first until first + batchSize).map { index ->
                remoteFile(
                    id = index.toLong(),
                    folderId = folderId,
                    path = "/Music/track-${index.toString().padStart(5, '0')}.flac",
                    remoteId = "item-$index",
                )
            }
            val tracks = files.map { file ->
                track(id = file.id, remoteFileId = file.id)
            }
            database.useWriterConnection { connection ->
                connection.immediateTransaction {
                    database.remoteFileDao().upsertAll(files)
                    database.trackDao().upsertAll(tracks)
                }
            }
        }

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        assertEquals(total.toLong(), database.remoteFileDao().countForFolder(folderId))
        assertEquals(total.toLong(), database.trackDao().count())
        val lastPage = database.trackDao().page(limit = 200, offset = total - 200)
        assertEquals(200, lastPage.size)
        assertTrue(elapsedMs < 60_000, "50,000-track import took ${elapsedMs}ms")
        println("room_50000_import_ms=$elapsedMs")
    }

    private fun withDatabase(block: suspend (TideTuneDatabase) -> Unit) = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<TideTuneDatabase> {
            TideTuneDatabaseConstructor.initialize()
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

    private fun roomLibraryStore(database: TideTuneDatabase) = RoomLibraryStore(
        database = database,
        trackDao = database.trackDao(),
        remoteFileDao = database.remoteFileDao(),
        playlistDao = database.playlistDao(),
        metadataDao = database.metadataDao(),
    )

    private suspend fun seedStorageAndFolder(database: TideTuneDatabase): Long {
        database.storageDao().upsert(
            StorageEntity(
                id = 1,
                type = "WEBDAV",
                displayName = "Test",
                baseUrl = "https://example.invalid/dav",
                credentialRef = "test-credential",
                username = "",
                isAnonymous = true,
                musicCount = 0,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        database.selectedFolderDao().upsert(
            SelectedFolderEntity(
                storageId = 1,
                remoteId = "folder-1",
                canonicalPath = "/Music",
                displayPath = "/Music",
                deltaLink = null,
                lastSyncAt = null,
                syncStatus = "RUNNING",
            ),
        )
        return assertNotNull(database.selectedFolderDao().findByPath(1, "/Music")).id
    }

    private fun remoteFile(
        folderId: Long,
        path: String,
        id: Long = 0,
        remoteId: String = "item-1",
    ) = RemoteFileEntity(
        id = id,
        storageId = 1,
        selectedFolderId = folderId,
        remoteId = remoteId,
        parentRemoteId = "folder-1",
        canonicalPath = path,
        displayPath = path,
        fileName = path.substringAfterLast('/'),
        extension = "flac",
        mimeType = "audio/flac",
        size = 1_000,
        etag = "\"etag-$remoteId\"",
        ctag = null,
        createdAt = 1,
        modifiedAt = 1,
        contentHash = null,
        isDeleted = false,
        lastSeenScanId = "scan-1",
    )

    private fun track(
        id: Long,
        remoteFileId: Long?,
        sourceStorageId: Long? = null,
        sourcePath: String? = null,
    ) = TrackEntity(
        id = id,
        remoteFileId = remoteFileId,
        sourceStorageId = sourceStorageId,
        sourcePath = sourcePath,
        title = "Track ${id.toString().padStart(5, '0')}",
        sortTitle = null,
        albumId = null,
        albumArtist = null,
        composer = null,
        comment = null,
        grouping = null,
        durationMs = 180_000,
        discNumber = 1,
        discTotal = 1,
        trackNumber = null,
        trackTotal = null,
        year = 2026,
        date = "2026",
        sampleRate = 48_000,
        bitRate = 900_000,
        bitsPerSample = 24,
        channels = 2,
        channelLayout = null,
        codec = "FLAC",
        container = "FLAC",
        lossless = true,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun storageEntry(
        path: String,
        name: String,
    ) = StorageEntry(
        storageId = StorageId(1),
        name = name,
        path = path,
        size = 1_000uL,
        isDir = false,
        remoteId = "entry-$name",
        parentRemoteId = "folder-1",
        mimeType = "audio/flac",
        etag = "\"etag-$name\"",
        ctag = null,
        createdAt = 1,
        modifiedAt = 1,
    )

    private fun columns(connection: SQLiteConnection, table: String): Set<String> = buildSet {
        connection.prepare("PRAGMA table_info($table)").use { statement ->
            while (statement.step()) {
                add(statement.getText(1))
            }
        }
    }

    private fun SQLiteConnection.execute(sql: String) {
        prepare(sql).use { statement ->
            statement.step()
        }
    }
}

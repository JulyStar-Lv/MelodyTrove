package com.github.tidetunes.core.data.media

import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.ArtworkCacheKey
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.database.ArtworkEntity
import com.github.tidetunes.database.TrackEntity
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.legacyStorageArtworkMediaId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import uniffi.tidetunes_core.StorageEntryLoc
import uniffi.tidetunes_core.StorageId

class LegacyArtworkRepositoryTest {
    @Test
    fun resolvesLibraryTrackArtworkThroughTrackLocationLookup() = runBlocking {
        val loc = Artwork.LibraryTrack(trackId = 7).resolveLegacyStorageEntryLoc { trackId ->
            StorageEntryLoc(
                storageId = StorageId(trackId + 1),
                path = "/music/$trackId.flac",
            )
        }

        assertEquals(
            StorageEntryLoc(
                storageId = StorageId(8),
                path = "/music/7.flac",
            ),
            loc,
        )
    }

    @Test
    fun resolvesLegacyStorageEntryArtworkDirectly() = runBlocking {
        val loc = Artwork.LegacyStorageEntry(
            storageId = 5,
            path = "/covers/now.jpg",
        ).resolveLegacyStorageEntryLoc {
            error("Direct storage entry artwork should not query track locations.")
        }

        assertEquals(
            StorageEntryLoc(
                storageId = StorageId(5),
                path = "/covers/now.jpg",
            ),
            loc,
        )
    }

    @Test
    fun resolvesSourceMediaArtworkThroughStableMediaId() = runBlocking {
        val loc = Artwork.SourceMedia(
            mediaId = legacyStorageArtworkMediaId(
                sourceId = BuiltInSourceIds.WebDav,
                accountId = SourceAccountId("storage:5"),
                path = "/covers/now.jpg",
            )
        ).resolveLegacyStorageEntryLoc {
            error("Source media artwork should not query track locations.")
        }

        assertEquals(
            StorageEntryLoc(
                storageId = StorageId(5),
                path = "/covers/now.jpg",
            ),
            loc,
        )
    }

    @Test
    fun leavesLibraryCoverArtworkUnsupportedUntilCoverIndexIsMigrated() = runBlocking {
        val loc = Artwork.LibraryCover(trackId = 7).resolveLegacyStorageEntryLoc {
            error("Library cover artwork should not query track locations yet.")
        }

        assertNull(loc)
    }

    @Test
    fun resolvesLibraryTrackArtworkCacheKeyFromRoomMetadata() = runBlocking {
        val cacheKey = Artwork.LibraryTrack(trackId = 7).resolveRoomArtworkCacheKey(
            findTrack = { error("Track lookup should not run when track artwork exists.") },
            findTrackArtwork = { trackId ->
                assertEquals(7, trackId)
                artworkEntity(trackId = trackId)
            },
            findAlbumArtwork = { error("Album artwork lookup should not run when track artwork exists.") },
        )

        assertEquals(
            ArtworkCacheKey(
                contentHash = "hash-7",
                localPath = "/cache/artwork/hash-7.jpg",
                thumbnailPath = "/cache/artwork/hash-7-thumb.jpg",
                width = 512,
                height = 512,
                mimeType = "image/jpeg",
                pictureType = "CoverFront",
            ),
            cacheKey,
        )
    }

    @Test
    fun resolvesLibraryCoverArtworkCacheKeyFromTrackMetadata() = runBlocking {
        val cacheKey = Artwork.LibraryCover(trackId = 8).resolveRoomArtworkCacheKey(
            findTrack = { error("Track lookup should not run when cover artwork exists.") },
            findTrackArtwork = { trackId ->
                assertEquals(8, trackId)
                artworkEntity(trackId = trackId)
            },
            findAlbumArtwork = { error("Album artwork lookup should not run when cover artwork exists.") },
        )

        assertEquals("hash-8", cacheKey?.contentHash)
        assertEquals("/cache/artwork/hash-8.jpg", cacheKey?.localPath)
    }

    @Test
    fun resolvesLibraryTrackArtworkCacheKeyFromAlbumMetadataFallback() = runBlocking {
        val cacheKey = Artwork.LibraryTrack(trackId = 9).resolveRoomArtworkCacheKey(
            findTrack = { trackId ->
                trackEntity(id = trackId, albumId = 90)
            },
            findTrackArtwork = {
                null
            },
            findAlbumArtwork = { albumId ->
                assertEquals(90, albumId)
                artworkEntity(trackId = null, albumId = albumId)
            },
        )

        assertEquals("hash-album-90", cacheKey?.contentHash)
        assertEquals("/cache/artwork/hash-album-90.jpg", cacheKey?.localPath)
    }

    @Test
    fun leavesLegacyStorageEntryCacheKeyUnsupportedUntilItIsPersisted() = runBlocking {
        val cacheKey = Artwork.LegacyStorageEntry(
            storageId = 1,
            path = "/Music/cover.jpg",
        ).resolveRoomArtworkCacheKey(
            findTrack = { error("Direct storage entry artwork should not query tracks.") },
            findTrackArtwork = {
                error("Direct storage entry artwork should not query track artwork metadata.")
            },
            findAlbumArtwork = {
                error("Direct storage entry artwork should not query album artwork metadata.")
            },
        )

        assertNull(cacheKey)
    }

    @Test
    fun leavesSourceMediaCacheKeyUnsupportedUntilItIsPersisted() = runBlocking {
        val cacheKey = Artwork.SourceMedia(
            mediaId = legacyStorageArtworkMediaId(
                sourceId = BuiltInSourceIds.WebDav,
                accountId = SourceAccountId("storage:1"),
                path = "/Music/cover.jpg",
            )
        ).resolveRoomArtworkCacheKey(
            findTrack = { error("Source media artwork should not query tracks.") },
            findTrackArtwork = {
                error("Source media artwork should not query track artwork metadata.")
            },
            findAlbumArtwork = {
                error("Source media artwork should not query album artwork metadata.")
            },
        )

        assertNull(cacheKey)
    }

    private fun artworkEntity(
        trackId: Long?,
        albumId: Long? = null,
    ): ArtworkEntity {
        val suffix = trackId?.toString() ?: "album-$albumId"
        return ArtworkEntity(
            trackId = trackId,
            albumId = albumId,
            contentHash = "hash-$suffix",
            localPath = "/cache/artwork/hash-$suffix.jpg",
            thumbnailPath = "/cache/artwork/hash-$suffix-thumb.jpg",
            width = 512,
            height = 512,
            mimeType = "image/jpeg",
            pictureType = "CoverFront",
        )
    }

    private fun trackEntity(
        id: Long,
        albumId: Long?,
    ): TrackEntity {
        return TrackEntity(
            id = id,
            title = "Track $id",
            sortTitle = null,
            albumId = albumId,
            albumArtist = null,
            composer = null,
            comment = null,
            grouping = null,
            durationMs = null,
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
            codec = null,
            container = null,
            lossless = null,
            createdAt = 1,
            updatedAt = 1,
        )
    }
}

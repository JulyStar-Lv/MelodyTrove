package com.github.tidetunes.core.data

import com.github.tidetunes.database.TrackEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryRepositoryTest {
    @Test
    fun mapsRoomTrackToLibraryItemWithoutRemoteData() {
        val item = track(
            artist = "Track Artist",
            albumArtist = "Album Artist",
            composer = "Composer",
        ).toLibraryTrackItem()

        assertEquals(99L, item.id)
        assertEquals("Song", item.title)
        assertEquals("Track Artist", item.artist)
        assertEquals(123_000L, item.durationMs)
        assertNull(item.mediaId)
    }

    @Test
    fun fallsBackToComposerWhenAlbumArtistIsBlank() {
        val item = track(
            artist = " ",
            albumArtist = " ",
            composer = "Composer",
        ).toLibraryTrackItem()

        assertEquals("Composer", item.artist)
    }

    private fun track(
        artist: String?,
        albumArtist: String?,
        composer: String?,
    ) = TrackEntity(
        id = 99,
        title = "Song",
        sortTitle = null,
        albumId = null,
        albumArtist = albumArtist,
        composer = composer,
        comment = null,
        grouping = null,
        durationMs = 123_000,
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
        updatedAt = 2,
        artist = artist,
    )
}

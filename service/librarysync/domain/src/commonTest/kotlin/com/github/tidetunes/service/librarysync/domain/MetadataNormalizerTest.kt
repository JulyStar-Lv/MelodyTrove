package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataNormalizerTest {
    private val normalizer = DefaultMetadataNormalizer

    @Test
    fun normalizesWithExplicitTitle() {
        val raw = rawItem(trackTitle = "01 - Sunrise", name = "01 - Sunrise.flac")
        val result = normalizer.normalize(raw)

        assertEquals("01 - Sunrise", result.title)
        assertEquals("/Music/01 - Sunrise.flac", result.sourcePath)
    }

    @Test
    fun derivesTitleFromFileNameWhenTitleIsNull() {
        val raw = rawItem(trackTitle = null, name = "Sunset Boulevard.mp3")
        val result = normalizer.normalize(raw)

        assertEquals("Sunset Boulevard", result.title)
    }

    @Test
    fun derivesTitleFromFileNameWhenTitleIsBlank() {
        val raw = rawItem(trackTitle = "   ", name = "Ocean Drive.ogg")
        val result = normalizer.normalize(raw)

        assertEquals("Ocean Drive", result.title)
    }

    @Test
    fun stripsMultipleDotsInExtension() {
        val raw = rawItem(trackTitle = null, name = "Track 01 - Artist.flac.mp3")
        val result = normalizer.normalize(raw)

        assertEquals("Track 01 - Artist.flac", result.title)
    }

    @Test
    fun passesThroughBlankArtistAsNull() {
        val raw = rawItem(artist = "", name = "track.flac")
        val result = normalizer.normalize(raw)
        assertNull(result.artist)
    }

    @Test
    fun preservesReplayGainValues() {
        val raw = rawItem(replayGainTrackDb = -3.5f, replayGainAlbumDb = -2.1f, name = "track.flac")
        val result = normalizer.normalize(raw)

        assertEquals(-3.5f, result.replayGainTrackDb)
        assertEquals(-2.1f, result.replayGainAlbumDb)
    }

    @Test
    fun passesThroughNullFieldsCorrectly() {
        val raw = rawItem(
            artist = null, album = null, genre = null,
            year = null, durationMs = null, bitRateKbps = null,
            replayGainTrackDb = null, replayGainAlbumDb = null,
            name = "track.flac",
        )
        val result = normalizer.normalize(raw)

        assertNull(result.artist)
        assertNull(result.album)
        assertNull(result.genre)
        assertNull(result.year)
        assertNull(result.durationMs)
    }

    @Test
    fun preservesSourceModifiedTimestamp() {
        val raw = rawItem(modifiedAtEpochMs = 1719700000000L, name = "track.flac")
        val result = normalizer.normalize(raw)

        assertEquals(1719700000000L, result.sourceModifiedAtEpochMs)
    }

    private fun rawItem(
        trackTitle: String? = "Test Track",
        name: String,
        artist: String? = "Test Artist",
        album: String? = "Test Album",
        genre: String? = null,
        year: Int? = null,
        durationMs: Long? = 240_000,
        bitRateKbps: Int? = 320,
        replayGainTrackDb: Float? = null,
        replayGainAlbumDb: Float? = null,
        modifiedAtEpochMs: Long? = null,
    ): RawMetadataItem {
        return RawMetadataItem(
            accountId = SourceAccountId("storage:1"),
            remoteId = "remote-1",
            path = "/Music/$name",
            name = name,
            sizeBytes = 10_000_000u,
            mimeType = "audio/flac",
            modifiedAtEpochMs = modifiedAtEpochMs,
            trackTitle = trackTitle,
            trackNumber = 1,
            discNumber = 1,
            artist = artist,
            albumArtist = artist,
            album = album,
            genre = genre,
            year = year,
            durationMs = durationMs,
            bitRateKbps = bitRateKbps,
            sampleRateHz = 44100,
            bitDepth = 16,
            channels = 2,
            coverHash = null,
            composer = null,
            bpm = null,
            replayGainTrackDb = replayGainTrackDb,
            replayGainAlbumDb = replayGainAlbumDb,
            lyricRaw = null,
            lyricFormat = null,
        )
    }
}

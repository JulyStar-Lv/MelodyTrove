package com.github.tidetunes.service.librarysync.domain

import com.github.tidetunes.core.domain.model.SourceAccountId
import kotlin.test.Test
import kotlin.test.assertEquals

class DuplicateMatcherTest {
    private val matcher = DefaultDuplicateMatcher

    @Test
    fun newTrackWhenLibraryIsEmpty() {
        val result = matcher.match(item("01 - Sunrise.flac"), emptyList())

        assertEquals(MatchResult.New, result.result)
    }

    @Test
    fun updateWhenSourcePathAndAccountMatch() {
        val existing = listOf(item("/a/01 - Sunrise.flac", account = "storage:1"))
        val result = matcher.match(
            item("/a/01 - Sunrise.flac", account = "storage:1"),
            existing,
        )

        assertEquals(MatchResult.Update, result.result)
    }

    @Test
    fun duplicateWhenTitleArtistAlbumAndDurationMatch() {
        val existing = listOf(
            item(
                path = "/Music/Different File.flac",
                title = "Sunrise",
                artist = "Ocean Waves",
                album = "Morning",
                duration = 241_000L,
            )
        )
        val result = matcher.match(
            item(
                path = "/Other/Sunrise.mp3",
                title = "Sunrise",
                artist = "Ocean Waves",
                album = "Morning",
                duration = 242_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.Duplicate, result.result)
    }

    @Test
    fun newTrackWhenArtistDiffers() {
        val existing = listOf(item(
            path = "/a/track.flac",
            title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = 240_000L,
        ))
        val result = matcher.match(
            item(
                path = "/b/track.flac",
                title = "Sunrise", artist = "Different Artist", album = "Morning", duration = 240_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.New, result.result)
    }

    @Test
    fun newTrackWhenAlbumDiffers() {
        val existing = listOf(item(
            path = "/a/track.flac",
            title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = 240_000L,
        ))
        val result = matcher.match(
            item(
                path = "/b/track.flac",
                title = "Sunrise", artist = "Ocean Waves", album = "Evening", duration = 240_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.New, result.result)
    }

    @Test
    fun newTrackWhenDurationDiffersBeyondBucket() {
        val existing = listOf(item(
            path = "/a/track.flac",
            title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = 240_000L,
        ))
        val result = matcher.match(
            item(
                path = "/b/track.flac",
                title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = 260_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.New, result.result)
    }

    @Test
    fun updatePriorityOverDuplicateForSamePath() {
        val existing = listOf(
            item(
                path = "/Music/Sunrise.flac",
                title = "Old Title",
                artist = "Ocean Waves",
                album = "Morning",
                duration = 240_000L,
            ),
            item(
                path = "/Music/Other.flac",
                title = "Sunrise",
                artist = "Ocean Waves",
                album = "Morning",
                duration = 240_000L,
            ),
        )
        val result = matcher.match(
            item(
                path = "/Music/Sunrise.flac",
                title = "Sunrise",
                artist = "Ocean Waves",
                album = "Morning",
                duration = 240_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.Update, result.result)
    }

    @Test
    fun caseInsensitiveAndWhitespaceNormalizedMatching() {
        val existing = listOf(item(
            path = "/a/track.flac",
            title = "  SUNRISE  ", artist = "ocean waves", album = "morning", duration = 240_000L,
        ))
        val result = matcher.match(
            item(
                path = "/b/track.flac",
                title = "Sunrise", artist = "Ocean Waves", album = " Morning ", duration = 240_000L,
            ),
            existing,
        )

        assertEquals(MatchResult.Duplicate, result.result)
    }

    @Test
    fun nullDurationsAlwaysMatchForDuplicateDetection() {
        val existing = listOf(item(
            path = "/a/track.flac",
            title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = null,
        ))
        val result = matcher.match(
            item(
                path = "/b/track.flac",
                title = "Sunrise", artist = "Ocean Waves", album = "Morning", duration = null,
            ),
            existing,
        )

        assertEquals(MatchResult.Duplicate, result.result)
    }

    private fun item(
        path: String,
        title: String = "Test Track",
        artist: String? = null,
        album: String? = null,
        duration: Long? = null,
        account: String = "storage:1",
    ): NormalizedMetadataItem {
        return NormalizedMetadataItem(
            accountId = SourceAccountId(account),
            sourcePath = path,
            title = title,
            trackNumber = null,
            discNumber = null,
            artist = artist,
            albumArtist = null,
            album = album,
            genre = null,
            year = null,
            durationMs = duration,
            bitRateKbps = null,
            sampleRateHz = null,
            bitDepth = null,
            channels = null,
            composer = null,
            bpm = null,
            coverHash = null,
            replayGainTrackDb = null,
            replayGainAlbumDb = null,
            lyricRaw = null,
            lyricFormat = null,
            sourceModifiedAtEpochMs = null,
        )
    }
}

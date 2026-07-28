package io.github.julystar.musicapp.source.api

import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.SourceAccountId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LegacyStorageMediaIdTest {
    @Test
    fun trackMediaIdRoundTripsAccountAndPath() {
        val accountId = SourceAccountId("storage:42")
        val mediaId = legacyStorageTrackMediaId(
            sourceId = BuiltInSourceIds.WebDav,
            accountId = accountId,
            path = "/Music/A&B/01 - Tide.flac",
        )

        assertEquals(BuiltInSourceIds.WebDav, mediaId.sourceId)
        assertEquals(MediaType.Track, mediaId.mediaType)
        assertEquals(
            LegacyStoragePlaybackTarget(
                accountId = accountId,
                path = "/Music/A&B/01 - Tide.flac",
            ),
            mediaId.toLegacyStoragePlaybackTarget(),
        )
        assertNull(mediaId.toLegacyStorageArtworkTarget())
    }

    @Test
    fun artworkMediaIdRoundTripsAccountAndPath() {
        val accountId = SourceAccountId("storage:7")
        val mediaId = legacyStorageArtworkMediaId(
            sourceId = BuiltInSourceIds.OneDrive,
            accountId = accountId,
            path = "/Cover Art/front cover.png",
        )

        assertEquals(BuiltInSourceIds.OneDrive, mediaId.sourceId)
        assertEquals(MediaType.Image, mediaId.mediaType)
        assertEquals(
            LegacyStoragePlaybackTarget(
                accountId = accountId,
                path = "/Cover Art/front cover.png",
            ),
            mediaId.toLegacyStorageArtworkTarget(),
        )
        assertNull(mediaId.toLegacyStoragePlaybackTarget())
    }

    @Test
    fun blankPathIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            legacyStorageTrackMediaId(
                sourceId = BuiltInSourceIds.Local,
                accountId = SourceAccountId("storage:1"),
                path = "",
            )
        }
    }
}

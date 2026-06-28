package com.github.tidetunes.service.playback.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlaybackEngineModelsTest {
    @Test
    fun playbackResourceRejectsBlankUri() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackEngineResource(uri = "")
        }
    }

    @Test
    fun playbackResourceRejectsBlankHeaderName() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackEngineResource(
                uri = "https://example.test/track.flac",
                headers = mapOf("" to "value"),
            )
        }
    }

    @Test
    fun playbackResourceRejectsNegativeExpiration() {
        assertFailsWith<IllegalArgumentException> {
            PlaybackEngineResource(
                uri = "https://example.test/track.flac",
                expiresAtEpochMs = -1,
            )
        }
    }

    @Test
    fun playbackResourceReportsExpiration() {
        val resource = PlaybackEngineResource(
            uri = "https://example.test/track.flac",
            expiresAtEpochMs = 1_000,
        )

        assertFalse(resource.isExpired(nowEpochMs = 999))
        assertTrue(resource.isExpired(nowEpochMs = 1_000))
        assertTrue(resource.isExpired(nowEpochMs = 1_001))
    }

    @Test
    fun playbackResourceWithoutExpirationDoesNotExpire() {
        val resource = PlaybackEngineResource(uri = "file:///music/track.flac")

        assertFalse(resource.isExpired(nowEpochMs = Long.MAX_VALUE))
    }

    @Test
    fun loadRequestKeepsPlayableItemAndResourceSeparated() {
        val item = PlayableItem(title = "Track", libraryTrackId = 1)
        val resource = PlaybackEngineResource(uri = "file:///music/track.flac")

        val request = PlaybackEngineLoadRequest(item = item, resource = resource)

        assertSame(item, request.item)
        assertSame(resource, request.resource)
    }
}

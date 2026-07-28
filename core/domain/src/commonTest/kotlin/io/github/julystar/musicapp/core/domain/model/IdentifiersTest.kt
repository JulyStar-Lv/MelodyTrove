package io.github.julystar.musicapp.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IdentifiersTest {
    @Test
    fun mediaIdSerializesAsStableDomainModel() {
        val mediaId = MediaId(
            sourceId = SourceId("webdav"),
            mediaType = MediaType.Track,
            remoteId = "music/song.flac",
        )

        val encoded = Json.encodeToString(mediaId)
        val decoded = Json.decodeFromString<MediaId>(encoded)

        assertEquals(mediaId, decoded)
    }

    @Test
    fun idsRejectBlankValues() {
        assertFailsWith<IllegalArgumentException> {
            SourceId("")
        }
        assertFailsWith<IllegalArgumentException> {
            SourceAccountId(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            MediaId(
                sourceId = SourceId("webdav"),
                mediaType = MediaType.Track,
                remoteId = "",
            )
        }
    }

    @Test
    fun storageSourceAccountIdRoundTripsNumericId() {
        val accountId = storageSourceAccountId(42)

        assertEquals(SourceAccountId("storage:42"), accountId)
        assertEquals(42, accountId.toStorageRouteIdOrNull())
        assertNull(SourceAccountId("webdav:42").toStorageRouteIdOrNull())
    }
}

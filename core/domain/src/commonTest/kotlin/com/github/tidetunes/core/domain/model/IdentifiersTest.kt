package com.github.tidetunes.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}

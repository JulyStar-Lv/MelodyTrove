package com.github.tidetunes.plugin.management

import com.github.tidetunes.source.api.MetaLyricLine
import com.github.tidetunes.source.api.MetaLyrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ManualMetadataServiceTest {
    @Test
    fun prefersPlainLrcReturnedByPlugin() {
        val entity = assertNotNull(
            MetaLyrics(
                lines = listOf(MetaLyricLine(text = "Parsed", startMs = 1_000)),
                rawPlainLrc = "[00:01.00]Raw",
            ).toEntity(trackId = 42, updatedAt = 7),
        )

        assertEquals(42, entity.trackId)
        assertEquals("LRC", entity.format)
        assertEquals("[00:01.00]Raw", entity.content)
        assertEquals(true, entity.synchronized)
    }

    @Test
    fun buildsLrcFromStructuredLines() {
        val entity = assertNotNull(
            MetaLyrics(
                lines = listOf(
                    MetaLyricLine(text = "First", startMs = 1_230),
                    MetaLyricLine(text = "Second", startMs = 61_090),
                ),
            ).toEntity(trackId = 1, updatedAt = 2),
        )

        assertEquals("[00:01.23]First\n[01:01.09]Second", entity.content)
        assertEquals(true, entity.synchronized)
    }

    @Test
    fun applyMessageDoesNotExposePluginFailureDetails() {
        val message = metadataApplyMessage(
            title = "兰亭序",
            lyricFailures = listOf(
                MetadataLookupFailure(
                    sourceId = "com.applemusic.source",
                    operation = MetadataLookupOperation.GET_LYRICS,
                    message = "HTTP status 500\n    at __lyricoHostCall (native)",
                    errorType = "HostApi",
                ),
            ),
        )

        assertEquals(
            "Applied metadata for 兰亭序. Lyrics were unavailable from the selected source.",
            message,
        )
    }
}

package com.github.tidetunes.core

import androidx.media3.common.MediaMetadata
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicPlayerUtilTest {
    @Test
    fun artworkDataIsPublishedWithoutDroppingMetadata() {
        val artworkData = byteArrayOf(1, 2, 3)
        val metadata = MediaMetadata.Builder()
            .setTitle("Track")
            .setArtist("Artist")
            .build()

        val updated = metadata.withArtworkData(artworkData)

        assertEquals("Track", updated.title)
        assertEquals("Artist", updated.artist)
        assertNull(updated.artworkUri)
        assertContentEquals(artworkData, updated.artworkData)
        assertEquals(MediaMetadata.PICTURE_TYPE_FRONT_COVER, updated.artworkDataType)
    }
}

package com.github.tidetunes.source.storage

import com.github.tidetunes.core.domain.model.MetadataScanMode
import com.github.tidetunes.core.domain.model.toOptions
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataRepositoryOptionsTest {
    @Test
    fun convertsCanonicalOptionsToUniFfiRecordWithoutRestoringDefaults() {
        val fast = MetadataScanMode.Fast.toOptions().toRustOptions()
        assertFalse(fast.readArtwork)
        assertFalse(fast.readLyrics)
        assertFalse(fast.readRawMetadata)

        val standard = MetadataScanMode.Standard.toOptions().toRustOptions()
        assertFalse(standard.readArtwork)
        assertTrue(standard.readLyrics)
        assertFalse(standard.readRawMetadata)

        val full = MetadataScanMode.Full.toOptions().toRustOptions()
        assertTrue(full.readArtwork)
        assertTrue(full.readLyrics)
        assertTrue(full.readRawMetadata)
    }
}

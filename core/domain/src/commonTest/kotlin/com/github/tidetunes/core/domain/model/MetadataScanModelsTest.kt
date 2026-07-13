package com.github.tidetunes.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataScanModelsTest {
    @Test
    fun mapsScanModesToOneCanonicalOptionsModel() {
        assertEquals(
            MetadataScanOptions(false, false, false),
            MetadataScanMode.Fast.toOptions(),
        )
        assertEquals(
            MetadataScanOptions(false, true, false),
            MetadataScanMode.Standard.toOptions(),
        )
        assertEquals(
            MetadataScanOptions(true, true, true),
            MetadataScanMode.Full.toOptions(),
        )
    }

    @Test
    fun mapsRefreshTargetsToMinimumReadOptions() {
        assertEquals(
            MetadataScanOptions(true, false, false),
            MetadataRefreshTarget.Artwork.toOptions(),
        )
        assertEquals(
            MetadataScanOptions(false, true, false),
            MetadataRefreshTarget.Lyrics.toOptions(),
        )
    }

    @Test
    fun webDavSettingDoesNotChangeLocalOrOtherSourceScanning() {
        val settings = AppSettings(webDavMetadataScanMode = MetadataScanMode.Fast)

        assertEquals(MetadataScanMode.Fast, settings.metadataScanModeFor(isWebDav = true))
        assertEquals(MetadataScanMode.Full, settings.metadataScanModeFor(isWebDav = false))
    }
}

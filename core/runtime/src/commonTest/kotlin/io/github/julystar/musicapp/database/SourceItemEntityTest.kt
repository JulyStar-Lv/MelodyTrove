package io.github.julystar.musicapp.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceItemEntityTest {
    @Test
    fun sourceItemRequiresProviderItemIdOrCanonicalPath() {
        assertFailsWith<IllegalArgumentException> {
            sourceItem(providerItemId = null, canonicalPath = null)
        }
    }

    @Test
    fun sourceItemCanUseProviderIdentityWithoutPath() {
        val item = sourceItem(
            providerItemId = "drive-item-1",
            canonicalPath = null,
        )

        assertEquals("drive-item-1", item.providerItemId)
        assertEquals(null, item.canonicalPath)
    }

    @Test
    fun sourceItemCanUseCanonicalPathWithoutProviderIdentity() {
        val item = sourceItem(
            providerItemId = null,
            canonicalPath = "/Music/Track.flac",
        )

        assertEquals(null, item.providerItemId)
        assertEquals("/Music/Track.flac", item.canonicalPath)
    }

    private fun sourceItem(
        providerItemId: String?,
        canonicalPath: String?,
    ) = SourceItemEntity(
        id = 7,
        sourceAccountId = 1,
        libraryRootId = 2,
        itemType = SourceItemTypes.Track,
        providerItemId = providerItemId,
        parentProviderItemId = null,
        canonicalPath = canonicalPath,
        displayPath = canonicalPath,
        displayName = "Track.flac",
        mimeType = "audio/flac",
        sizeBytes = 33_731_890,
        etag = "\"etag\"",
        revision = null,
        createdAtRemote = 1_779_810_276_000,
        modifiedAtRemote = 1_779_810_276_000,
        contentHash = null,
        audioFingerprint = null,
        isDeleted = false,
        firstSyncedAt = 100,
        lastSyncedAt = 200,
        lastSeenScanId = "scan-1",
    )
}

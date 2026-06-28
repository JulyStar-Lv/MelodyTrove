package com.github.tidetunes.database

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uniffi.tidetunes_core.StorageEntry
import uniffi.tidetunes_core.StorageId

class RemoteFileFingerprintTest {
    @Test
    fun prefersEtagAndFallsBackToModifiedTime() {
        val existing = remoteFile()
        assertTrue(existing.hasSameRemoteContent(entry()))
        assertFalse(existing.hasSameRemoteContent(entry(etag = "\"changed\"")))

        val withoutEtag = existing.copy(etag = null)
        assertTrue(withoutEtag.hasSameRemoteContent(entry(etag = null)))
        assertFalse(
            withoutEtag.hasSameRemoteContent(
                entry(etag = null, modifiedAt = 1_779_810_277_000),
            ),
        )
        assertFalse(withoutEtag.hasSameRemoteContent(entry(etag = null, modifiedAt = null)))
    }

    @Test
    fun revisionMatchIgnoresPathForStableRemoteIdMoves() {
        val existing = remoteFile()
        val moved = entry(path = "/Moved/track.flac")

        assertTrue(existing.hasSameRemoteRevision(moved))
        assertFalse(existing.hasSameRemoteContent(moved))
    }

    private fun remoteFile() = RemoteFileEntity(
        id = 7,
        storageId = 1,
        selectedFolderId = 2,
        remoteId = null,
        parentRemoteId = null,
        canonicalPath = "/track.flac",
        displayPath = "/track.flac",
        fileName = "track.flac",
        extension = "flac",
        mimeType = "audio/flac",
        size = 33_731_890,
        etag = "\"etag\"",
        ctag = null,
        createdAt = 1_779_810_276_000,
        modifiedAt = 1_779_810_276_000,
        contentHash = null,
        isDeleted = false,
        lastSeenScanId = "previous",
    )

    private fun entry(
        path: String = "/track.flac",
        etag: String? = "\"etag\"",
        modifiedAt: Long? = 1_779_810_276_000,
    ) = StorageEntry(
        storageId = StorageId(1),
        name = "track.flac",
        path = path,
        size = 33_731_890uL,
        isDir = false,
        remoteId = null,
        parentRemoteId = null,
        mimeType = "audio/flac",
        etag = etag,
        ctag = null,
        createdAt = 1_779_810_276_000,
        modifiedAt = modifiedAt,
    )
}

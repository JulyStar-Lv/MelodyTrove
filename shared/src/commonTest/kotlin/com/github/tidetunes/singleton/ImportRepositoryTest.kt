package com.github.tidetunes.singleton

import com.github.tidetunes.core.data.toLegacyStorageArtwork
import com.github.tidetunes.core.domain.model.SourceAccountId
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.core.data.toLegacyStorageEntry
import com.github.tidetunes.core.data.toLegacyStorageEntryLoc
import com.github.tidetunes.core.data.toSourceNodeSelection
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.feature.importing.data.ImportRepositoryImpl
import com.github.tidetunes.core.domain.model.ImportSelectionMode
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.api.SourceDirectorySelection
import com.github.tidetunes.source.api.SourceNode
import com.github.tidetunes.source.api.SourceNodeSelection
import com.github.tidetunes.source.api.SourceNodeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import uniffi.tidetunes_backend.Storage
import uniffi.tidetunes_backend.StorageId
import uniffi.tidetunes_backend.StorageEntryLoc
import uniffi.tidetunes_backend.StorageType

class ImportRepositoryTest {
    @Test
    fun entryImportUsesSourceSelectionCallback() {
        val repository = ImportRepositoryImpl()
        var selected: List<SourceNodeSelection>? = null
        val selection = importSelection()

        repository.prepare(listOf(SourceNodeType.Track)) { entries ->
            selected = entries
        }

        assertEquals(ImportSelectionMode.Entries, repository.selectionMode.value)
        assertEquals(listOf(SourceNodeType.Track), repository.allowTypes.value)

        repository.onFinish(listOf(selection))

        assertEquals(listOf(selection), selected)
    }

    @Test
    fun sourceSelectionCanBeAdaptedToLegacyStorageEntryAtBoundary() {
        val entry = importSelection().toLegacyStorageEntry()

        assertEquals(StorageId(9), entry?.storageId)
        assertEquals("Track.flac", entry?.name)
        assertEquals("/Music/Track.flac", entry?.path)
        assertEquals("file-id", entry?.remoteId)
    }

    @Test
    fun legacyCoverLocCanBeAdaptedToSourceSelectionAtBoundary() {
        val loc = StorageEntryLoc(StorageId(9), "/Music/Cover.jpg")
        val selection = loc.toSourceNodeSelection(listOf(storage(id = 9, type = StorageType.WEBDAV)))

        assertEquals(BuiltInSourceIds.WebDav, selection?.sourceId)
        assertEquals(SourceAccountId("storage:9"), selection?.accountId)
        assertEquals(SourceNodeType.Image, selection?.node?.type)
        assertEquals("Cover.jpg", selection?.node?.name)
        assertEquals(loc, selection?.toLegacyStorageEntryLoc())

        val artwork = assertIs<Artwork.SourceMedia>(selection?.toLegacyStorageArtwork())
        assertEquals(BuiltInSourceIds.WebDav, artwork.mediaId.sourceId)
        assertEquals(MediaType.Image, artwork.mediaId.mediaType)
    }

    @Test
    fun legacyCoverLocWithoutStorageStillPreservesUpdateSelection() {
        val loc = StorageEntryLoc(StorageId(9), "/Music/Cover.jpg")
        val selection = loc.toSourceNodeSelection(emptyList())

        assertEquals(SourceAccountId("storage:9"), selection?.accountId)
        assertEquals(loc, selection?.toLegacyStorageEntryLoc())
        assertNull(selection?.toLegacyStorageArtwork())
    }

    @Test
    fun currentDirectoryImportUsesDirectoryModeAndCallback() {
        val repository = ImportRepositoryImpl()
        var selected: SourceDirectorySelection? = null
        val selection = SourceDirectorySelection(
            sourceId = SourceId("webdav"),
            accountId = SourceAccountId("storage:9"),
            path = "/Music",
            remoteId = "folder-id",
        )

        repository.prepareCurrentDirectory { directorySelection ->
            selected = directorySelection
        }

        assertEquals(ImportSelectionMode.CurrentDirectory, repository.selectionMode.value)
        assertEquals(emptyList(), repository.allowTypes.value)

        repository.onFinishCurrentDirectory(selection)

        assertEquals(selection, selected)
    }

    @Test
    fun entryImportResetsDirectoryMode() {
        val repository = ImportRepositoryImpl()

        repository.prepareCurrentDirectory { }
        repository.prepare(listOf(SourceNodeType.Track)) {}

        assertEquals(ImportSelectionMode.Entries, repository.selectionMode.value)
        assertEquals(listOf(SourceNodeType.Track), repository.allowTypes.value)
    }

    private fun importSelection(): SourceNodeSelection {
        return SourceNodeSelection(
            sourceId = SourceId("webdav"),
            accountId = SourceAccountId("storage:9"),
            node = SourceNode(
                accountId = SourceAccountId("storage:9"),
                nodeId = "file-id",
                remoteId = "file-id",
                parentNodeId = "folder-id",
                name = "Track.flac",
                path = "/Music/Track.flac",
                type = SourceNodeType.Track,
                sizeBytes = 42u,
                mimeType = "audio/flac",
                etag = "etag",
                ctag = "ctag",
                createdAtEpochMs = 1,
                modifiedAtEpochMs = 2,
            ),
        )
    }

    private fun storage(id: Long, type: StorageType): Storage {
        return Storage(
            id = StorageId(id),
            addr = "",
            alias = "Storage $id",
            username = "",
            password = "",
            isAnonymous = true,
            typ = type,
            musicCount = 0u,
        )
    }
}

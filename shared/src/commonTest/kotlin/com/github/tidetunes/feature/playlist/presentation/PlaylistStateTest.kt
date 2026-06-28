package com.github.tidetunes.feature.playlist.presentation

import com.github.tidetunes.core.domain.model.DomainPlaylistTrack
import com.github.tidetunes.source.api.BuiltInSourceIds
import com.github.tidetunes.source.storage.LegacyStorageLookup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import com.github.tidetunes.core.domain.model.PlaylistSummary
import uniffi.tidetunes_core.Storage
import uniffi.tidetunes_core.StorageId
import uniffi.tidetunes_core.StorageType
import kotlin.time.Duration.Companion.seconds

class PlaylistStateTest {
    @Test
    fun mapsPlaylistHeaderWithoutReplacingTrackState() {
        val track = PlaylistTrackItem(
            id = 7,
            title = "Song",
            durationMs = 65_000,
            sortOrder = 1,
            mediaId = null,
        )
        val current = PlaylistState(
            tracks = persistentListOf(track),
            isRemoveDialogOpen = true,
        )

        val state = playlist(
            id = 42,
            title = "Road Trip",
            durationSeconds = 65,
        ).toPlaylistHeaderState(current)

        assertEquals(42, state.playlistId)
        assertEquals("Road Trip", state.title)
        assertEquals("00:01:05", state.durationLabel)
        assertEquals(true, state.isRemoveDialogOpen)
        assertSame(current.tracks, state.tracks)
    }

    @Test
    fun mapsPlaylistTrackRowToDownloadablePresentationItem() = runBlocking {
        val item = DomainPlaylistTrack(
            trackId = 9,
            sortOrder = 2,
            title = "Downloadable",
            durationMs = 123_000,
            sourceStorageId = 5,
            sourcePath = "/Music/Downloadable.flac",
        ).toPlaylistTrackItem(
            LegacyStorageLookup { storageId ->
                storage(id = storageId.value, typ = StorageType.WEBDAV)
            }
        )

        assertEquals(9, item.id)
        assertEquals("Downloadable", item.title)
        assertEquals("00:02:03", item.durationLabel)
        assertEquals(BuiltInSourceIds.WebDav, item.mediaId?.sourceId)
        assertEquals(
            "legacy-storage-track:storage%3A5:%2FMusic%2FDownloadable.flac",
            item.mediaId?.remoteId,
        )
    }

    private fun playlist(
        id: Long,
        title: String,
        durationSeconds: Long,
    ) = PlaylistSummary(
        id = id,
        title = title,
        musicCount = 1,
        durationMs = durationSeconds * 1000,
        coverArtwork = null,
    )

    private fun storage(
        id: Long,
        typ: StorageType,
    ) = Storage(
        id = StorageId(id),
        addr = "https://example.com",
        alias = "NAS",
        username = "alice",
        password = "",
        isAnonymous = true,
        typ = typ,
        musicCount = 0u,
    )
}

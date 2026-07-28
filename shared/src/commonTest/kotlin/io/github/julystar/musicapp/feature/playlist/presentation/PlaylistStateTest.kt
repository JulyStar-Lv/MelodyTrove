package io.github.julystar.musicapp.feature.playlist.presentation

import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.source.api.BuiltInSourceIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.collections.immutable.persistentListOf
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
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
    fun mapsPlaylistTrackRowToDownloadablePresentationItem() {
        val mediaId = MediaId(
            sourceId = BuiltInSourceIds.WebDav,
            mediaType = MediaType.Track,
            remoteId = "legacy-storage-track:storage%3A5:%2FMusic%2FDownloadable.flac",
        )
        val item = DomainPlaylistTrack(
            trackId = 9,
            sortOrder = 2,
            title = "Downloadable",
            durationMs = 123_000,
            sourceStorageId = 5,
            sourcePath = "/Music/Downloadable.flac",
            mediaId = mediaId,
        ).toPlaylistTrackItem()

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

}

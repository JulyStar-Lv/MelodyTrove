package com.github.tidetunes.feature.artist.presentation

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtistStateTest {

    @Test
    fun `default state is loading with empty collections`() {
        val state = ArtistState()

        assertTrue(state.isLoading)
        assertEquals("", state.name)
        assertEquals(persistentListOf(), state.albums)
        assertEquals(persistentListOf(), state.tracks)
        assertNull(state.error)
    }

    @Test
    fun `loaded state carries artist data`() {
        val albums = persistentListOf(ArtistAlbumItem(id = 1, name = "Album", year = 2024, artwork = null))
        val tracks = persistentListOf(ArtistTrackItem(id = 1, title = "Song", albumName = "Album", trackNumber = 1, discNumber = 1, durationMs = 200_000, mediaId = null, canDownload = false, albumId = 1L))
        val state = ArtistState(isLoading = false, name = "Artist", albums = albums, tracks = tracks)

        assertFalse(state.isLoading)
        assertEquals("Artist", state.name)
        assertEquals(1, state.albums.size)
        assertEquals(1, state.tracks.size)
    }

    @Test
    fun `error state preserves name`() {
        val state = ArtistState(isLoading = false, name = "Artist", error = "Failed")

        assertEquals("Artist", state.name)
        assertEquals("Failed", state.error)
    }

    @Test
    fun `play track action carries track id`() {
        val action = ArtistAction.PlayTrack(7)
        assertEquals(7, action.trackId)
    }

    @Test
    fun `navigate to album action carries album id`() {
        val action = ArtistAction.NavigateToAlbum(3)
        assertEquals(3, action.albumId)
    }

    @Test
    fun `navigate back, retry, play all are singletons`() {
        assertEquals(ArtistAction.NavigateBack, ArtistAction.NavigateBack)
        assertEquals(ArtistAction.Retry, ArtistAction.Retry)
        assertEquals(ArtistAction.PlayAll, ArtistAction.PlayAll)
    }
}

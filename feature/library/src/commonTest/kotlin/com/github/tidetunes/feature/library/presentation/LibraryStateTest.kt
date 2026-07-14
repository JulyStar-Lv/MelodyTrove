package com.github.tidetunes.feature.library.presentation

import com.github.tidetunes.core.domain.model.LibraryTrackItem
import com.github.tidetunes.core.domain.model.LibraryAlbumItem
import com.github.tidetunes.core.domain.model.LibraryArtistItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LibraryStateTest {

    @Test
    fun `default state is empty`() {
        val state = LibraryState()

        assertTrue(state.tracks.isEmpty())
    }

    @Test
    fun `state with tracks preserves data`() {
        val tracks = persistentListOf(
            LibraryTrackItem(id = 1, title = "Track 1", artist = "Artist", durationMs = 240_000, mediaId = null),
            LibraryTrackItem(id = 2, title = "Track 2", artist = null, durationMs = null, mediaId = null),
        )
        val state = LibraryState(tracks = tracks)

        assertEquals(2, state.tracks.size)
        assertEquals("Track 1", state.tracks[0].title)
        assertEquals(240_000, state.tracks[0].durationMs)
    }

    @Test
    fun `state preserves complete album and artist collections`() {
        val albums = (1L..12L).map { LibraryAlbumItem(it, "Album $it", 2000 + it.toInt()) }.toPersistentList()
        val artists = (1L..13L).map { LibraryArtistItem(it, "Artist $it") }.toPersistentList()

        val state = LibraryState(albums = albums, artists = artists)

        assertEquals(12, state.albums.size)
        assertEquals(13, state.artists.size)
        assertEquals("Album 12", state.albums.last().name)
        assertEquals("Artist 13", state.artists.last().name)
    }

    @Test
    fun `library row keys stay unique when track ids repeat`() {
        val first = LibraryTrackItem(id = 16830502, title = "Track", artist = null, durationMs = null, mediaId = null)
        val second = first.copy(title = "Track duplicate")

        assertNotEquals(first.lazyListKey(index = 0), second.lazyListKey(index = 1))
    }

    @Test
    fun `refresh action is singleton`() {
        assertEquals(LibraryAction.Refresh, LibraryAction.Refresh)
    }

    @Test
    fun `play track action carries track id`() {
        val action = LibraryAction.PlayTrack(trackId = 7)

        assertEquals(7, action.trackId)
    }

    @Test
    fun `download track action carries track`() {
        val track = LibraryTrackItem(id = 7, title = "DL", artist = "Me", durationMs = 1000, mediaId = null)
        val action = LibraryAction.DownloadTrack(track)

        assertEquals("DL", action.track.title)
        assertEquals(7, action.track.id)
    }

    @Test
    fun `show message event carries text`() {
        val event = LibraryEvent.ShowMessage("hello")
        assertEquals("hello", event.message)
    }
}

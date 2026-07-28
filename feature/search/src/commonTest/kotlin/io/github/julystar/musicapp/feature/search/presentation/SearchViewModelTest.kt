package io.github.julystar.musicapp.feature.search.presentation

import io.github.julystar.musicapp.core.domain.model.MediaId
import io.github.julystar.musicapp.core.domain.model.MediaType
import io.github.julystar.musicapp.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import io.github.julystar.musicapp.core.domain.model.SourceId
import io.github.julystar.musicapp.feature.search.domain.SearchAlbumItem
import io.github.julystar.musicapp.feature.search.domain.SearchArtistItem
import io.github.julystar.musicapp.feature.search.domain.SearchTrackItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SearchViewModelTest {

    @Test
    fun `initial state has default values`() {
        val state = SearchState()

        assertEquals("", state.query)
        assertEquals(SearchLoadState.Idle, state.loadState)
        assertEquals(persistentListOf(), state.tracks)
        assertEquals(persistentListOf(), state.history)
        assertEquals(persistentListOf(), state.suggestions)
        assertEquals(0, state.failedSourceCount)
        assertFalse(state.isSearching)
    }

    @Test
    fun `searching state reports isSearching true`() {
        val state = SearchState(loadState = SearchLoadState.Searching)

        assertTrue(state.isSearching)
    }

    @Test
    fun `QueryChanged action carries query value`() {
        val action = SearchAction.QueryChanged("test query")

        assertEquals("test query", action.query)
    }

    @Test
    fun `SubmitSearch is a singleton action`() {
        assertEquals(SearchAction.SubmitSearch, SearchAction.SubmitSearch)
    }

    @Test
    fun `ClearQuery is a singleton action`() {
        assertEquals(SearchAction.ClearQuery, SearchAction.ClearQuery)
    }

    @Test
    fun `OpenTrack action carries track`() {
        val track = SearchTrackItem(
            title = "Song",
            artist = "Artist",
            durationMs = null,
            sourceLabel = "Library",
        )
        val action = SearchAction.OpenTrack(track)

        assertEquals("Song", action.track.title)
    }

    @Test
    fun `open track event preserves playback metadata`() {
        val track = SearchTrackItem(
            id = 42,
            title = "Song",
            artist = "Artist",
            durationMs = 240_000L,
            sourceLabel = "Library",
        )
        val event = SearchEvent.OpenTrack(track)

        assertEquals("Song", event.track.title)
        assertEquals("Artist", event.track.artist)
        assertEquals(42, event.track.id)
    }

    @Test
    fun `library search result keeps its legacy playback context`() {
        val track = SearchTrackItem(
            id = 42,
            title = "Song",
            artist = "Artist",
            durationMs = 240_000L,
        )

        val playable = track.toPlayableItem()

        assertTrue(track.isPlayableFromSearch())
        assertEquals(42, playable.libraryTrackId)
        assertEquals(LIBRARY_PLAYBACK_PLAYLIST_ID, playable.libraryPlaylistId)
    }

    @Test
    fun `search item without a media or library id is not playable`() {
        val track = SearchTrackItem(
            title = "Unmapped result",
            artist = null,
            durationMs = null,
        )

        assertFalse(track.isPlayableFromSearch())
    }

    @Test
    fun `source only result is downloadable but not sent to legacy playback`() {
        val track = SearchTrackItem(
            title = "Remote song",
            artist = "Source artist",
            durationMs = 180_000L,
            mediaId = MediaId(
                sourceId = SourceId("webdav"),
                mediaType = MediaType.Track,
                remoteId = "music/remote-song.flac",
            ),
            sourceLabel = "WebDAV",
        )

        assertFalse(track.isPlayableFromSearch())
        assertEquals("webdav", track.mediaId?.sourceId?.value)
    }

    @Test
    fun `DownloadTrack action carries track`() {
        val track = SearchTrackItem(
            title = "Song",
            artist = "Artist",
            durationMs = 240000L,
            mediaId = MediaId(
                sourceId = SourceId("webdav"),
                mediaType = MediaType.Track,
                remoteId = "music/song.flac",
            ),
            sourceLabel = "WebDAV",
        )
        val action = SearchAction.DownloadTrack(track)

        assertEquals("Song", action.track.title)
        assertEquals("WebDAV", action.track.sourceLabel)
        assertEquals("webdav", action.track.mediaId?.sourceId?.value)
    }

    @Test
    fun `all load states are distinct`() {
        val states = SearchLoadState.entries.toSet()
        assertEquals(6, states.size)
    }

    @Test
    fun `row keys stay unique when search results repeat`() {
        val track = SearchTrackItem(
            id = 42,
            title = "Song",
            artist = "Artist",
            durationMs = null,
        )

        assertNotEquals(track.lazyListKey(0), track.copy(title = "Song duplicate").lazyListKey(1))

    }
    @Test
    fun `initial state has empty albums and artists`() {
        val state = SearchState()

        assertEquals(persistentListOf(), state.albums)
        assertEquals(persistentListOf(), state.artists)
    }

    @Test
    fun `OpenAlbum action carries album`() {
        val album = SearchAlbumItem(
            id = 1,
            name = "Test Album",
            artist = "Test Artist",
        )
        val action = SearchAction.OpenAlbum(album)

        assertEquals(1, action.album.id)
        assertEquals("Test Album", action.album.name)
        assertEquals("Test Artist", action.album.artist)
    }

    @Test
    fun `OpenArtist action carries artist`() {
        val artist = SearchArtistItem(
            id = 2,
            name = "Test Artist",
        )
        val action = SearchAction.OpenArtist(artist)

        assertEquals(2, action.artist.id)
        assertEquals("Test Artist", action.artist.name)
    }

    @Test
    fun `NavigateToAlbum event carries album id`() {
        val event = SearchEvent.NavigateToAlbum(42)

        assertEquals(42, event.albumId)
    }

    @Test
    fun `NavigateToArtist event carries artist id`() {
        val event = SearchEvent.NavigateToArtist(99)

        assertEquals(99, event.artistId)
    }

}

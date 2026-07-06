package com.github.tidetunes.feature.search.presentation

import com.github.tidetunes.core.domain.model.MediaId
import com.github.tidetunes.core.domain.model.MediaType
import com.github.tidetunes.core.domain.model.SourceId
import com.github.tidetunes.feature.search.domain.SearchTrackItem
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
}

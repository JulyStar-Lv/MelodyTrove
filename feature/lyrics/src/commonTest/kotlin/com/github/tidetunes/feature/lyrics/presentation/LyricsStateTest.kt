package com.github.tidetunes.feature.lyrics.presentation

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LyricsStateTest {

    @Test
    fun `default state is loading with empty lines`() {
        val state = LyricsState()

        assertTrue(state.isLoading)
        assertEquals("", state.trackTitle)
        assertNull(state.trackArtist)
        assertEquals(persistentListOf(), state.lines)
        assertNull(state.error)
    }

    @Test
    fun `loaded state carries lyric data`() {
        val lines = persistentListOf("Line 1", "Line 2")
        val state = LyricsState(
            isLoading = false,
            trackTitle = "My Song",
            trackArtist = "Artist",
            lines = lines,
            format = "lrc",
            synchronized = true,
        )

        assertFalse(state.isLoading)
        assertEquals("My Song", state.trackTitle)
        assertEquals("Artist", state.trackArtist)
        assertEquals(2, state.lines.size)
        assertEquals("lrc", state.format)
        assertTrue(state.synchronized)
    }

    @Test
    fun `error state carries message`() {
        val state = LyricsState(isLoading = false, error = "Failed to load lyrics")

        assertFalse(state.isLoading)
        assertEquals("Failed to load lyrics", state.error)
    }

    @Test
    fun `navigate back and retry actions are singletons`() {
        assertEquals(LyricsAction.NavigateBack, LyricsAction.NavigateBack)
        assertEquals(LyricsAction.Retry, LyricsAction.Retry)
    }

    @Test
    fun `error state preserves previous title and artist`() {
        val state = LyricsState(
            isLoading = false,
            trackTitle = "Lost Song",
            trackArtist = "Ghost",
            error = "Not found",
        )

        assertEquals("Lost Song", state.trackTitle)
        assertEquals("Ghost", state.trackArtist)
        assertEquals("Not found", state.error)
    }
}

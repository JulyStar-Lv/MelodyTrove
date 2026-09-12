package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CarPlaybackRequestTest {
    private val tracks = listOf(
        LibraryTrackItem(11, "First", "Artist A", 101_000, null),
        LibraryTrackItem(22, "Second", "Artist B", 202_000, null),
        LibraryTrackItem(33, "Third", null, null, null),
    )

    @Test
    fun selectionKeepsCompleteQueueOrderAndSelectedIndex() {
        val request = tracks.toCarPlaybackRequest(startIndex = 1, playlistId = 99)

        assertEquals(listOf(11L, 22L, 33L), request.items.map { it.libraryTrackId })
        assertEquals(listOf(99L, 99L, 99L), request.items.map { it.libraryPlaylistId })
        assertEquals(1, request.startIndex)
        assertEquals("Second", request.items[request.startIndex].title)
    }

    @Test
    fun selectionRejectsAnIndexOutsideTheQueue() {
        assertFailsWith<IllegalArgumentException> { tracks.toCarPlaybackRequest(3) }
    }
}

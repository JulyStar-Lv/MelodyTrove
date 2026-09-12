package io.github.julystar.musicapp.car.presentation.screen

import io.github.julystar.musicapp.core.domain.search.SearchTrackItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CarSearchPlaybackRequestTest {
    @Test
    fun selectedResultUsesItsIndexInFilteredPlayableQueue() {
        val results = listOf(
            result(id = 1, title = "First"),
            result(id = null, title = "Unavailable"),
            result(id = 3, title = "Third"),
        )

        val request = results.toSearchPlaybackRequest(selectedIndex = 2)!!

        assertEquals(listOf(1L, 3L), request.items.map { it.libraryTrackId })
        assertEquals(1, request.startIndex)
    }

    @Test
    fun duplicatePlayableValuesStillSelectClickedOccurrence() {
        val duplicate = result(id = 8, title = "Same")
        val request = listOf(duplicate, duplicate).toSearchPlaybackRequest(selectedIndex = 1)!!

        assertEquals(2, request.items.size)
        assertEquals(1, request.startIndex)
    }

    @Test
    fun unavailableOrOutOfBoundsSelectionDoesNotStartPlayback() {
        val results = listOf(result(id = null, title = "Unavailable"))

        assertNull(results.toSearchPlaybackRequest(0))
        assertNull(results.toSearchPlaybackRequest(1))
    }

    private fun result(id: Long?, title: String) = SearchTrackItem(
        id = id,
        mediaId = null,
        title = title,
        artist = "Artist",
        durationMs = 90_000,
        sourceLabel = "Library",
    )
}

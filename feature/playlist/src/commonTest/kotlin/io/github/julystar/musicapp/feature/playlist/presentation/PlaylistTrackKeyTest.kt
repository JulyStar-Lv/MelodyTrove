package io.github.julystar.musicapp.feature.playlist.presentation

import kotlin.test.Test
import kotlin.test.assertNotEquals

class PlaylistTrackKeyTest {

    @Test
    fun `row keys stay unique when playlist contains same track twice`() {
        val first = PlaylistTrackItem(
            id = 42,
            title = "Song",
            durationMs = null,
            sortOrder = 1,
            mediaId = null,
        )
        val second = first.copy(sortOrder = 2)

        assertNotEquals(first.lazyListKey(0), second.lazyListKey(1))
    }
}

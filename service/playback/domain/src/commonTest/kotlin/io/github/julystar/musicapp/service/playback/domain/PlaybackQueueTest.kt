package io.github.julystar.musicapp.service.playback.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackQueueTest {
    @Test
    fun movingItemKeepsCurrentItemStable() {
        val queue = PlaybackQueue(
            items = listOf(item(1), item(2), item(3)),
            currentIndex = 1,
        )

        val moved = queue.moveItem(from = 0, to = 2)

        assertEquals(listOf(2L, 3L, 1L), moved.items.map { it.libraryTrackId })
        assertEquals(0, moved.currentIndex)
        assertEquals(2L, moved.currentItem?.libraryTrackId)
    }

    @Test
    fun movingTheCurrentItemKeepsItsIdentityAtTheDestination() {
        val queue = PlaybackQueue(
            items = listOf(item(1), item(2), item(3)),
            currentIndex = 1,
        )

        val moved = queue.moveItem(from = 1, to = 2)

        assertEquals(listOf(1L, 3L, 2L), moved.items.map { it.libraryTrackId })
        assertEquals(2, moved.currentIndex)
        assertEquals(2L, moved.currentItem?.libraryTrackId)
    }

    @Test
    fun movingAnItemAfterCurrentDoesNotChangeCurrentIndex() {
        val queue = PlaybackQueue(
            items = listOf(item(1), item(2), item(3), item(4)),
            currentIndex = 1,
        )

        val moved = queue.moveItem(from = 3, to = 2)

        assertEquals(1, moved.currentIndex)
        assertEquals(2L, moved.currentItem?.libraryTrackId)
    }

    @Test
    fun removingItemBeforeCurrentShiftsCurrentIndex() {
        val queue = PlaybackQueue(
            items = listOf(item(1), item(2), item(3)),
            currentIndex = 2,
        )

        val removed = queue.removeItem(index = 0)

        assertEquals(listOf(2L, 3L), removed.items.map { it.libraryTrackId })
        assertEquals(1, removed.currentIndex)
        assertEquals(3L, removed.currentItem?.libraryTrackId)
    }

    @Test
    fun removingCurrentItemSelectsNextAvailableItem() {
        val queue = PlaybackQueue(
            items = listOf(item(1), item(2), item(3)),
            currentIndex = 1,
        )

        val removed = queue.removeItem(index = 1)

        assertEquals(listOf(1L, 3L), removed.items.map { it.libraryTrackId })
        assertEquals(1, removed.currentIndex)
        assertEquals(3L, removed.currentItem?.libraryTrackId)
    }

    private fun item(id: Long): PlayableItem {
        return PlayableItem(
            title = "Track $id",
            libraryTrackId = id,
        )
    }
}

package io.github.julystar.musicapp.feature.queue.presentation

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class QueueStateTest {

    @Test
    fun `default state is empty`() {
        val state = QueueState()

        assertEquals(persistentListOf(), state.items)
        assertEquals(-1, state.currentIndex)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `populated state preserves item data`() {
        val items = persistentListOf(
            QueueItemUi(index = 0, title = "Track 1", artist = "Artist A", isCurrent = true),
            QueueItemUi(index = 1, title = "Track 2", artist = null, isCurrent = false),
        )
        val state = QueueState(items = items, currentIndex = 0, isPlaying = true)

        assertEquals(2, state.items.size)
        assertEquals(0, state.currentIndex)
        assertTrue(state.isPlaying)
        assertEquals("Track 1", state.items[0].title)
        assertTrue(state.items[0].isCurrent)
        assertFalse(state.items[1].isCurrent)
    }

    @Test
    fun `play item action carries index`() {
        val action = QueueAction.PlayItem(3)
        assertEquals(3, action.index)
    }

    @Test
    fun `row keys stay unique when queue indices repeat`() {
        val item = QueueItemUi(index = 3, title = "T", artist = null, isCurrent = false)

        assertNotEquals(item.lazyListKey(0), item.copy(title = "T2").lazyListKey(1))
    }

    @Test
    fun `clear queue is a singleton action`() {
        assertEquals(QueueAction.ClearQueue, QueueAction.ClearQueue)
    }
}

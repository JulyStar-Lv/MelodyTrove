package io.github.julystar.musicapp.service.playback.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import uniffi.app_backend.MusicAbstract
import uniffi.app_backend.MusicId
import uniffi.app_backend.MusicMeta
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalUnsignedTypes::class)
class QueueOrderKeyManagerTest {
    @Test
    fun movesMiddleItemToEndAndOnlyChangesItsOrderKey() {
        val items = items(100u, 200u, 300u)
        val moved = QueueOrderKeyManager(TestOrderKeyApi()).move(items, fromIndex = 1, toIndex = 2)

        assertEquals(listOf(1L, 3L, 2L), moved.map { it.meta.id.value })
        assertEquals(listOf(100u), moved[0].meta.order)
        assertEquals(listOf(300u), moved[1].meta.order)
        assertEquals(listOf(301u), moved[2].meta.order)
        assertEquals("Track 2", moved[2].meta.title)
        assertEquals(120_000.milliseconds, moved[2].meta.duration)
    }

    @Test
    fun movesMiddleItemToStartWithSingleNewKey() {
        val items = items(100u, 200u, 300u)
        val moved = QueueOrderKeyManager(TestOrderKeyApi()).move(items, fromIndex = 1, toIndex = 0)

        assertEquals(listOf(2L, 1L, 3L), moved.map { it.meta.id.value })
        assertEquals(listOf(99u), moved[0].meta.order)
        assertEquals(listOf(100u), moved[1].meta.order)
        assertEquals(listOf(300u), moved[2].meta.order)
    }

    @Test
    fun invalidOrdersAreRebalancedBeforeMove() {
        val api = TestOrderKeyApi()
        val moved = QueueOrderKeyManager(api).move(items(100u, 100u, 300u), fromIndex = 2, toIndex = 0)

        assertEquals(1, api.rebalanceCalls)
        assertEquals(listOf(3L, 1L, 2L), moved.map { it.meta.id.value })
        assertTrue(api.isStrictlyIncreasing(moved.map { it.meta.order }))
    }

    @Test
    fun ignoresNoOpAndOutOfRangeMoves() {
        val items = items(100u, 200u)
        val manager = QueueOrderKeyManager(TestOrderKeyApi())

        assertSame(items, manager.move(items, fromIndex = 0, toIndex = 0))
        assertSame(items, manager.move(items, fromIndex = -1, toIndex = 1))
        assertSame(items, manager.move(items, fromIndex = 0, toIndex = 2))
    }

    private fun items(vararg keys: UInt): List<MusicAbstract> = keys.mapIndexed { index, key ->
        MusicAbstract(
            meta = MusicMeta(
                id = MusicId((index + 1).toLong()),
                title = "Track ${index + 1}",
                duration = 120_000.milliseconds,
                order = listOf(key),
            ),
            cover = null,
        )
    }
}

private class TestOrderKeyApi : QueueOrderKeyApi {
    var rebalanceCalls = 0

    override fun default(): List<UInt> = listOf(500u)

    override fun less(value: List<UInt>): List<UInt> = listOf(value.single() - 1u)

    override fun greater(value: List<UInt>): List<UInt> = listOf(value.single() + 1u)

    override fun between(left: List<UInt>, right: List<UInt>): List<UInt> {
        val next = (left.single() + right.single()) / 2u
        check(next > left.single() && next < right.single())
        return listOf(next)
    }

    override fun isStrictlyIncreasing(values: List<List<UInt>>): Boolean =
        values.zipWithNext().all { (left, right) -> left.single() < right.single() }

    override fun needsRebalance(values: List<List<UInt>>): Boolean =
        values.any { it.size != 1 } || !isStrictlyIncreasing(values)

    override fun rebalance(count: Int): List<List<UInt>> {
        rebalanceCalls++
        return List(count) { index -> listOf((index + 1).toUInt() * 100u) }
    }
}

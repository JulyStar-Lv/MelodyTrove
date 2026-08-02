package io.github.julystar.musicapp.service.playback.data

import uniffi.app_backend.OrderKeyBatch
import uniffi.app_backend.OrderKeyValue
import uniffi.app_backend.backendError
import uniffi.app_backend.orderKeyBetween
import uniffi.app_backend.orderKeyDefault
import uniffi.app_backend.orderKeyGreater
import uniffi.app_backend.orderKeyIsStrictlyIncreasing
import uniffi.app_backend.orderKeyLess
import uniffi.app_backend.orderKeyNeedsRebalance
import uniffi.app_backend.orderKeyRebalance
import uniffi.app_backend.MusicAbstract

/**
 * Keeps the session-only playback queue in the same order as its Rust-generated
 * [MusicAbstract.meta] order keys. Kotlin selects neighbours; Rust owns all key
 * arithmetic, validation, and rebalancing.
 */
internal class QueueOrderKeyManager(
    private val orderKeys: QueueOrderKeyApi = RustQueueOrderKeyApi,
) {
    fun move(
        items: List<MusicAbstract>,
        fromIndex: Int,
        toIndex: Int,
    ): List<MusicAbstract> {
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
            return items
        }

        return try {
            moveOnce(normalize(items), fromIndex, toIndex)
        } catch (firstFailure: Exception) {
            try {
                moveOnce(rebalance(items), fromIndex, toIndex)
            } catch (retryFailure: Exception) {
                backendError(
                    "Playback queue move abandoned after order-key recovery failed: " +
                        "${firstFailure.message}; ${retryFailure.message}",
                )
                items
            }
        }
    }

    /** Rebalances only malformed, duplicate, overlong, or out-of-order keys. */
    fun normalize(items: List<MusicAbstract>): List<MusicAbstract> {
        if (items.isEmpty()) return items
        return try {
            if (orderKeys.needsRebalance(items.orderKeys())) rebalance(items) else items
        } catch (failure: Exception) {
            backendError("Playback queue order-key normalization failed: ${failure.message}")
            items
        }
    }

    fun rebalance(items: List<MusicAbstract>): List<MusicAbstract> {
        return try {
            rebalanceOrThrow(items)
        } catch (failure: Exception) {
            backendError("Playback queue order-key rebalance failed: ${failure.message}")
            items
        }
    }

    private fun rebalanceOrThrow(items: List<MusicAbstract>): List<MusicAbstract> {
        if (items.isEmpty()) return items
        val keys = orderKeys.rebalance(items.size)
        check(keys.size == items.size) { "Rust returned ${keys.size} keys for ${items.size} queue entries" }
        check(orderKeys.isStrictlyIncreasing(keys)) { "Rust returned a non-increasing key batch" }
        return items.mapIndexed { index, item ->
            item.copy(meta = item.meta.copy(order = keys[index]))
        }
    }

    private fun moveOnce(
        items: List<MusicAbstract>,
        fromIndex: Int,
        toIndex: Int,
    ): List<MusicAbstract> {
        val reordered = items.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)

        val previous = reordered.getOrNull(toIndex - 1)?.meta?.order
        val next = reordered.getOrNull(toIndex + 1)?.meta?.order
        val newKey = when {
            previous == null && next == null -> orderKeys.default()
            previous == null -> orderKeys.less(next!!)
            next == null -> orderKeys.greater(previous)
            else -> orderKeys.between(previous, next)
        }
        reordered[toIndex] = moved.copy(meta = moved.meta.copy(order = newKey))
        check(orderKeys.isStrictlyIncreasing(reordered.orderKeys())) {
            "Moved queue item produced a non-increasing order-key sequence"
        }
        return reordered
    }
}

internal interface QueueOrderKeyApi {
    fun default(): List<UInt>
    fun less(value: List<UInt>): List<UInt>
    fun greater(value: List<UInt>): List<UInt>
    fun between(left: List<UInt>, right: List<UInt>): List<UInt>
    fun isStrictlyIncreasing(values: List<List<UInt>>): Boolean
    fun needsRebalance(values: List<List<UInt>>): Boolean
    fun rebalance(count: Int): List<List<UInt>>
}

private object RustQueueOrderKeyApi : QueueOrderKeyApi {
    override fun default(): List<UInt> = orderKeyDefault().segments

    override fun less(value: List<UInt>): List<UInt> =
        orderKeyLess(OrderKeyValue(segments = value)).segments

    override fun greater(value: List<UInt>): List<UInt> =
        orderKeyGreater(OrderKeyValue(segments = value)).segments

    override fun between(left: List<UInt>, right: List<UInt>): List<UInt> =
        orderKeyBetween(
            left = OrderKeyValue(segments = left),
            right = OrderKeyValue(segments = right),
        ).segments

    override fun isStrictlyIncreasing(values: List<List<UInt>>): Boolean =
        orderKeyIsStrictlyIncreasing(values.toOrderKeyBatch())

    override fun needsRebalance(values: List<List<UInt>>): Boolean =
        orderKeyNeedsRebalance(values.toOrderKeyBatch())

    override fun rebalance(count: Int): List<List<UInt>> =
        orderKeyRebalance(count.toULong()).keys.map(OrderKeyValue::segments)
}

private fun List<List<UInt>>.toOrderKeyBatch(): OrderKeyBatch =
    OrderKeyBatch(keys = map { value -> OrderKeyValue(segments = value) })

private fun List<MusicAbstract>.orderKeys(): List<List<UInt>> = map { item -> item.meta.order }

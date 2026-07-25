package com.github.tidetunes.feature.home.domain

import kotlinx.coroutines.flow.StateFlow

interface HomePinnedRepository {
    val pinnedItems: StateFlow<List<PinnedHomeItem>>

    suspend fun pinItem(item: PinnedHomeItem)

    suspend fun unpinItem(id: String)

    suspend fun reorder(orderedIds: List<String>)
}

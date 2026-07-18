package com.github.tidetunes.feature.queue.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class QueueState(
    val items: ImmutableList<QueueItemUi> = persistentListOf(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
)

@Immutable
data class QueueItemUi(
    val index: Int,
    val title: String,
    val artist: String?,
    val durationMs: Long?,
    val isCurrent: Boolean,
)

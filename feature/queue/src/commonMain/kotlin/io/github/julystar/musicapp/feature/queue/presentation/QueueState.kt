package io.github.julystar.musicapp.feature.queue.presentation

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class QueueState(
    val items: ImmutableList<QueueItemUi> = persistentListOf(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
)

@Immutable
data class QueueItemUi(
    val index: Int,
    val title: String,
    val artist: String?,
    val isCurrent: Boolean,
)

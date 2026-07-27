package com.github.tidetunes.feature.queue.presentation

sealed interface QueueAction {
    data class PlayItem(val index: Int) : QueueAction
    data object ClearQueue : QueueAction
}

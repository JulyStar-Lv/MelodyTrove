package com.github.tidetunes.feature.queue.presentation

sealed interface QueueAction {
    data object NavigateBack : QueueAction
    data class PlayItem(val index: Int) : QueueAction
    data class RemoveItem(val index: Int) : QueueAction
    data class MoveItem(val from: Int, val to: Int) : QueueAction
    data object ClearQueue : QueueAction
}

package io.github.julystar.musicapp.feature.queue.presentation

sealed interface QueueAction {
    data class PlayItem(val index: Int) : QueueAction
    data class ToggleFavorite(val trackId: Long) : QueueAction
    data class RemoveItem(val index: Int) : QueueAction
    data object ClearQueue : QueueAction
}

package io.github.julystar.musicapp.feature.recentlyadded.presentation

sealed interface RecentlyAddedEvent {
    data class ShowMessage(val message: String) : RecentlyAddedEvent
}

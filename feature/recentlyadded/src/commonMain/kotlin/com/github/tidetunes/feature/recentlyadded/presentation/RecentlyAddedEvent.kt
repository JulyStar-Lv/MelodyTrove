package com.github.tidetunes.feature.recentlyadded.presentation

sealed interface RecentlyAddedEvent {
    data class ShowMessage(val message: String) : RecentlyAddedEvent
}

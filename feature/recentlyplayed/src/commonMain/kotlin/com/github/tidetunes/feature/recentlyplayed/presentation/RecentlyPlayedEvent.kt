package com.github.tidetunes.feature.recentlyplayed.presentation

sealed interface RecentlyPlayedEvent {
    data class ShowMessage(val message: String) : RecentlyPlayedEvent
}

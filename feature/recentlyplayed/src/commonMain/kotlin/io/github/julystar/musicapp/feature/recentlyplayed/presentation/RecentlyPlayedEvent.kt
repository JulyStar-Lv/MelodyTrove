package io.github.julystar.musicapp.feature.recentlyplayed.presentation

sealed interface RecentlyPlayedEvent {
    data class ShowMessage(val message: String) : RecentlyPlayedEvent
}

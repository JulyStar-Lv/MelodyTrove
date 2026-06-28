package com.github.tidetunes.feature.recentlyadded.presentation

sealed interface RecentlyAddedAction {
    data object NavigateBack : RecentlyAddedAction
    data object Retry : RecentlyAddedAction
    data object PlayAll : RecentlyAddedAction
    data class PlayTrack(val trackId: Long) : RecentlyAddedAction
    data class DownloadTrack(val track: RecentlyAddedTrackItem) : RecentlyAddedAction
}

package io.github.julystar.musicapp.feature.recentlyplayed.presentation

sealed interface RecentlyPlayedAction {
    data object NavigateBack : RecentlyPlayedAction
    data object Retry : RecentlyPlayedAction
    data object PlayAll : RecentlyPlayedAction
    data class PlayTrack(val trackId: Long) : RecentlyPlayedAction
    data class DownloadTrack(val track: RecentlyPlayedTrackItem) : RecentlyPlayedAction
}

package com.github.tidetunes.feature.radio.presentation

sealed interface RadioAction {
    data object NavigateBack : RadioAction
    data object Refresh : RadioAction
    data object PlayAll : RadioAction
    data class PlayTrack(val trackId: Long) : RadioAction
    data class DownloadTrack(val track: RadioTrackItem) : RadioAction
}

package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Immutable
import com.github.tidetunes.service.playback.domain.ReplayGainMode

@Immutable
data class PlaybackSettingsState(
    val gaplessEnabled: Boolean = false,
    val gaplessAvailable: Boolean = false,
    val crossfadeDurationMs: Long = 0,
    val crossfadeAvailable: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.Off,
    val replayGainPreampDb: Float = 0f,
    val replayGainAvailable: Boolean = false,
)

sealed interface PlaybackSettingsAction {
    data class SetGaplessEnabled(val enabled: Boolean) : PlaybackSettingsAction
    data class SetCrossfadeDurationMs(val durationMs: Long) : PlaybackSettingsAction
    data class SetReplayGainMode(val mode: ReplayGainMode) : PlaybackSettingsAction
    data class SetReplayGainPreampDb(val preampDb: Float) : PlaybackSettingsAction
    data object NavigateBack : PlaybackSettingsAction
}

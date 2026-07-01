package com.github.tidetunes.feature.settings.presentation

import com.github.tidetunes.service.playback.domain.ReplayGainMode

sealed interface SettingsAction {
    data object NavigateToLog : SettingsAction
    data object NavigateToDebugMore : SettingsAction
    data class OpenGitRepo(val url: String) : SettingsAction
    data class SetGaplessEnabled(val enabled: Boolean) : SettingsAction
    data class SetCrossfadeDurationMs(val durationMs: Long) : SettingsAction
    data class SetReplayGainMode(val mode: ReplayGainMode) : SettingsAction
    data class SetReplayGainPreampDb(val preampDb: Float) : SettingsAction
}

package com.github.tidetunes.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tidetunes.service.playback.domain.AdvancedPlaybackController
import com.github.tidetunes.service.playback.domain.PlaybackEngineCapabilities
import com.github.tidetunes.service.playback.domain.PlaybackEnhancementSettings
import com.github.tidetunes.service.playback.domain.PlaybackFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlaybackSettingsVM(
    private val advancedPlaybackController: AdvancedPlaybackController?,
) : ViewModel() {

    private val _controller = advancedPlaybackController

    val state: StateFlow<PlaybackSettingsState> = combine(
        _controller?.capabilities ?: MutableStateFlow(PlaybackEngineCapabilities.None),
        _controller?.enhancementSettings ?: MutableStateFlow(PlaybackEnhancementSettings.Default),
    ) { capabilities, settings ->
        PlaybackSettingsState(
            gaplessEnabled = settings.gaplessEnabled,
            gaplessAvailable = capabilities.supports(PlaybackFeature.GaplessPlayback),
            crossfadeDurationMs = settings.crossfadeDurationMs,
            crossfadeAvailable = capabilities.supports(PlaybackFeature.Crossfade),
            replayGainMode = settings.replayGainMode,
            replayGainPreampDb = settings.replayGainPreampDb,
            replayGainAvailable = capabilities.supports(PlaybackFeature.ReplayGain),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaybackSettingsState(),
    )

    fun onAction(action: PlaybackSettingsAction) {
        when (action) {
            is PlaybackSettingsAction.SetGaplessEnabled ->
                _controller?.setGaplessEnabled(action.enabled)
            is PlaybackSettingsAction.SetCrossfadeDurationMs ->
                _controller?.setCrossfadeDurationMs(action.durationMs)
            is PlaybackSettingsAction.SetReplayGainMode ->
                _controller?.setReplayGainMode(action.mode)
            is PlaybackSettingsAction.SetReplayGainPreampDb ->
                _controller?.setReplayGainPreampDb(action.preampDb)
            PlaybackSettingsAction.NavigateBack -> Unit
        }
    }
}

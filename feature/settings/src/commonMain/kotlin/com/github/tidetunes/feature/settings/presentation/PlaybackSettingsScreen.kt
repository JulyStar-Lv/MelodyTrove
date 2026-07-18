package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Composable
import com.github.tidetunes.core.domain.model.AudioFocusMode
import com.github.tidetunes.core.domain.model.MAX_EQ_BAND_GAIN_DB
import com.github.tidetunes.core.domain.model.MAX_REPLAY_GAIN_PREAMP_TENTHS_DB
import com.github.tidetunes.core.domain.model.MIN_EQ_BAND_GAIN_DB
import com.github.tidetunes.core.domain.model.MIN_REPLAY_GAIN_PREAMP_TENTHS_DB
import com.github.tidetunes.core.domain.model.LyricTimingEditorApp
import com.github.tidetunes.core.domain.model.MetadataEditorApp
import com.github.tidetunes.core.domain.model.PlayNextMode
import com.github.tidetunes.core.domain.model.PreviousButtonBehavior
import com.github.tidetunes.core.domain.model.ReplayGainMode
import com.github.tidetunes.core.domain.model.ReverbPreset
import com.github.tidetunes.core.domain.model.ShuffleStrategy
import com.github.tidetunes.core.domain.model.StartupPlaybackMode
import org.jetbrains.compose.resources.stringResource
import tidetunes.feature.settings.generated.resources.*

@Composable
fun PlaybackSettingsSection(
    state: SettingsUiState,
    onBack: () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val settings = state.settings
    val capabilities = state.capabilities

    SettingsPageLayout(title = stringResource(Res.string.settings_playback_title), onBack = onBack) {
        if (capabilities.audioFocusSupported) {
            SettingsSection(title = stringResource(Res.string.settings_audio_focus_section)) {
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_pause),
                    summary = stringResource(Res.string.settings_audio_focus_pause_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Pause,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Pause)) },
                )
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_duck),
                    summary = stringResource(Res.string.settings_audio_focus_duck_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Duck,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Duck)) },
                )
                SettingsChoiceRow(
                    title = stringResource(Res.string.settings_audio_focus_mix),
                    summary = stringResource(Res.string.settings_audio_focus_mix_summary),
                    selected = settings.audioFocusMode == AudioFocusMode.Mix,
                    onClick = { onAction(SettingsAction.SetAudioFocusMode(AudioFocusMode.Mix)) },
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_playback_behavior_section)) {
            if (capabilities.deviceDisconnectSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_pause_disconnect),
                    summary = stringResource(Res.string.settings_pause_disconnect_summary),
                    checked = settings.pauseOnDisconnect,
                    onCheckedChange = { onAction(SettingsAction.SetPauseOnDisconnect(it)) },
                )
            }
            if (capabilities.gaplessPlaybackSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_gapless),
                    summary = stringResource(Res.string.settings_gapless_summary),
                    checked = settings.gaplessPlaybackEnabled,
                    onCheckedChange = { onAction(SettingsAction.SetGaplessPlaybackEnabled(it)) },
                )
            }
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_retry_playback),
                summary = stringResource(Res.string.settings_retry_playback_summary),
                checked = settings.retryPlaybackOnFailure,
                onCheckedChange = { onAction(SettingsAction.SetRetryPlaybackOnFailure(it)) },
            )
            if (capabilities.networkStatusSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_resume_network),
                    summary = stringResource(Res.string.settings_resume_network_summary),
                    checked = settings.resumePlaybackAfterNetworkRecovery,
                    onCheckedChange = {
                        onAction(SettingsAction.SetResumePlaybackAfterNetworkRecovery(it))
                    },
                )
            }
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_keep_screen_on),
                summary = stringResource(Res.string.settings_keep_screen_on_summary),
                checked = settings.keepScreenOnInPlayer,
                onCheckedChange = { onAction(SettingsAction.SetKeepScreenOnInPlayer(it)) },
            )
        }

        SettingsSection(title = stringResource(Res.string.settings_playback_advanced_section)) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_resume_playback_position),
                summary = stringResource(Res.string.settings_resume_playback_position_summary),
                checked = settings.playbackAdvanced.resumePlaybackPosition,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlaybackAdvancedSettings(
                            settings.playbackAdvanced.copy(resumePlaybackPosition = it)
                        )
                    )
                },
            )
            StartupPlaybackMode.entries.forEach { mode ->
                SettingsChoiceRow(
                    title = stringResource(mode.titleResource()),
                    summary = stringResource(Res.string.settings_startup_playback),
                    selected = settings.playbackAdvanced.startupPlaybackMode == mode,
                    onClick = {
                        onAction(
                            SettingsAction.SetPlaybackAdvancedSettings(
                                settings.playbackAdvanced.copy(startupPlaybackMode = mode)
                            )
                        )
                    },
                )
            }
            PreviousButtonBehavior.entries.forEach { behavior ->
                SettingsChoiceRow(
                    title = stringResource(behavior.titleResource()),
                    summary = stringResource(Res.string.settings_previous_button_behavior),
                    selected = settings.playbackAdvanced.previousButtonBehavior == behavior,
                    onClick = {
                        onAction(
                            SettingsAction.SetPlaybackAdvancedSettings(
                                settings.playbackAdvanced.copy(previousButtonBehavior = behavior)
                            )
                        )
                    },
                )
            }
            PlayNextMode.entries.forEach { mode ->
                SettingsChoiceRow(
                    title = stringResource(mode.titleResource()),
                    summary = stringResource(Res.string.settings_play_next_mode),
                    selected = settings.playbackAdvanced.playNextMode == mode,
                    onClick = {
                        onAction(
                            SettingsAction.SetPlaybackAdvancedSettings(
                                settings.playbackAdvanced.copy(playNextMode = mode)
                            )
                        )
                    },
                )
            }
            ShuffleStrategy.entries.forEach { strategy ->
                SettingsChoiceRow(
                    title = stringResource(strategy.titleResource()),
                    summary = stringResource(Res.string.settings_shuffle_strategy),
                    selected = settings.playbackAdvanced.shuffleStrategy == strategy,
                    onClick = {
                        onAction(
                            SettingsAction.SetPlaybackAdvancedSettings(
                                settings.playbackAdvanced.copy(shuffleStrategy = strategy)
                            )
                        )
                    },
                )
            }
        }

        if (capabilities.crossfadeSupported || capabilities.replayGainSupported) {
            SettingsSection(title = stringResource(Res.string.settings_playback_enhancement_section)) {
                if (capabilities.crossfadeSupported) {
                    SettingsSliderRow(
                        title = stringResource(Res.string.settings_crossfade),
                        summary = stringResource(Res.string.settings_crossfade_summary),
                        value = settings.playbackAdvanced.crossfadeDurationMs / 1_000,
                        valueRange = 0..30,
                        valueText = stringResource(
                            Res.string.settings_seconds_value,
                            settings.playbackAdvanced.crossfadeDurationMs / 1_000,
                        ),
                        onValueChange = {
                            onAction(
                                SettingsAction.SetPlaybackAdvancedSettings(
                                    settings.playbackAdvanced.copy(crossfadeDurationMs = it * 1_000)
                                )
                            )
                        },
                    )
                }
                if (capabilities.replayGainSupported) {
                    ReplayGainMode.entries.forEach { mode ->
                        SettingsChoiceRow(
                            title = stringResource(mode.titleResource()),
                            summary = stringResource(Res.string.settings_replay_gain),
                            selected = settings.playbackAdvanced.replayGainMode == mode,
                            onClick = {
                                onAction(
                                    SettingsAction.SetPlaybackAdvancedSettings(
                                        settings.playbackAdvanced.copy(replayGainMode = mode)
                                    )
                                )
                            },
                        )
                    }
                    SettingsSliderRow(
                        title = stringResource(Res.string.settings_replay_gain_preamp),
                        value = settings.playbackAdvanced.replayGainPreampTenthsDb,
                        valueRange = MIN_REPLAY_GAIN_PREAMP_TENTHS_DB..
                            MAX_REPLAY_GAIN_PREAMP_TENTHS_DB,
                        valueText = settings.playbackAdvanced.replayGainPreampTenthsDb.formatTenthsDb(),
                        onValueChange = {
                            onAction(
                                SettingsAction.SetPlaybackAdvancedSettings(
                                    settings.playbackAdvanced.copy(replayGainPreampTenthsDb = it)
                                )
                            )
                        },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(Res.string.settings_player_interaction_section)) {
            val interaction = settings.playerInteraction
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_open_player_on_play),
                checked = interaction.openPlayerOnPlay,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlayerInteractionSettings(
                            interaction.copy(openPlayerOnPlay = it)
                        )
                    )
                },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_player_cover_swipe),
                checked = interaction.coverSwipeEnabled,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlayerInteractionSettings(
                            interaction.copy(coverSwipeEnabled = it)
                        )
                    )
                },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_player_tap_progress),
                checked = interaction.tapProgressToSeekEnabled,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlayerInteractionSettings(
                            interaction.copy(tapProgressToSeekEnabled = it)
                        )
                    )
                },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_player_total_duration),
                checked = interaction.showTotalDuration,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlayerInteractionSettings(
                            interaction.copy(showTotalDuration = it)
                        )
                    )
                },
            )
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_player_song_annotation),
                checked = interaction.showSongAnnotation,
                onCheckedChange = {
                    onAction(
                        SettingsAction.SetPlayerInteractionSettings(
                            interaction.copy(showSongAnnotation = it)
                        )
                    )
                },
            )
            if (capabilities.desktopShortcutsSupported) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_desktop_shortcuts),
                    summary = stringResource(Res.string.settings_desktop_shortcuts_summary),
                    checked = interaction.desktopShortcutsEnabled,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.SetPlayerInteractionSettings(
                                interaction.copy(desktopShortcutsEnabled = it)
                            )
                        )
                    },
                )
            }
            if (capabilities.externalEditorSupported) {
                MetadataEditorApp.entries.forEach { editor ->
                    SettingsChoiceRow(
                        title = stringResource(editor.titleResource()),
                        summary = stringResource(Res.string.settings_metadata_editor),
                        selected = interaction.metadataEditor == editor,
                        onClick = {
                            onAction(
                                SettingsAction.SetPlayerInteractionSettings(
                                    interaction.copy(metadataEditor = editor)
                                )
                            )
                        },
                    )
                }
                LyricTimingEditorApp.entries.forEach { editor ->
                    SettingsChoiceRow(
                        title = stringResource(editor.titleResource()),
                        summary = stringResource(Res.string.settings_lyric_timing_editor),
                        selected = interaction.lyricTimingEditor == editor,
                        onClick = {
                            onAction(
                                SettingsAction.SetPlayerInteractionSettings(
                                    interaction.copy(lyricTimingEditor = editor)
                                )
                            )
                        },
                    )
                }
            }
        }

        if (capabilities.audioEffectsSupported) {
            AudioEffectsSettingsSection(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun AudioEffectsSettingsSection(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    val effects = state.settings.audioEffects
    SettingsSection(title = stringResource(Res.string.settings_audio_effects_section)) {
        SettingsSwitchRow(
            title = stringResource(Res.string.settings_audio_effects_enabled),
            checked = effects.enabled,
            onCheckedChange = {
                onAction(SettingsAction.SetAudioEffectSettings(effects.copy(enabled = it)))
            },
        )
        effects.eqBandGainsDb.forEachIndexed { index, gain ->
            SettingsSliderRow(
                title = stringResource(Res.string.settings_eq_band, EQ_BAND_LABELS[index]),
                value = gain,
                valueRange = MIN_EQ_BAND_GAIN_DB..MAX_EQ_BAND_GAIN_DB,
                valueText = stringResource(Res.string.settings_db_value, gain),
                enabled = effects.enabled,
                onValueChange = { updatedGain ->
                    val gains = effects.eqBandGainsDb.toMutableList().apply {
                        this[index] = updatedGain
                    }
                    onAction(
                        SettingsAction.SetAudioEffectSettings(
                            effects.copy(eqBandGainsDb = gains)
                        )
                    )
                },
            )
        }
        SettingsSliderRow(
            title = stringResource(Res.string.settings_bass),
            value = effects.bassDb,
            valueRange = MIN_EQ_BAND_GAIN_DB..MAX_EQ_BAND_GAIN_DB,
            valueText = stringResource(Res.string.settings_db_value, effects.bassDb),
            enabled = effects.enabled,
            onValueChange = {
                onAction(SettingsAction.SetAudioEffectSettings(effects.copy(bassDb = it)))
            },
        )
        SettingsSliderRow(
            title = stringResource(Res.string.settings_treble),
            value = effects.trebleDb,
            valueRange = MIN_EQ_BAND_GAIN_DB..MAX_EQ_BAND_GAIN_DB,
            valueText = stringResource(Res.string.settings_db_value, effects.trebleDb),
            enabled = effects.enabled,
            onValueChange = {
                onAction(SettingsAction.SetAudioEffectSettings(effects.copy(trebleDb = it)))
            },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.settings_compressor),
            checked = effects.compressorEnabled,
            enabled = effects.enabled,
            onCheckedChange = {
                onAction(
                    SettingsAction.SetAudioEffectSettings(effects.copy(compressorEnabled = it))
                )
            },
        )
        SettingsSliderRow(
            title = stringResource(Res.string.settings_compressor_threshold),
            value = effects.compressorThresholdDb,
            valueRange = -60..0,
            valueText = stringResource(Res.string.settings_db_value, effects.compressorThresholdDb),
            enabled = effects.enabled && effects.compressorEnabled,
            onValueChange = {
                onAction(
                    SettingsAction.SetAudioEffectSettings(effects.copy(compressorThresholdDb = it))
                )
            },
        )
        SettingsSliderRow(
            title = stringResource(Res.string.settings_compressor_ratio),
            value = effects.compressorRatio,
            valueRange = 1..20,
            valueText = "${effects.compressorRatio}:1",
            enabled = effects.enabled && effects.compressorEnabled,
            onValueChange = {
                onAction(
                    SettingsAction.SetAudioEffectSettings(effects.copy(compressorRatio = it))
                )
            },
        )
        SettingsSliderRow(
            title = stringResource(Res.string.settings_stereo_width),
            value = effects.stereoWidthPercent,
            valueRange = 0..200,
            valueText = "${effects.stereoWidthPercent}%",
            enabled = effects.enabled,
            onValueChange = {
                onAction(
                    SettingsAction.SetAudioEffectSettings(effects.copy(stereoWidthPercent = it))
                )
            },
        )
        ReverbPreset.entries.forEach { preset ->
            SettingsChoiceRow(
                title = stringResource(preset.titleResource()),
                summary = stringResource(Res.string.settings_reverb),
                selected = effects.reverbPreset == preset,
                enabled = effects.enabled,
                onClick = {
                    onAction(
                        SettingsAction.SetAudioEffectSettings(effects.copy(reverbPreset = preset))
                    )
                },
            )
        }
    }
}

private val EQ_BAND_LABELS = listOf("31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz")

private fun Int.formatTenthsDb(): String {
    val sign = if (this > 0) "+" else ""
    return "$sign${this / 10}.${kotlin.math.abs(this % 10)} dB"
}

private fun StartupPlaybackMode.titleResource() = when (this) {
    StartupPlaybackMode.Off -> Res.string.settings_startup_off
    StartupPlaybackMode.ResumeLastQueue -> Res.string.settings_startup_resume
    StartupPlaybackMode.ShuffleLibrary -> Res.string.settings_startup_shuffle
}

private fun PreviousButtonBehavior.titleResource() = when (this) {
    PreviousButtonBehavior.PreviousTrack -> Res.string.settings_previous_track
    PreviousButtonBehavior.RestartCurrentTrack -> Res.string.settings_previous_restart
}

private fun PlayNextMode.titleResource() = when (this) {
    PlayNextMode.FirstRequestedFirst -> Res.string.settings_play_next_fifo
    PlayNextMode.LastRequestedFirst -> Res.string.settings_play_next_lifo
}

private fun ShuffleStrategy.titleResource() = when (this) {
    ShuffleStrategy.QueueOrder -> Res.string.settings_shuffle_queue
    ShuffleStrategy.TrueRandom -> Res.string.settings_shuffle_true_random
}

private fun MetadataEditorApp.titleResource() = when (this) {
    MetadataEditorApp.AskEveryTime -> Res.string.settings_editor_ask_every_time
    MetadataEditorApp.Lyrico -> Res.string.settings_editor_lyrico
    MetadataEditorApp.LunaBeat -> Res.string.settings_editor_lunabeat_metadata
    MetadataEditorApp.MusicTag -> Res.string.settings_editor_music_tag
}

private fun LyricTimingEditorApp.titleResource() = when (this) {
    LyricTimingEditorApp.AskEveryTime -> Res.string.settings_editor_ask_every_time
    LyricTimingEditorApp.LunaBeat -> Res.string.settings_editor_lunabeat_lyric_timing
}

private fun ReplayGainMode.titleResource() = when (this) {
    ReplayGainMode.Off -> Res.string.settings_replay_gain_off
    ReplayGainMode.Track -> Res.string.settings_replay_gain_track
    ReplayGainMode.Album -> Res.string.settings_replay_gain_album
    ReplayGainMode.Auto -> Res.string.settings_replay_gain_auto
}

private fun ReverbPreset.titleResource() = when (this) {
    ReverbPreset.None -> Res.string.settings_reverb_none
    ReverbPreset.SmallRoom -> Res.string.settings_reverb_small_room
    ReverbPreset.MediumRoom -> Res.string.settings_reverb_medium_room
    ReverbPreset.LargeRoom -> Res.string.settings_reverb_large_room
    ReverbPreset.Hall -> Res.string.settings_reverb_hall
    ReverbPreset.Plate -> Res.string.settings_reverb_plate
}

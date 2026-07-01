package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.AppChip
import com.github.tidetunes.service.playback.domain.PlaybackEnhancementSettings
import com.github.tidetunes.service.playback.domain.ReplayGainMode

@Composable
fun PlaybackSettingsSection(
    state: PlaybackSettingsState,
    onAction: (PlaybackSettingsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("Playback")

        if (state.gaplessAvailable) {
            SettingsToggleRow(
                title = "Gapless Playback",
                subtitle = "Eliminate silence between tracks",
                checked = state.gaplessEnabled,
                onCheckedChange = { onAction(PlaybackSettingsAction.SetGaplessEnabled(it)) },
            )
        }

        if (state.crossfadeAvailable) {
            SettingsSliderRow(
                title = "Crossfade",
                subtitle = if (state.crossfadeDurationMs == 0L) "Off"
                    else "${state.crossfadeDurationMs / 1000}s",
                value = state.crossfadeDurationMs.toFloat(),
                valueRange = 0f..PlaybackEnhancementSettings.MAX_CROSSFADE_MS.toFloat(),
                onValueChange = { onAction(PlaybackSettingsAction.SetCrossfadeDurationMs(it.toLong())) },
            )
        }

        if (state.replayGainAvailable) {
            ReplayGainModeRow(
                currentMode = state.replayGainMode,
                preampDb = state.replayGainPreampDb,
                onModeChange = { onAction(PlaybackSettingsAction.SetReplayGainMode(it)) },
                onPreampChange = { onAction(PlaybackSettingsAction.SetReplayGainPreampDb(it)) },
            )
        }
    }
}

private fun formatPreampDb(db: Float): String {
    val rounded = (db * 10).toLong() / 10f
    return if (rounded == rounded.toLong().toFloat()) {
        "${rounded.toLong()}.0"
    } else {
        rounded.toString()
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = title,
        letterSpacing = 1.sp,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp)
            Text(
                text = subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp)
            Text(
                text = subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
        }
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReplayGainModeRow(
    currentMode: ReplayGainMode,
    preampDb: Float,
    onModeChange: (ReplayGainMode) -> Unit,
    onPreampChange: (Float) -> Unit,
) {
    val modes = ReplayGainMode.entries
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "ReplayGain Mode", fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            modes.forEach { mode ->
                val label = when (mode) {
                    ReplayGainMode.Off -> "Off"
                    ReplayGainMode.Track -> "Track"
                    ReplayGainMode.Album -> "Album"
                }
                AppChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    label = label,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        if (currentMode != ReplayGainMode.Off) {
            SettingsSliderRow(
                title = "Preamp",
                subtitle = "${formatPreampDb(preampDb)} dB",
                value = preampDb,
                valueRange = PlaybackEnhancementSettings.MIN_REPLAY_GAIN_PREAMP_DB
                    ..PlaybackEnhancementSettings.MAX_REPLAY_GAIN_PREAMP_DB,
                onValueChange = onPreampChange,
            )
        }
    }
}

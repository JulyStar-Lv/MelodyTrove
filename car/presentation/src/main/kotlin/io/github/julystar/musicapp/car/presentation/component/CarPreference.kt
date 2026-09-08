package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography

@Composable
fun CarPreference(
    title: String,
    summary: String,
    checked: Boolean? = null,
    enabled: Boolean = true,
    controlSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .carInteractiveSurface(
                shape = LocalCarShapes.current.navigationItem,
                selected = checked == true,
                enabled = enabled,
                defaultColor = colors.backgroundSubtle,
                onClick = onClick,
            )
            .padding(horizontal = LocalCarSpacing.current.section),
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = LocalCarTypography.current.bodyLarge.copy(
                    color = if (enabled) colors.textPrimary else colors.textDisabled,
                ),
            )
            BasicText(
                text = summary,
                style = LocalCarTypography.current.body.copy(
                    color = if (enabled) colors.textSecondary else colors.textDisabled,
                ),
            )
        }
        checked?.let {
            Spacer(Modifier.size(LocalCarSpacing.current.section))
            CarSwitch(checked = it, enabled = enabled, size = controlSize)
        }
    }
}

@Composable
private fun CarSwitch(checked: Boolean, enabled: Boolean, size: Dp) {
    val colors = LocalCarColors.current
    val trackColor = when {
        !enabled -> colors.controlTrackDisabled
        checked -> colors.accentPrimary
        else -> colors.controlTrack
    }
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .size(width = size, height = size * 0.55f)
            .clip(LocalCarShapes.current.control)
            .background(trackColor)
            .padding(size * 0.08f),
    ) {
        Box(
            Modifier
                .size(size * 0.39f)
                .clip(LocalCarShapes.current.control)
                .background(if (enabled) Color.White else colors.textDisabled),
        )
    }
}

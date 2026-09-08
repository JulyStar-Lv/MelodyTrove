package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as IconView
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTouchTargets
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography

@Composable
fun CarAppWindowHeader(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val touchTargets = LocalCarTouchTargets.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
        modifier = modifier,
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(touchTargets.exit)
                .carFocusTarget(CarFocusIds.Exit, down = CarFocusIds.Home)
                .semantics { contentDescription = "退出 Tide Player" }
                .carInteractiveSurface(LocalCarShapes.current.control, onClick = onExit),
        ) {
            IconView(
                icon = CarIcon.Close,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(touchTargets.miniControl),
            )
        }
        BasicText(
            text = "Tide Player",
            style = LocalCarTypography.current.pageTitle.copy(color = colors.textPrimary),
            modifier = Modifier.fillMaxHeight().widthIn(min = touchTargets.minimum),
        )
    }
}

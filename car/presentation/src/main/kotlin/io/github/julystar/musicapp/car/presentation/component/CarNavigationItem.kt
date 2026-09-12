package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as IconView
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CarNavigationItem(
    label: String,
    icon: CarIcon,
    selected: Boolean,
    enabled: Boolean,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val contentColor = when {
        !enabled -> colors.textDisabled
        selected -> colors.accentPrimary
        else -> colors.textPrimary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .carInteractiveSurface(
                shape = LocalCarShapes.current.navigationItem,
                selected = selected,
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Spacer(Modifier.width(20.dp))
        IconView(icon, null, contentColor, Modifier.size(iconSize))
        Spacer(Modifier.width(18.dp))
        BasicText(
            text = label,
            style = LocalCarTypography.current.navigation.copy(color = contentColor),
        )
    }
}

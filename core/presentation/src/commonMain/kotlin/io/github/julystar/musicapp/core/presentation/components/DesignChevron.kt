package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import musicapp.core.presentation.generated.resources.Res
import musicapp.core.presentation.generated.resources.icon_chevron_left
import musicapp.core.presentation.generated.resources.icon_chevron_right
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class DesignChevronDirection {
    Left,
    Right,
}

@Composable
fun DesignChevron(
    modifier: Modifier = Modifier,
    direction: DesignChevronDirection = DesignChevronDirection.Right,
    size: Dp = 14.dp,
    tint: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    contentDescription: String? = null,
) {
    val icon = when (direction) {
        DesignChevronDirection.Left -> Res.drawable.icon_chevron_left
        DesignChevronDirection.Right -> Res.drawable.icon_chevron_right
    }

    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

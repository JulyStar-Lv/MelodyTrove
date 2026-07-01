package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TideTunesIconButtonSize {
    Small,
    Medium,
    Large,
}

fun tideTunesIconButtonSizeToDp(sizeType: TideTunesIconButtonSize): Dp {
    return when (sizeType) {
        TideTunesIconButtonSize.Small -> 24.dp
        TideTunesIconButtonSize.Medium -> 36.dp
        TideTunesIconButtonSize.Large -> 64.dp
    }
}

enum class TideTunesIconButtonType {
    Default,
    Surface,
    Primary,
    Error,
    ErrorVariant,
}

data class TideTunesIconButtonColors(
    val buttonBg: Color? = null,
    val buttonDisabledBg: Color? = null,
    val iconTint: Color? = null,
)

@Composable
fun TideTunesIconButton(
    sizeType: TideTunesIconButtonSize,
    buttonType: TideTunesIconButtonType,
    painter: Painter,
    onClick: () -> Unit,
    overrideColors: TideTunesIconButtonColors? = null,
    disabled: Boolean = false,
) {
    val buttonSize = tideTunesIconButtonSizeToDp(sizeType)
    val isVariant = buttonType == TideTunesIconButtonType.Primary || buttonType == TideTunesIconButtonType.ErrorVariant
    val iconSize = run {
        when (sizeType) {
            TideTunesIconButtonSize.Small -> 10.dp
            TideTunesIconButtonSize.Medium -> 16.dp
            TideTunesIconButtonSize.Large -> 24.dp
        }
    }
    val buttonBg = run {
        if (disabled) {
            if (!isVariant) {
                Color.Transparent
            } else {
                overrideColors?.buttonDisabledBg ?: MiuixTheme.colorScheme.surfaceVariant
            }
        } else {
            overrideColors?.buttonBg
                ?: when (buttonType) {
                    TideTunesIconButtonType.Primary -> MiuixTheme.colorScheme.primary
                    TideTunesIconButtonType.Surface -> Color.Transparent
                    TideTunesIconButtonType.Default -> Color.Transparent
                    TideTunesIconButtonType.Error -> Color.Transparent
                    TideTunesIconButtonType.ErrorVariant -> MiuixTheme.colorScheme.error
                }
        }
    }
    val iconTint = run {
        if (disabled) {
            if (!isVariant) {
                MiuixTheme.colorScheme.surfaceVariant
            } else {
                MiuixTheme.colorScheme.surface
            }
        } else {
            overrideColors?.iconTint
                ?: when (buttonType) {
                    TideTunesIconButtonType.Primary -> Color.White
                    TideTunesIconButtonType.Surface -> MiuixTheme.colorScheme.surface
                    TideTunesIconButtonType.Default -> MiuixTheme.colorScheme.onSurface
                    TideTunesIconButtonType.Error -> MiuixTheme.colorScheme.error
                    TideTunesIconButtonType.ErrorVariant -> MiuixTheme.colorScheme.surface
                }
        }
    }

    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(999.dp))
            .background(buttonBg)
            .clickable(
                enabled = !disabled,
                onClick = {
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconTint,
        )
    }
}

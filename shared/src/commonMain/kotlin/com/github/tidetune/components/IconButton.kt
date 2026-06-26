package com.github.tidetune.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

enum class TideTuneIconButtonSize {
    Small,
    Medium,
    Large,
}

fun tideTuneIconButtonSizeToDp(sizeType: TideTuneIconButtonSize): Dp {
    return when (sizeType) {
        TideTuneIconButtonSize.Small -> 24.dp
        TideTuneIconButtonSize.Medium -> 36.dp
        TideTuneIconButtonSize.Large -> 64.dp
    }
}

enum class TideTuneIconButtonType {
    Default,
    Surface,
    Primary,
    Error,
    ErrorVariant,
}

data class TideTuneIconButtonColors(
    val buttonBg: Color? = null,
    val buttonDisabledBg: Color? = null,
    val iconTint: Color? = null,
)

@Composable
fun TideTuneIconButton(
    sizeType: TideTuneIconButtonSize,
    buttonType: TideTuneIconButtonType,
    painter: Painter,
    onClick: () -> Unit,
    overrideColors: TideTuneIconButtonColors? = null,
    disabled: Boolean = false,
) {
    val buttonSize = tideTuneIconButtonSizeToDp(sizeType)
    val isVariant = buttonType == TideTuneIconButtonType.Primary || buttonType == TideTuneIconButtonType.ErrorVariant
    val iconSize = run {
        when (sizeType) {
            TideTuneIconButtonSize.Small -> 10.dp
            TideTuneIconButtonSize.Medium -> 16.dp
            TideTuneIconButtonSize.Large -> 24.dp
        }
    }
    val buttonBg = run {
        if (disabled) {
            if (!isVariant) {
                Color.Transparent
            } else {
                overrideColors?.buttonDisabledBg ?: MaterialTheme.colorScheme.surfaceVariant
            }
        } else {
            overrideColors?.buttonBg
                ?: when (buttonType) {
                    TideTuneIconButtonType.Primary -> MaterialTheme.colorScheme.primary
                    TideTuneIconButtonType.Surface -> Color.Transparent
                    TideTuneIconButtonType.Default -> Color.Transparent
                    TideTuneIconButtonType.Error -> Color.Transparent
                    TideTuneIconButtonType.ErrorVariant -> MaterialTheme.colorScheme.error
                }
        }
    }
    val iconTint = run {
        if (disabled) {
            if (!isVariant) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        } else {
            overrideColors?.iconTint
                ?: when (buttonType) {
                    TideTuneIconButtonType.Primary -> Color.White
                    TideTuneIconButtonType.Surface -> MaterialTheme.colorScheme.surface
                    TideTuneIconButtonType.Default -> MaterialTheme.colorScheme.onSurface
                    TideTuneIconButtonType.Error -> MaterialTheme.colorScheme.error
                    TideTuneIconButtonType.ErrorVariant -> MaterialTheme.colorScheme.surface
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

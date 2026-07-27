package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class TideIconButtonSize {
    Small,
    Medium,
    Touch,
    Large,
}

fun tideIconButtonSizeToDp(size: TideIconButtonSize): Dp {
    return when (size) {
        TideIconButtonSize.Small -> 24.dp
        TideIconButtonSize.Medium -> 36.dp
        TideIconButtonSize.Touch -> 44.dp
        TideIconButtonSize.Large -> 64.dp
    }
}

enum class TideIconButtonVariant {
    Default,
    Surface,
    Primary,
    Error,
    ErrorFilled,
}

enum class TidePlayerControlSize {
    Mini,
    Large,
}

enum class TidePlayerControlVariant {
    Ghost,
    Secondary,
    Primary,
}

data class TideIconButtonColors(
    val buttonBg: Color? = null,
    val buttonDisabledBg: Color? = null,
    val iconTint: Color? = null,
)

enum class TideTunesIconButtonSize {
    Small,
    Medium,
    Touch,
    Large,
}

fun tideTunesIconButtonSizeToDp(sizeType: TideTunesIconButtonSize): Dp {
    return tideIconButtonSizeToDp(sizeType.toTideIconButtonSize())
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
    TideIconButton(
        size = sizeType.toTideIconButtonSize(),
        variant = buttonType.toTideIconButtonVariant(),
        painter = painter,
        onClick = onClick,
        colors = overrideColors?.toTideIconButtonColors(),
        enabled = !disabled,
    )
}

@Composable
fun TideIconButton(
    size: TideIconButtonSize,
    variant: TideIconButtonVariant,
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    colors: TideIconButtonColors? = null,
    enabled: Boolean = true,
    showClickIndication: Boolean = true,
) {
    val buttonSize = tideIconButtonSizeToDp(size)
    val touchTargetSize = maxOf(buttonSize, TideTunesTokens.adaptive.minimumTouchTarget)
    val isFilled = variant == TideIconButtonVariant.Primary || variant == TideIconButtonVariant.ErrorFilled
    val iconSize = tideIconButtonIconSizeToDp(size)
    val buttonBg = tideIconButtonBackground(
        variant = variant,
        colors = colors,
        enabled = enabled,
        isFilled = isFilled,
    )
    val iconTint = tideIconButtonIconTint(
        variant = variant,
        colors = colors,
        enabled = enabled,
        isFilled = isFilled,
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .clickable(
                interactionSource = interactionSource,
                indication = if (showClickIndication) {
                    LocalIndication.current
                } else {
                    null
                },
                enabled = enabled,
                onClick = {
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(999.dp))
                .background(buttonBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = iconTint,
            )
        }
    }
}

@Composable
fun TidePlayerControlButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: TidePlayerControlSize = TidePlayerControlSize.Mini,
    variant: TidePlayerControlVariant = TidePlayerControlVariant.Secondary,
    contentDescription: String? = null,
    showClickIndication: Boolean = true,
) {
    val isPrimary = variant == TidePlayerControlVariant.Primary
    val buttonSize = tidePlayerControlButtonSize(size, variant)
    val touchTargetSize = maxOf(buttonSize, TideTunesTokens.adaptive.minimumTouchTarget)
    val background = when (variant) {
        TidePlayerControlVariant.Ghost -> Brush.linearGradient(
            listOf(Color.Transparent, Color.Transparent),
        )
        TidePlayerControlVariant.Secondary -> Brush.linearGradient(
            listOf(
                MiuixTheme.colorScheme.secondaryContainer,
                MiuixTheme.colorScheme.secondaryContainer,
            ),
        )
        TidePlayerControlVariant.Primary -> Brush.linearGradient(
            listOf(
                TideTunesBrand.Primary,
                TideTunesBrand.Secondary,
            ),
        )
    }
    val backgroundAlpha = when {
        enabled -> 1f
        size == TidePlayerControlSize.Large -> 0.42f
        else -> 0.46f
    }
    val iconTint = when {
        !enabled && size == TidePlayerControlSize.Mini -> MiuixTheme.colorScheme.disabledOnSurface
        isPrimary -> Color.White
        !enabled -> MiuixTheme.colorScheme.disabledOnSurface
        variant == TidePlayerControlVariant.Ghost -> MiuixTheme.colorScheme.onSurface
        else -> MiuixTheme.colorScheme.onSecondaryContainer
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .clickable(
                interactionSource = interactionSource,
                indication = if (showClickIndication) {
                    LocalIndication.current
                } else {
                    null
                },
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(999.dp))
                .background(background, alpha = backgroundAlpha),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(tidePlayerControlIconSize(size, variant)),
            )
        }
    }
}

private fun tideIconButtonIconSizeToDp(size: TideIconButtonSize): Dp {
    return when (size) {
        TideIconButtonSize.Small -> 10.dp
        TideIconButtonSize.Medium -> 16.dp
        TideIconButtonSize.Touch -> 24.dp
        TideIconButtonSize.Large -> 24.dp
    }
}

private fun tidePlayerControlButtonSize(
    size: TidePlayerControlSize,
    variant: TidePlayerControlVariant,
): Dp {
    return when (size) {
        TidePlayerControlSize.Mini -> when (variant) {
            TidePlayerControlVariant.Ghost -> 40.dp
            TidePlayerControlVariant.Primary -> 34.dp
            TidePlayerControlVariant.Secondary -> 30.dp
        }
        TidePlayerControlSize.Large -> 64.dp
    }
}

private fun tidePlayerControlIconSize(
    size: TidePlayerControlSize,
    variant: TidePlayerControlVariant,
): Dp {
    return when (size) {
        TidePlayerControlSize.Mini -> when (variant) {
            TidePlayerControlVariant.Ghost -> 20.dp
            TidePlayerControlVariant.Primary -> 15.dp
            TidePlayerControlVariant.Secondary -> 12.dp
        }
        TidePlayerControlSize.Large -> 26.dp
    }
}

@Composable
private fun tideIconButtonBackground(
    variant: TideIconButtonVariant,
    colors: TideIconButtonColors?,
    enabled: Boolean,
    isFilled: Boolean,
): Color {
    return if (!enabled) {
        if (isFilled) {
            colors?.buttonDisabledBg ?: MiuixTheme.colorScheme.surfaceVariant
        } else {
            Color.Transparent
        }
    } else {
        colors?.buttonBg ?: when (variant) {
            TideIconButtonVariant.Primary -> MiuixTheme.colorScheme.primary
            TideIconButtonVariant.Surface -> MiuixTheme.colorScheme.surfaceContainerHigh
            TideIconButtonVariant.Default -> Color.Transparent
            TideIconButtonVariant.Error -> Color.Transparent
            TideIconButtonVariant.ErrorFilled -> MiuixTheme.colorScheme.error
        }
    }
}

@Composable
private fun tideIconButtonIconTint(
    variant: TideIconButtonVariant,
    colors: TideIconButtonColors?,
    enabled: Boolean,
    isFilled: Boolean,
): Color {
    return if (!enabled) {
        if (isFilled) {
            MiuixTheme.colorScheme.surface
        } else {
            MiuixTheme.colorScheme.surfaceVariant
        }
    } else {
        colors?.iconTint ?: when (variant) {
            TideIconButtonVariant.Primary -> Color.White
            TideIconButtonVariant.Surface -> MiuixTheme.colorScheme.primary
            TideIconButtonVariant.Default -> MiuixTheme.colorScheme.onSurface
            TideIconButtonVariant.Error -> MiuixTheme.colorScheme.error
            TideIconButtonVariant.ErrorFilled -> MiuixTheme.colorScheme.surface
        }
    }
}

private fun TideTunesIconButtonSize.toTideIconButtonSize(): TideIconButtonSize {
    return when (this) {
        TideTunesIconButtonSize.Small -> TideIconButtonSize.Small
        TideTunesIconButtonSize.Medium -> TideIconButtonSize.Medium
        TideTunesIconButtonSize.Touch -> TideIconButtonSize.Touch
        TideTunesIconButtonSize.Large -> TideIconButtonSize.Large
    }
}

private fun TideTunesIconButtonType.toTideIconButtonVariant(): TideIconButtonVariant {
    return when (this) {
        TideTunesIconButtonType.Default -> TideIconButtonVariant.Default
        TideTunesIconButtonType.Surface -> TideIconButtonVariant.Surface
        TideTunesIconButtonType.Primary -> TideIconButtonVariant.Primary
        TideTunesIconButtonType.Error -> TideIconButtonVariant.Error
        TideTunesIconButtonType.ErrorVariant -> TideIconButtonVariant.ErrorFilled
    }
}

private fun TideTunesIconButtonColors.toTideIconButtonColors(): TideIconButtonColors {
    return TideIconButtonColors(
        buttonBg = buttonBg,
        buttonDisabledBg = buttonDisabledBg,
        iconTint = iconTint,
    )
}

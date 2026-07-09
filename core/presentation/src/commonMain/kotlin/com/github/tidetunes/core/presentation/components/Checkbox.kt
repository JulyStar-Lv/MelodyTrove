package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.icon_yes
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.xs)
    val borderColor = when {
        !enabled -> MiuixTheme.colorScheme.disabledOnSurface.copy(alpha = 0.46f)
        checked -> MiuixTheme.colorScheme.primary
        else -> MiuixTheme.colorScheme.outline
    }
    val bgColor = when {
        !enabled && checked -> MiuixTheme.colorScheme.disabledPrimaryButton
        checked -> MiuixTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val iconTint = if (enabled) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.disabledOnPrimaryButton
    }

    Box(
        modifier = modifier
            .size(20.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                painter = painterResource(Res.drawable.icon_yes),
                tint = iconTint,
                contentDescription = null,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun TideTunesCheckbox(
    value: Boolean,
    onChange: (value: Boolean) -> Unit
) {
    TideCheckbox(
        checked = value,
        onCheckedChange = onChange,
    )
}

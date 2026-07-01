package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.icon_yes
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideTunesCheckbox(
    value: Boolean,
    onChange: (value: Boolean) -> Unit
) {
    val borderColor = if (value) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
    val bgColor = if (value) MiuixTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .size(16.dp)
            .background(bgColor)
            .clickable { onChange(!value) },
        contentAlignment = Alignment.Center
    ) {
        if (value) {
            Icon(
                painter = painterResource(Res.drawable.icon_yes),
                tint = MiuixTheme.colorScheme.surface,
                contentDescription = null,
                modifier = Modifier.width(6.dp)
            )
        }
    }
}

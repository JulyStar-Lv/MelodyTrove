package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)
    val colors = tideChipColors(selected = selected, enabled = enabled)

    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(colors.container)
            .border(1.dp, colors.border, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.content,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun tideChipColors(
    selected: Boolean,
    enabled: Boolean,
): TideChipColors {
    return when {
        !enabled -> TideChipColors(
            container = MiuixTheme.colorScheme.disabledSecondary,
            border = MiuixTheme.colorScheme.disabledSecondaryVariant,
            content = MiuixTheme.colorScheme.disabledOnSurface,
        )
        selected -> TideChipColors(
            container = MiuixTheme.colorScheme.primary,
            border = MiuixTheme.colorScheme.primary,
            content = MiuixTheme.colorScheme.onPrimary,
        )
        else -> TideChipColors(
            container = MiuixTheme.colorScheme.surfaceContainerHigh,
            border = MiuixTheme.colorScheme.outline,
            content = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Immutable
private data class TideChipColors(
    val container: Color,
    val border: Color,
    val content: Color,
)

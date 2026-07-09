package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideSettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            TideSectionHeader(
                title = title,
                variant = TideSectionHeaderVariant.Subtle,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(shapes.lg))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MiuixTheme.colorScheme.outline.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(shapes.lg),
                ),
            content = content,
        )
    }
}

@Composable
fun TidePreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .then(
                if (enabled && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.main,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (leading != null) 74.dp else 18.dp)
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.55f)),
            )
        }
    }
}

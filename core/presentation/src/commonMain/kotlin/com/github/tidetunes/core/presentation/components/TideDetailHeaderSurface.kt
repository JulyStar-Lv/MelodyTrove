package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TideDetailHeaderSurface(
    modifier: Modifier = Modifier,
    accentColor: Color = TideTunesBrand.Primary,
    accentAlpha: Float = 0.72f,
    surfaceAlpha: Float = 0.92f,
    borderAlpha: Float = 0.16f,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.xl)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = accentAlpha),
                        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = surfaceAlpha),
                    ),
                ),
            )
            .border(1.dp, accentColor.copy(alpha = borderAlpha), shape)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

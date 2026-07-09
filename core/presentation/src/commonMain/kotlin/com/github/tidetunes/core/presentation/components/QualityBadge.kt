package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesFontFamilies
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class QualityBadgeType(
    val label: String,
) {
    Flac("FLAC"),
    HiRes("Hi-Res"),
    DolbyAtmos("Dolby Atmos"),
}

@Composable
fun QualityBadge(
    type: QualityBadgeType,
    modifier: Modifier = Modifier,
) {
    val colors = qualityBadgeColors(type)
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)

    Box(
        modifier = modifier
            .heightIn(min = 24.dp)
            .clip(shape)
            .background(colors.container)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.label,
            color = colors.content,
            style = MiuixTheme.textStyles.footnote2.copy(fontFamily = TideTunesFontFamilies.Mono),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TideQualityBadge(
    type: QualityBadgeType,
    modifier: Modifier = Modifier,
) {
    QualityBadge(type = type, modifier = modifier)
}

@Composable
fun TideSourceBadge(
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = TideTunesBrand.SupportBlue,
) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.full)

    Box(
        modifier = modifier
            .heightIn(min = 24.dp)
            .clip(shape)
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.38f), shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = accentColor,
            style = MiuixTheme.textStyles.footnote2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun qualityBadgeColors(type: QualityBadgeType): QualityBadgeColors {
    val accent = when (type) {
        QualityBadgeType.Flac -> TideTunesBrand.Primary
        QualityBadgeType.HiRes -> TideTunesBrand.Secondary
        QualityBadgeType.DolbyAtmos -> TideTunesBrand.SupportGreen
    }
    return QualityBadgeColors(
        container = accent.copy(alpha = 0.14f),
        border = accent.copy(alpha = 0.42f),
        content = accent,
    )
}

@Immutable
private data class QualityBadgeColors(
    val container: Color,
    val border: Color,
    val content: Color,
)

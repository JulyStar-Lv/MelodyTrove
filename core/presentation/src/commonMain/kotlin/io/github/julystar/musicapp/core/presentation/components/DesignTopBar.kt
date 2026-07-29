package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DesignTopBar(
    title: String,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    titleStyle: TextStyle = MiuixTheme.textStyles.title3,
    centerTitle: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            navigationIcon?.invoke()
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = if (centerTitle) Alignment.Center else Alignment.CenterStart,
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = if (centerTitle) Modifier.width(48.dp) else Modifier,
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke()
        }
    }
}

@Composable
fun DesignTopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showBackground: Boolean = true,
) {
    val backgroundModifier = if (showBackground) {
        Modifier.background(MiuixTheme.colorScheme.surfaceVariant)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(backgroundModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        DesignChevron(
            direction = DesignChevronDirection.Left,
            size = 20.dp,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            contentDescription = contentDescription,
        )
    }
}

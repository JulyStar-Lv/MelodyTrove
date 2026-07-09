package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TidePageHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = if (trailing != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote1,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                it()
            }
        }
    }
}

package io.github.julystar.musicapp.core.presentation.components

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
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Shared large-title header for root and detail pages.
 * Scroll containers may place this inside a sticky item and switch [compact] as the page collapses.
 */
@Composable
fun DesignPageHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
    compact: Boolean = false,
    eyebrow: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 6.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = if (trailing != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            eyebrow?.let {
                Text(
                    text = it.uppercase(),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.footnote2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = if (compact) {
                    MiuixTheme.textStyles.title2.copy(fontSize = 24.sp, lineHeight = 30.sp)
                } else {
                    MiuixTheme.textStyles.title1
                },
                fontWeight = FontWeight.Bold,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Box(
                modifier = Modifier.padding(start = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                it()
            }
        }
    }
}

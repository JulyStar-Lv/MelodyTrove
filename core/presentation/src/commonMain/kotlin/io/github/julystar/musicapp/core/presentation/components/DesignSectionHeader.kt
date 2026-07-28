package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
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

enum class DesignSectionHeaderVariant {
    Standard,
    Compact,
    Subtle,
}

enum class DesignSectionHeaderMetadataTone {
    Default,
    Accent,
}

@Composable
fun DesignSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    titleWeight: FontWeight = FontWeight.SemiBold,
    variant: DesignSectionHeaderVariant = DesignSectionHeaderVariant.Standard,
    metadataTone: DesignSectionHeaderMetadataTone = DesignSectionHeaderMetadataTone.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleStyle = when (variant) {
        DesignSectionHeaderVariant.Standard -> MiuixTheme.textStyles.title3
        DesignSectionHeaderVariant.Compact,
        DesignSectionHeaderVariant.Subtle,
        -> MiuixTheme.textStyles.subtitle
    }
    val titleColor = when (variant) {
        DesignSectionHeaderVariant.Subtle -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        else -> MiuixTheme.colorScheme.onBackground
    }
    val metadataColor = when (metadataTone) {
        DesignSectionHeaderMetadataTone.Default -> MiuixTheme.colorScheme.onBackgroundVariant
        DesignSectionHeaderMetadataTone.Accent -> MiuixTheme.colorScheme.primary
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = if (metadata != null || trailing != null) {
                Modifier.weight(1f)
            } else {
                Modifier.fillMaxWidth()
            },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = titleColor,
                style = titleStyle,
                fontWeight = titleWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (metadata != null || trailing != null) {
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                metadata?.let {
                    Text(
                        text = it,
                        color = metadataColor,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                trailing?.invoke()
            }
        }
    }
}

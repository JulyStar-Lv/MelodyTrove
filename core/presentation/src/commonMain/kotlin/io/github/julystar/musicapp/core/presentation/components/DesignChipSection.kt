package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DesignChipSection(
    title: String,
    labels: List<String>,
    onLabelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    metadataTone: DesignSectionHeaderMetadataTone = DesignSectionHeaderMetadataTone.Default,
    trailing: (@Composable () -> Unit)? = null,
    chipLeading: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DesignSectionHeader(
            title = title,
            metadata = metadata,
            variant = DesignSectionHeaderVariant.Compact,
            metadataTone = metadataTone,
            trailing = trailing,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { label ->
                DesignChip(
                    label = label,
                    leading = chipLeading,
                    onClick = { onLabelClick(label) },
                )
            }
        }
    }
}

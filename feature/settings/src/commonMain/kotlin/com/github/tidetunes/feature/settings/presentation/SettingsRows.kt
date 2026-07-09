package com.github.tidetunes.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.AppSwitch
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TidePreferenceRow
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsPageLayout(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageMedium
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            SettingsTopBar(title = title, onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = content,
            )
        }
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: (() -> Unit)?,
) {
    val spacing = TideTunesTokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                TideChevron(direction = TideChevronDirection.Left)
            }
        }
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (onBack != null) 4.dp else 16.dp),
        )
    }
}

@Composable
internal fun SettingsEntryCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        onClick = onClick,
        trailing = {
            TideChevron(direction = TideChevronDirection.Right)
        },
    )
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    TideSettingsGroup(title = title, content = content)
}

@Composable
internal fun SettingsChoiceRow(
    title: String,
    summary: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            ChoiceIndicator(selected = selected)
        },
        showDivider = false,
    )
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
        showDivider = false,
    )
}

@Composable
internal fun SettingsInfoRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    TidePreferenceRow(
        title = title,
        summary = value,
        enabled = enabled,
        onClick = onClick,
        trailing = if (onClick != null) {
            {
                TideChevron(direction = TideChevronDirection.Right)
            }
        } else {
            null
        },
        showDivider = false,
    )
}

@Composable
internal fun SettingsDangerRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        onClick = onClick,
        titleColor = MiuixTheme.colorScheme.error,
        showDivider = false,
    )
}

@Composable
internal fun SettingsConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TideDialog(
        show = show,
        onDismiss = onDismiss,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.xs))
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TideTextButton(
                text = "Cancel",
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Medium,
                onClick = onDismiss,
            )
            TideTextButton(
                text = confirmText,
                variant = TideTextButtonVariant.Error,
                size = TideTextButtonSize.Medium,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
internal fun SettingsInputDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TideDialog(
        show = show,
        onDismiss = onDismiss,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.xs))
        Text(
            text = "请输入 0 到 10240 MB。",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.sm))
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = "MB",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TideTextButton(
                text = "Cancel",
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Medium,
                onClick = onDismiss,
            )
            TideTextButton(
                text = "Save",
                variant = TideTextButtonVariant.Primary,
                size = TideTextButtonSize.Medium,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun ChoiceIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(
                if (selected) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.outline,
            ),
    )
}

internal fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "暂不可用"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${formatOneDecimal(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${formatOneDecimal(mb)} MB"
    return "${formatOneDecimal(mb / 1024.0)} GB"
}

private fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10).toLong()
    val whole = scaled / 10
    val decimal = scaled % 10
    return if (decimal == 0L) whole.toString() else "$whole.$decimal"
}

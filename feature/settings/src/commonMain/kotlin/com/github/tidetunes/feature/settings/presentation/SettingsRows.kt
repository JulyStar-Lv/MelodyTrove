package com.github.tidetunes.feature.settings.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.window.Popup
import com.github.tidetunes.core.presentation.components.TideLoadingIndicator
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.github.tidetunes.core.presentation.components.AppSwitch
import com.github.tidetunes.core.presentation.components.AppTextField
import com.github.tidetunes.core.presentation.components.TideChevron
import com.github.tidetunes.core.presentation.components.TideChevronDirection
import com.github.tidetunes.core.presentation.components.TideDialog
import com.github.tidetunes.core.presentation.components.TidePreferenceRow
import com.github.tidetunes.core.presentation.components.TideSettingsGroup
import com.github.tidetunes.core.presentation.components.TideSlider
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_adjust
import tidetunes.core.presentation.generated.resources.icon_album
import tidetunes.core.presentation.generated.resources.icon_cloud
import tidetunes.core.presentation.generated.resources.icon_dashboard
import tidetunes.core.presentation.generated.resources.icon_download
import tidetunes.core.presentation.generated.resources.icon_file
import tidetunes.core.presentation.generated.resources.icon_folder
import tidetunes.core.presentation.generated.resources.icon_image
import tidetunes.core.presentation.generated.resources.icon_lyrics
import tidetunes.core.presentation.generated.resources.icon_mode_repeat
import tidetunes.core.presentation.generated.resources.icon_music_note
import tidetunes.core.presentation.generated.resources.icon_onedrive
import tidetunes.core.presentation.generated.resources.icon_ok
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.core.presentation.generated.resources.icon_search
import tidetunes.core.presentation.generated.resources.icon_setting
import tidetunes.core.presentation.generated.resources.icon_timelapse
import tidetunes.core.presentation.generated.resources.icon_wifitethering
import tidetunes.feature.settings.generated.resources.Res as SettingsRes
import tidetunes.feature.settings.generated.resources.settings_cancel
import tidetunes.feature.settings.generated.resources.settings_save
import kotlin.math.roundToInt

@Composable
internal fun SettingsPageLayout(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = spacing.pageCompact
        val showTopBar = maxWidth < 1024.dp || onBack != null
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            if (showTopBar) {
                SettingsTopBar(title = title, onBack = onBack)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
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
            style = MiuixTheme.textStyles.title1,
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
    summary: String?,
    icon: DrawableResource,
    onClick: (() -> Unit)? = null,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        onClick = onClick,
        leading = {
            SettingsLeadingIcon(drawable = icon)
        },
        trailing = if (onClick != null) {
            { TideChevron(direction = TideChevronDirection.Right) }
        } else {
            null
        },
    )
}

@Composable
private fun SettingsLeadingIcon(drawable: DrawableResource) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(16.dp),
        )
    }
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
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    marker: String? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        leading = marker?.let { iconMarker ->
            { SettingsLeadingIcon(marker = iconMarker, accentColor = accentColor) }
        },
        trailing = {
            AppSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        },
    )
}

@Composable
internal fun SettingsSliderRow(
    title: String,
    summary: String? = null,
    value: Int,
    valueRange: IntRange,
    valueText: String,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onValueChange: (Int) -> Unit,
) {
    var previewValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.main,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = valueText,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            if (summary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            TideSlider(
                value = previewValue,
                onValueChange = { previewValue = it },
                onValueChangeFinished = { onValueChange(previewValue.roundToInt()) },
                enabled = enabled,
                valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
            )
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.dividerLine.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
internal fun SettingsInfoRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    marker: String? = null,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    TidePreferenceRow(
        title = title,
        summary = value,
        enabled = enabled,
        onClick = onClick,
        leading = marker?.let { iconMarker ->
            { SettingsLeadingIcon(marker = iconMarker, accentColor = accentColor) }
        },
        trailing = if (onClick != null) {
            {
                TideChevron(direction = TideChevronDirection.Right)
            }
        } else {
            null
        },
    )
}

@Composable
internal fun SettingsDangerRow(
    title: String,
    summary: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TidePreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        titleColor = MiuixTheme.colorScheme.error,
        showDivider = false,
    )
}

@Composable
private fun SettingsLeadingIcon(
    marker: String,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val drawable = markerDrawable(marker)
        if (drawable != null) {
            Icon(
                painter = painterResource(drawable),
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                text = marker,
                color = if (marker == "●") accentColor else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
    }
}

private fun markerDrawable(marker: String): DrawableResource? = when (marker) {
    "≋" -> CoreRes.drawable.icon_wifitethering
    "◠", "≡", "◈" -> CoreRes.drawable.icon_adjust
    "↓" -> CoreRes.drawable.icon_download
    "▷" -> CoreRes.drawable.icon_play
    "↻", "↺" -> CoreRes.drawable.icon_mode_repeat
    "⌁" -> CoreRes.drawable.icon_timelapse
    "◎", "W", "G", "E", "P", "J", "N", "D", "C" -> CoreRes.drawable.icon_cloud
    "O" -> CoreRes.drawable.icon_onedrive
    "S" -> CoreRes.drawable.icon_folder
    "▦", "▣" -> CoreRes.drawable.icon_album
    "DS" -> CoreRes.drawable.icon_dashboard
    "◇", "文", "§" -> CoreRes.drawable.icon_file
    "♫" -> CoreRes.drawable.icon_lyrics
    "◐", "◌" -> CoreRes.drawable.icon_image
    "▢" -> CoreRes.drawable.icon_dashboard
    "☾" -> CoreRes.drawable.icon_setting
    "⌕" -> CoreRes.drawable.icon_search
    "♪" -> CoreRes.drawable.icon_music_note
    else -> null
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
                text = stringResource(SettingsRes.string.settings_cancel),
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
    message: String,
    value: String,
    label: String,
    singleLine: Boolean = true,
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
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.sm))
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(TideTunesTokens.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TideTextButton(
                text = stringResource(SettingsRes.string.settings_cancel),
                variant = TideTextButtonVariant.Default,
                size = TideTextButtonSize.Medium,
                onClick = onDismiss,
            )
            TideTextButton(
                text = stringResource(SettingsRes.string.settings_save),
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
    if (bytes == null) return "—"
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

// ── Select Row (popup choice menu) ──

@Composable
internal fun SettingsSelectRow(
    label: String,
    subtitle: String? = null,
    selectedValue: String,
    selectedLabel: String,
    options: List<SettingsSelectOption>,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { menuOpen = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = selectedLabel,
                color = if (menuOpen) MiuixTheme.colorScheme.primary
                else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
            // Up/down chevron
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TideChevron(
                    direction = TideChevronDirection.Right,
                    size = 10.dp,
                    modifier = Modifier.graphicsLayer(rotationZ = -90f),
                )
                TideChevron(
                    direction = TideChevronDirection.Right,
                    size = 10.dp,
                    modifier = Modifier.graphicsLayer(rotationZ = 90f),
                )
            }
        }

        if (menuOpen) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { menuOpen = false },
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 200.dp, max = 300.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = option.value == selectedValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    onSelect(option.value)
                                    menuOpen = false
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSelected) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.onSurface,
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(CoreRes.drawable.icon_ok),
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
internal data class SettingsSelectOption(
    val value: String,
    val label: String,
)

// ── Action Row (destructive actions with confirm states) ──

@Composable
internal fun SettingsActionRow(
    label: String,
    subtitle: String,
    state: SettingsActionState,
    actionLabel: String = "Clear",
    onStateChange: (SettingsActionState) -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
            )
            val statusText = when (state) {
                SettingsActionState.Busy -> "Working…"
                SettingsActionState.Success -> "Done"
                SettingsActionState.Error -> "Failed — tap to retry"
                else -> subtitle
            }
            Text(
                text = statusText,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote1,
            )

            // Confirm buttons
            if (state == SettingsActionState.Confirm) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Confirm",
                        color = Color.White,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.error)
                            .clickable { onConfirm() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Text(
                        text = "Cancel",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onStateChange(SettingsActionState.Idle) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // Right side state indicator
        when (state) {
            SettingsActionState.Busy -> TideLoadingIndicator(size = 18.dp)
            SettingsActionState.Success -> Icon(
                painter = painterResource(CoreRes.drawable.icon_ok),
                contentDescription = null,
                tint = TideTunesBrand.SupportGreen,
                modifier = Modifier.size(18.dp),
            )
            SettingsActionState.Idle -> Text(
                text = actionLabel,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onStateChange(SettingsActionState.Confirm) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
            else -> {}
        }
    }
}

internal enum class SettingsActionState {
    Idle,
    Confirm,
    Busy,
    Success,
    Error,
}

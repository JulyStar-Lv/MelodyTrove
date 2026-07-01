package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorColors
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MiuixTheme.textStyles.main,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AppIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurface,
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColorsPrimary(),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    colors: CardColors = CardDefaults.defaultColors(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = cornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = label.isNotEmpty(),
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun AppLinearProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(),
) {
    LinearProgressIndicator(
        modifier = modifier,
        progress = progress,
        colors = colors,
    )
}

@Composable
fun AppChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AppDialog(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        content = content,
    )
}

@Composable
fun AppBottomSheet(
    show: Boolean,
    title: String? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        content = content,
    )
}

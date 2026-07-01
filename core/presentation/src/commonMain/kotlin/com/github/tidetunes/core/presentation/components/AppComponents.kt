package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// --- AppTopBar ---

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
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
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke()
        }
    }
}

// --- AppIconButton ---

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MiuixTheme.colorScheme.onSurface,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

// --- AppSectionHeader ---

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        action?.invoke()
    }
}

// --- AppLoadingIndicator ---

@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            colors = ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = MiuixTheme.colorScheme.primary,
            ),
            strokeWidth = 3.dp,
            size = 32.dp,
        )
    }
}

// --- AppEmptyState ---

@Composable
fun AppEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            action?.let {
                Spacer(Modifier.height(12.dp))
                it()
            }
        }
    }
}

// --- AppErrorState ---

@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.error,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                TideTunesTextButton(
                    onClick = onRetry,
                    text = "Retry",
                    type = TideTunesTextButtonType.Primary,
                    size = TideTunesTextButtonSize.Medium,
                )
            }
        }
    }
}

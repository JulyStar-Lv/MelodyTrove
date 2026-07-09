package com.github.tidetunes.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
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
    TideTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
    )
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
    TideIconButton(
        size = TideIconButtonSize.Medium,
        variant = TideIconButtonVariant.Default,
        painter = painter,
        onClick = onClick,
        modifier = modifier,
        contentDescription = contentDescription,
        colors = TideIconButtonColors(iconTint = tint),
        enabled = enabled,
    )
}

// --- AppSectionHeader ---

@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    TideSectionHeader(
        title = title,
        variant = TideSectionHeaderVariant.Subtle,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        trailing = action,
    )
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
        TideLoadingIndicator()
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
                TideTextButton(
                    onClick = onRetry,
                    text = "Retry",
                    variant = TideTextButtonVariant.Primary,
                    size = TideTextButtonSize.Medium,
                )
            }
        }
    }
}

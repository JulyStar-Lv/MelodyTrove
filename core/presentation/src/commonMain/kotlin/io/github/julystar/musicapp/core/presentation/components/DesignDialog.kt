package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

object DesignDialogDefaults {
    val scrimColor = Color.Black.copy(alpha = 0.56f)
    const val maxHeightFraction = 0.86f
    val maxHeight = 640.dp

    fun isCompactWindow(viewportWidth: Dp): Boolean =
        viewportWidth.isSpecified && viewportWidth < 600.dp

    fun scrimEnterTransition(): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing,
        ),
    )

    fun scrimExitTransition(): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutLinearInEasing,
        ),
    )

    fun surfaceEnterTransition(): EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = 220,
                easing = LinearOutSlowInEasing,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 260,
                easing = FastOutSlowInEasing,
            ),
            initialOffsetY = { height ->
                (height * 0.02f).roundToInt().coerceIn(8, 24)
            },
        )

    fun surfaceExitTransition(): ExitTransition =
        fadeOut(
            animationSpec = tween(
                durationMillis = 160,
                easing = FastOutLinearInEasing,
            ),
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 180,
                easing = FastOutLinearInEasing,
            ),
            targetOffsetY = { height ->
                (height * 0.012f).roundToInt().coerceIn(6, 16)
            },
        )
}

fun resolveDialogMaxHeight(requestedMaxHeight: Dp?, viewportHeight: Dp): Dp {
    val maxHeight = minOf(
        requestedMaxHeight ?: DesignDialogDefaults.maxHeight,
        DesignDialogDefaults.maxHeight,
    )
    return if (viewportHeight.isSpecified) {
        minOf(maxHeight, viewportHeight * DesignDialogDefaults.maxHeightFraction)
    } else {
        maxHeight
    }
}

@Composable
fun DesignDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 520.dp,
    maxHeight: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibilityState = remember { MutableTransitionState(false) }
    visibilityState.targetState = show
    if (!visibilityState.currentState && !visibilityState.targetState) return

    val viewportSize = LocalWindowInfo.current.containerDpSize
    val compact = DesignDialogDefaults.isCompactWindow(viewportSize.width)
    val dialogRadius = DesignTokens.shapes.lg
    val shape = if (compact) {
        RoundedCornerShape(
            topStart = dialogRadius,
            topEnd = dialogRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        )
    } else {
        RoundedCornerShape(dialogRadius)
    }
    val alignment = if (compact) Alignment.BottomCenter else Alignment.Center
    val resolvedMaxHeight = resolveDialogMaxHeight(maxHeight, viewportSize.height)
    val contentPadding = if (compact) {
        PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp)
    } else {
        PaddingValues(20.dp)
    }

    DesignDialogHost(
        onDismissRequest = onDismiss,
        navigationBarStyle = if (compact) {
            DesignDialogNavigationBarStyle.Surface
        } else {
            DesignDialogNavigationBarStyle.Dimmed
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedVisibility(
                visibleState = visibilityState,
                modifier = Modifier.fillMaxSize(),
                enter = DesignDialogDefaults.scrimEnterTransition(),
                exit = DesignDialogDefaults.scrimExitTransition(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DesignDialogDefaults.scrimColor)
                        .clickable(
                            enabled = show,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onDismiss() },
                )
            }

            AnimatedVisibility(
                visible = show,
                modifier = Modifier.align(alignment),
                enter = DesignDialogDefaults.surfaceEnterTransition(),
                exit = DesignDialogDefaults.surfaceExitTransition(),
            ) {
                Column(
                    modifier = (if (compact) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.widthIn(min = 280.dp, max = maxWidth)
                    })
                        .heightIn(max = resolvedMaxHeight)
                        .then(modifier)
                        .shadow(DesignTokens.elevation.overlay, shape)
                        .clip(shape)
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MiuixTheme.colorScheme.outline, shape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { }
                        .then(
                            if (compact) Modifier.navigationBarsPadding() else Modifier,
                        )
                        .padding(contentPadding),
                    content = content,
                )
            }
        }
    }
}

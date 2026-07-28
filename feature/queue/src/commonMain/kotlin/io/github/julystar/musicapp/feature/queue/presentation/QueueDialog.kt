package io.github.julystar.musicapp.feature.queue.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.julystar.musicapp.core.presentation.components.AppTrackRow
import io.github.julystar.musicapp.core.presentation.components.DesignIconButton
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonColors
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTrackNumberBadge
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import musicapp.core.presentation.generated.resources.Res as CoreRes
import musicapp.core.presentation.generated.resources.icon_deleteseep
import musicapp.core.presentation.generated.resources.icon_mode_list
import musicapp.feature.queue.generated.resources.Res as QueueRes
import musicapp.feature.queue.generated.resources.icon_locate
import musicapp.feature.queue.generated.resources.queue_clear
import musicapp.feature.queue.generated.resources.queue_empty
import musicapp.feature.queue.generated.resources.queue_locate_current
import musicapp.feature.queue.generated.resources.queue_title
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val QueueSideDialogWidth = 480.dp
private val QueueBottomDialogMaxWidth = 680.dp
private val QueueBottomDialogMaxHeight = 720.dp
private const val QueueEnterDurationMillis = 240
private const val QueueExitDurationMillis = 180

@Composable
fun QueueDialog(
    state: QueueState,
    onDismiss: () -> Unit,
    onAction: (QueueAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val hasCurrentItem = state.currentIndex in state.items.indices
    var contentVisible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }

    fun requestDismiss() {
        if (dismissing) return
        dismissing = true
        contentVisible = false
        coroutineScope.launch {
            delay(QueueExitDurationMillis.toLong())
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(QueueEnterDurationMillis)),
                exit = fadeOut(tween(QueueExitDurationMillis)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.48f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { requestDismiss() })
                        },
                )
            }

            val sideDialog = maxWidth >= 600.dp || maxWidth > maxHeight
            val surfaceShape: Shape = if (sideDialog) {
                RectangleShape
            } else {
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            }
            val surfaceModifier = if (sideDialog) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(max = QueueSideDialogWidth)
                    .fillMaxWidth()
                    .fillMaxHeight()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = QueueBottomDialogMaxWidth)
                    .fillMaxWidth()
                    .height(minOf(maxHeight * 0.76f, QueueBottomDialogMaxHeight))
            }

            AnimatedVisibility(
                visible = contentVisible,
                modifier = surfaceModifier,
                enter = if (sideDialog) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(QueueEnterDurationMillis, easing = FastOutSlowInEasing),
                    ) + fadeIn(tween(QueueEnterDurationMillis))
                } else {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(QueueEnterDurationMillis, easing = FastOutSlowInEasing),
                    ) + fadeIn(tween(QueueEnterDurationMillis))
                },
                exit = if (sideDialog) {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(QueueExitDurationMillis, easing = FastOutSlowInEasing),
                    ) + fadeOut(tween(QueueExitDurationMillis))
                } else {
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(QueueExitDurationMillis, easing = FastOutSlowInEasing),
                    ) + fadeOut(tween(QueueExitDurationMillis))
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(surfaceShape)
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MiuixTheme.colorScheme.outline, surfaceShape)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent()
                            }
                        },
                ) {
                    if (!sideDialog) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 12.dp)
                                .size(width = 48.dp, height = 6.dp)
                                .clip(RoundedCornerShape(DesignTokens.shapes.full))
                                .background(MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.30f)),
                        )
                    }

                    QueueHeader(
                        itemCount = state.items.size,
                        canLocateCurrent = hasCurrentItem,
                        onLocateCurrent = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.currentIndex)
                            }
                        },
                        onClear = { onAction(QueueAction.ClearQueue) },
                    )

                    if (state.items.isEmpty()) {
                        QueueEmptyState(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
                        ) {
                            itemsIndexed(
                                items = state.items,
                                key = { index, item -> item.lazyListKey(index) },
                            ) { index, item ->
                                AppTrackRow(
                                    title = item.title,
                                    artist = item.artist,
                                    duration = null,
                                    active = item.isCurrent && state.isPlaying,
                                    cover = {
                                        DesignTrackNumberBadge(
                                            label = (index + 1).toString(),
                                            active = item.isCurrent && state.isPlaying,
                                        )
                                    },
                                    onClick = { onAction(QueueAction.PlayItem(item.index)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    itemCount: Int,
    canLocateCurrent: Boolean,
    onLocateCurrent: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CoreRes.drawable.icon_mode_list),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(QueueRes.string.queue_title, itemCount),
                modifier = Modifier.weight(1f),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title3.copy(fontSize = 20.sp, lineHeight = 24.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DesignIconButton(
                size = DesignIconButtonSize.Medium,
                variant = DesignIconButtonVariant.Default,
                painter = painterResource(QueueRes.drawable.icon_locate),
                contentDescription = stringResource(QueueRes.string.queue_locate_current),
                colors = QueueHeaderActionColors(),
                enabled = canLocateCurrent,
                onClick = onLocateCurrent,
            )
            DesignIconButton(
                size = DesignIconButtonSize.Medium,
                variant = DesignIconButtonVariant.Default,
                painter = painterResource(CoreRes.drawable.icon_deleteseep),
                contentDescription = stringResource(QueueRes.string.queue_clear),
                colors = QueueHeaderActionColors(),
                onClick = onClear,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MiuixTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun QueueHeaderActionColors() = DesignIconButtonColors(
    buttonBg = MiuixTheme.colorScheme.surfaceContainerHigh,
    iconTint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
)

@Composable
private fun QueueEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 208.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_mode_list),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.45f),
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(QueueRes.string.queue_empty),
            modifier = Modifier.padding(top = 12.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
        )
    }
}

internal fun QueueItemUi.lazyListKey(index: Int): String = "queue-item-$index-${this.index}"

package io.github.julystar.musicapp.feature.queue.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.julystar.musicapp.core.presentation.components.DesignIconButton
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonColors
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignIconButtonVariant
import io.github.julystar.musicapp.core.presentation.components.DesignDialogDefaults
import io.github.julystar.musicapp.core.presentation.components.DesignContextMenu
import io.github.julystar.musicapp.core.presentation.components.DesignContextMenuItem
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import musicapp.core.presentation.generated.resources.Res as CoreRes
import musicapp.core.presentation.generated.resources.icon_heart
import musicapp.core.presentation.generated.resources.icon_heart_filled
import musicapp.core.presentation.generated.resources.icon_mode_list
import musicapp.core.presentation.generated.resources.icon_settings_list_music
import musicapp.core.presentation.generated.resources.icon_vertialcal_more
import musicapp.feature.queue.generated.resources.Res as QueueRes
import musicapp.feature.queue.generated.resources.icon_locate
import musicapp.feature.queue.generated.resources.icon_queue_trash
import musicapp.feature.queue.generated.resources.queue_add_favorite
import musicapp.feature.queue.generated.resources.queue_clear
import musicapp.feature.queue.generated.resources.queue_empty
import musicapp.feature.queue.generated.resources.queue_locate_current
import musicapp.feature.queue.generated.resources.queue_more_actions
import musicapp.feature.queue.generated.resources.queue_remove_favorite
import musicapp.feature.queue.generated.resources.queue_remove_item
import musicapp.feature.queue.generated.resources.queue_title
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val QueueSideDialogWidth = 480.dp
private val QueueBottomDialogMaxWidth = 680.dp
private val QueueBottomDialogMaxHeight = 720.dp
private val QueueWideMinWidth = 860.dp
private val QueueWideMinHeight = 520.dp
private val QueueLandscapeMinWidth = 640.dp
private val NowPlayingContentStartPadding = 34.dp
private val NowPlayingContentEndPadding = 28.dp
private val NowPlayingColumnsGap = 34.dp
private const val NowPlayingLyricsWeight = 0.54f
private const val QueueEnterDurationMillis = 240
private const val QueueExitDurationMillis = 180

/** Matches the maintained Design player breakpoints for the queue surface. */
internal fun isQueueSideDialog(maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp): Boolean =
    (maxWidth >= QueueWideMinWidth && maxHeight >= QueueWideMinHeight) ||
        (
            maxWidth >= QueueLandscapeMinWidth &&
                maxWidth > maxHeight &&
                maxHeight < QueueWideMinHeight
            )

/** Matches the desktop [LyricsSurface] width so the queue fully replaces that column. */
internal fun nowPlayingLyricsPanelWidth(maxWidth: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    (maxWidth - NowPlayingContentStartPadding - NowPlayingContentEndPadding - NowPlayingColumnsGap) *
        NowPlayingLyricsWeight + NowPlayingContentEndPadding

@Composable
fun QueueDialog(
    state: QueueState,
    coverNowPlayingLyrics: Boolean,
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
                        .background(DesignDialogDefaults.scrimColor)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { requestDismiss() })
                        },
                )
            }

            val sideDialog = isQueueSideDialog(maxWidth, maxHeight)
            val coversDesktopLyrics = coverNowPlayingLyrics &&
                maxWidth >= QueueWideMinWidth &&
                maxHeight >= QueueWideMinHeight
            val surfaceShape: Shape = if (sideDialog) {
                RectangleShape
            } else {
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            }
            val surfaceModifier = if (sideDialog) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(
                        if (coversDesktopLyrics) {
                            nowPlayingLyricsPanelWidth(maxWidth)
                        } else {
                            QueueSideDialogWidth
                        },
                    )
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
                        .then(
                            if (sideDialog) {
                                Modifier.shadow(
                                    elevation = DesignTokens.elevation.overlay,
                                    shape = RectangleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clip(surfaceShape)
                        .background(MiuixTheme.colorScheme.surfaceContainer)
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            itemsIndexed(
                                items = state.items,
                                key = { index, item -> item.lazyListKey(index) },
                            ) { index, item ->
                                QueueTrackRow(
                                    item = item,
                                    position = index + 1,
                                    active = item.isCurrent && state.isPlaying,
                                    onClick = { onAction(QueueAction.PlayItem(item.index)) },
                                    onToggleFavorite = {
                                        item.trackId?.let { trackId ->
                                            onAction(QueueAction.ToggleFavorite(trackId))
                                        }
                                    },
                                    onRemove = { onAction(QueueAction.RemoveItem(item.index)) },
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
    val dividerColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.08f)
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    painter = painterResource(CoreRes.drawable.icon_settings_list_music),
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
                painter = painterResource(QueueRes.drawable.icon_queue_trash),
                contentDescription = stringResource(QueueRes.string.queue_clear),
                colors = QueueHeaderActionColors(),
                onClick = onClear,
            )
        }
    }
}

@Composable
private fun QueueTrackRow(
    item: QueueItemUi,
    position: Int,
    active: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val contentColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
    val secondaryColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    val dividerColor = MiuixTheme.colorScheme.outline.copy(alpha = 0.05f)
    val subtitle = item.subtitle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (active) {
                    QueuePlayingIndicator()
                } else {
                    Text(
                        text = position.toString(),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.footnote2.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body1.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let { secondaryText ->
                    Text(
                        text = secondaryText,
                        color = secondaryColor,
                        style = MiuixTheme.textStyles.footnote1.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.width(64.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = item.trackId != null,
                        onClick = onToggleFavorite,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (item.isFavorite) {
                            CoreRes.drawable.icon_heart_filled
                        } else {
                            CoreRes.drawable.icon_heart
                        },
                    ),
                    contentDescription = stringResource(
                        if (item.isFavorite) {
                            QueueRes.string.queue_remove_favorite
                        } else {
                            QueueRes.string.queue_add_favorite
                        },
                        item.title,
                    ),
                    tint = if (item.isFavorite) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { moreMenuExpanded = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(CoreRes.drawable.icon_vertialcal_more),
                        contentDescription = stringResource(
                            QueueRes.string.queue_more_actions,
                            item.title,
                        ),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DesignContextMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                    compact = true,
                    items = listOf(
                        DesignContextMenuItem(
                            label = QueueRes.string.queue_remove_item,
                            isError = true,
                            onClick = {
                                moreMenuExpanded = false
                                onRemove()
                            },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun QueuePlayingIndicator() {
    val transition = rememberInfiniteTransition(label = "queue-playing")
    val heights = listOf(0.35f, 0.70f, 0.50f, 0.80f)

    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, initialHeight ->
            val height by transition.animateFloat(
                initialValue = initialHeight,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 800,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "queue-playing-bar-$index",
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp * height)
                    .clip(RoundedCornerShape(DesignTokens.shapes.full))
                    .background(MiuixTheme.colorScheme.primary),
            )
        }
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

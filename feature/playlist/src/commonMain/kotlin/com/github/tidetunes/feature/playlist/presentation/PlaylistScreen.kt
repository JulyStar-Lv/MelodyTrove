package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.ConfirmDialog
import com.github.tidetunes.core.presentation.components.TideTunesContextMenu
import com.github.tidetunes.core.presentation.components.TideTunesContextMenuItem
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonColors
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import com.github.tidetunes.core.presentation.components.MusicCover
import org.jetbrains.compose.resources.painterResource
import com.github.tidetunes.core.presentation.components.customAnchoredDraggable
import com.github.tidetunes.core.presentation.components.rememberCustomAnchoredDraggableState
import com.github.tidetunes.core.presentation.components.tideTunesIconButtonSizeToDp
import com.github.tidetunes.core.presentation.components.BottomBarSpacer
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.cover_default_playlist_image
import tidetunes.feature.playlist.generated.resources.empty_playlist
import tidetunes.feature.playlist.generated.resources.icon_back
import tidetunes.feature.playlist.generated.resources.icon_deleteseep
import tidetunes.feature.playlist.generated.resources.icon_download
import tidetunes.feature.playlist.generated.resources.icon_play
import tidetunes.feature.playlist.generated.resources.icon_vertialcal_more
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_edit
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_import
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_remove
import tidetunes.feature.playlist.generated.resources.playlist_empty_list
import tidetunes.feature.playlist.generated.resources.playlist_list_count_suffix
import tidetunes.feature.playlist.generated.resources.playlist_list_count_suffixes
import tidetunes.feature.playlist.generated.resources.playlist_remove_dialog_text

@Composable
fun PlaylistScreen(
    state: PlaylistState,
    currentPlayingTrackId: Long?,
    scaffoldPadding: PaddingValues,
    onAction: (PlaylistAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MiuixTheme.colorScheme.surface)
            .fillMaxSize(),
    ) {
        Column {
            PlaylistHeader(
                state = state,
                onAction = onAction,
            )
            if (state.tracks.isEmpty()) {
                EmptyPlaylist()
            } else {
                PlaylistItemsBlock(
                    state = state,
                    currentPlayingTrackId = currentPlayingTrackId,
                    scaffoldPadding = scaffoldPadding,
                    onAction = onAction,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset((-20).dp, 157.dp - tideTunesIconButtonSizeToDp(TideTunesIconButtonSize.Large) / 2),
        ) {
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Large,
                buttonType = TideTunesIconButtonType.Primary,
                painter = painterResource(Res.drawable.icon_play),
                disabled = state.tracks.isEmpty(),
                onClick = {
                    onAction(PlaylistAction.PlayAll)
                },
            )
        }
    }
    RemovePlaylistDialog(
        state = state,
        onAction = onAction,
    )
}

@Composable
private fun RemovePlaylistDialog(
    state: PlaylistState,
    onAction: (PlaylistAction) -> Unit,
) {
    ConfirmDialog(
        open = state.isRemoveDialogOpen,
        onConfirm = {
            onAction(PlaylistAction.ConfirmRemovePlaylist)
        },
        onCancel = {
            onAction(PlaylistAction.CloseRemoveDialog)
        },
    ) {
        Text(
            text = "${stringResource(Res.string.playlist_remove_dialog_text)} \"${state.title}\""
        )
    }
}

@Composable
private fun PlaylistHeader(
    state: PlaylistState,
    onAction: (PlaylistAction) -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val countSuffixStringRes = if (state.tracks.size <= 1) {
        Res.string.playlist_list_count_suffix
    } else {
        Res.string.playlist_list_count_suffixes
    }

    Box(
        modifier = Modifier
            .height(157.dp)
            .fillMaxWidth(),
    ) {
        if (state.cover == null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(Res.drawable.cover_default_playlist_image),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                MusicCover(
                    modifier = Modifier.fillMaxSize(),
                    artwork = state.cover,
                )
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .fillMaxSize(),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(13.dp, 13.dp)
                .fillMaxWidth(),
        ) {
            TideTunesIconButton(
                sizeType = TideTunesIconButtonSize.Medium,
                buttonType = TideTunesIconButtonType.Surface,
                painter = painterResource(Res.drawable.icon_back),
                overrideColors = TideTunesIconButtonColors().copy(iconTint = Color.White),
                onClick = {
                    onAction(PlaylistAction.NavigateBack)
                },
            )
            Box {
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Surface,
                    overrideColors = TideTunesIconButtonColors().copy(iconTint = Color.White),
                    painter = painterResource(Res.drawable.icon_vertialcal_more),
                    onClick = { moreMenuExpanded = true },
                )
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.offset(20.dp, 20.dp),
                ) {
                    TideTunesContextMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                        items = listOf(
                            TideTunesContextMenuItem(
                                label = Res.string.playlist_context_menu_import,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(PlaylistAction.ImportTracks)
                                },
                            ),
                            TideTunesContextMenuItem(
                                label = Res.string.playlist_context_menu_edit,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(PlaylistAction.EditPlaylist)
                                },
                            ),
                            TideTunesContextMenuItem(
                                label = Res.string.playlist_context_menu_remove,
                                isError = true,
                                onClick = {
                                    moreMenuExpanded = false
                                    onAction(PlaylistAction.OpenRemoveDialog)
                                },
                            ),
                        ),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(48.dp, 0.dp)
                .offset(0.dp, 60.dp),
        ) {
            Text(
                text = state.title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 26.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
            Text(
                text = "${state.tracks.size} ${stringResource(countSuffixStringRes)} · ${state.durationLabel}",
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun EmptyPlaylist() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {}
                .clip(RoundedCornerShape(16.dp))
                .padding(24.dp, 24.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.empty_playlist),
                contentDescription = null,
            )
            Box(modifier = Modifier.height(11.dp))
            Text(
                text = stringResource(Res.string.playlist_empty_list),
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlaylistItem(
    item: PlaylistTrackItem,
    index: Int,
    playing: Boolean,
    currentSwipingTrackId: Long?,
    onSwipe: () -> Unit,
    onAction: (PlaylistAction) -> Unit,
) {
    val density = LocalDensity.current
    val panelWidthDp = 48.dp

    val anchoredDraggableState = with(density) {
        rememberCustomAnchoredDraggableState(
            initialValue = 0f,
            anchors = mapOf(
                0.dp.toPx() to "START",
                -(panelWidthDp * 2 + 8.dp).toPx() to "END",
            ),
            animationSpec = tween(200),
        )
    }

    val color = if (playing) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    val bgColor = if (playing) {
        MiuixTheme.colorScheme.secondary
    } else {
        Color.Transparent
    }
    val durationColor = if (playing) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val dragHandleColor = if (playing) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    LaunchedEffect(currentSwipingTrackId) {
        if (currentSwipingTrackId != item.id) {
            anchoredDraggableState.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .padding(
                start = 20.dp,
                end = 20.dp,
            )
            .fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .offset(x = with(density) { anchoredDraggableState.value.toDp() })
                .clip(RoundedCornerShape(14.dp))
                .customAnchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        onSwipe()
                    },
                )
                .clickable {
                    onAction(PlaylistAction.PlayTrack(item.id))
                    onSwipe()
                }
                .background(bgColor)
                .padding(8.dp, 16.dp)
                .fillMaxWidth()
                .padding(6.dp, 0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    modifier = Modifier.draggableHandle(),
                    text = (index + 1).toString(),
                    color = dragHandleColor,
                    maxLines = 1,
                    fontSize = 14.sp,
                )
                Text(
                    text = item.title,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                )
            }
            Box(modifier = Modifier.width(16.dp))
            Text(
                text = item.durationLabel,
                color = durationColor,
                maxLines = 1,
                modifier = Modifier.wrapContentWidth(),
                fontSize = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .clipToBounds()
                .fillMaxSize()
                .align(alignment = Alignment.CenterEnd),
        ) {
            Row(
                modifier = Modifier
                    .offset(x = panelWidthDp + with(density) { anchoredDraggableState.value.toDp() })
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(modifier = Modifier.width(8.dp))
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.Default,
                    painter = painterResource(Res.drawable.icon_download),
                    onClick = {
                        onAction(PlaylistAction.DownloadTrack(item))
                    },
                )
                Box(modifier = Modifier.width(8.dp))
                TideTunesIconButton(
                    sizeType = TideTunesIconButtonSize.Medium,
                    buttonType = TideTunesIconButtonType.ErrorVariant,
                    painter = painterResource(Res.drawable.icon_deleteseep),
                    onClick = {
                        onAction(PlaylistAction.RemoveTrack(item.id))
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistItemsBlock(
    state: PlaylistState,
    currentPlayingTrackId: Long?,
    scaffoldPadding: PaddingValues,
    onAction: (PlaylistAction) -> Unit,
) {
    var swipingTrackId by remember {
        mutableStateOf<Long?>(null)
    }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            onAction(PlaylistAction.MoveTrack(from.index - 1, to.index - 1))
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        state = lazyListState,
    ) {
        item {
            Box(modifier = Modifier.height(48.dp))
        }
        items(state.tracks.size, key = { state.tracks[it].id }) {
            val item = state.tracks[it]
            val playing = item.id == currentPlayingTrackId

            ReorderableItem(reorderableLazyListState, key = item.id) { _ ->
                PlaylistItem(
                    item = item,
                    index = it,
                    playing = playing,
                    currentSwipingTrackId = swipingTrackId,
                    onSwipe = { swipingTrackId = item.id },
                    onAction = { action ->
                        if (action is PlaylistAction.RemoveTrack && swipingTrackId == item.id) {
                            swipingTrackId = null
                        }
                        onAction(action)
                    },
                )
            }
        }
        item {
            BottomBarSpacer(
                hasCurrentMusic = currentPlayingTrackId != null,
                scaffoldPadding = scaffoldPadding,
            )
        }
    }
}

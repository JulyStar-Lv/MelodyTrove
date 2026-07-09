package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.BottomBarSpacer
import com.github.tidetunes.core.presentation.components.ConfirmDialog
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideDetailHeaderSurface
import com.github.tidetunes.core.presentation.components.TideSectionHeader
import com.github.tidetunes.core.presentation.components.TideContextMenu
import com.github.tidetunes.core.presentation.components.TideContextMenuItem
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TideTrackNumberBadge
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.components.customAnchoredDraggable
import com.github.tidetunes.core.presentation.components.rememberCustomAnchoredDraggableState
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.cover_default_playlist_image
import tidetunes.feature.playlist.generated.resources.icon_back
import tidetunes.feature.playlist.generated.resources.icon_deleteseep
import tidetunes.feature.playlist.generated.resources.icon_download
import tidetunes.feature.playlist.generated.resources.icon_vertialcal_more
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_edit
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_import
import tidetunes.feature.playlist.generated.resources.playlist_context_menu_remove
import tidetunes.feature.playlist.generated.resources.playlist_empty_list
import tidetunes.feature.playlist.generated.resources.playlist_list_count_suffix
import tidetunes.feature.playlist.generated.resources.playlist_list_count_suffixes
import tidetunes.feature.playlist.generated.resources.playlist_remove_dialog_text
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TrackListStartIndex = 2

@Composable
fun PlaylistScreen(
    state: PlaylistState,
    currentPlayingTrackId: Long?,
    scaffoldPadding: PaddingValues,
    onAction: (PlaylistAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
        PlaylistContent(state = state, currentPlayingTrackId = currentPlayingTrackId, scaffoldPadding = scaffoldPadding, onAction = onAction)
    }
    RemovePlaylistDialog(state = state, onAction = onAction)
}

@Composable
private fun PlaylistContent(
    state: PlaylistState,
    currentPlayingTrackId: Long?,
    scaffoldPadding: PaddingValues,
    onAction: (PlaylistAction) -> Unit,
) {
    var swipingTrackId by remember { mutableStateOf<Long?>(null) }
    val spacing = TideTunesTokens.spacing
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
        onAction(PlaylistAction.MoveTrack(fromIndex = from.index - TrackListStartIndex, toIndex = to.index - TrackListStartIndex))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 18.dp),
        ) {
            item { PlaylistHeader(state = state, onAction = onAction) }
            if (state.tracks.isEmpty()) {
                item { EmptyPlaylist(modifier = Modifier.heightIn(min = 280.dp)) }
            } else {
                item {
                    TideSectionHeader(title = "Tracks", metadata = "${state.tracks.size} ${playlistCountLabel(state.tracks.size)}", titleWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                }
                items(state.tracks.size, key = { state.tracks[it].lazyListKey(it) }) {
                    val item = state.tracks[it]
                    val playing = item.id == currentPlayingTrackId
                    val itemKey = item.lazyListKey(it)
                    ReorderableItem(reorderableLazyListState, key = itemKey) { _ ->
                        PlaylistItem(item = item, index = it, playing = playing, currentSwipingTrackId = swipingTrackId, onSwipe = { swipingTrackId = item.id }, onAction = { action ->
                            if (action is PlaylistAction.RemoveTrack && swipingTrackId == item.id) swipingTrackId = null
                            onAction(action)
                        })
                    }
                }
            }
            item { BottomBarSpacer(hasCurrentMusic = currentPlayingTrackId != null, scaffoldPadding = scaffoldPadding) }
        }
    }
}

@Composable
private fun RemovePlaylistDialog(state: PlaylistState, onAction: (PlaylistAction) -> Unit) {
    ConfirmDialog(open = state.isRemoveDialogOpen, onConfirm = { onAction(PlaylistAction.ConfirmRemovePlaylist) }, onCancel = { onAction(PlaylistAction.CloseRemoveDialog) }) {
        Text(text = "${stringResource(Res.string.playlist_remove_dialog_text)} \"${state.title}\"")
    }
}

@Composable
private fun PlaylistHeader(state: PlaylistState, onAction: (PlaylistAction) -> Unit) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val shapes = TideTunesTokens.shapes

    TideDetailHeaderSurface(contentPadding = PaddingValues(16.dp), surfaceAlpha = 0.94f, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TideIconButton(size = TideIconButtonSize.Medium, variant = TideIconButtonVariant.Default, painter = painterResource(Res.drawable.icon_back), onClick = { onAction(PlaylistAction.NavigateBack) })
            Box {
                TideIconButton(size = TideIconButtonSize.Medium, variant = TideIconButtonVariant.Default, painter = painterResource(Res.drawable.icon_vertialcal_more), onClick = { moreMenuExpanded = true })
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.offset(20.dp, 20.dp)) {
                    TideContextMenu(expanded = moreMenuExpanded, onDismissRequest = { moreMenuExpanded = false }, items = listOf(
                        TideContextMenuItem(label = Res.string.playlist_context_menu_import, onClick = { moreMenuExpanded = false; onAction(PlaylistAction.ImportTracks) }),
                        TideContextMenuItem(label = Res.string.playlist_context_menu_edit, onClick = { moreMenuExpanded = false; onAction(PlaylistAction.EditPlaylist) }),
                        TideContextMenuItem(label = Res.string.playlist_context_menu_remove, isError = true, onClick = { moreMenuExpanded = false; onAction(PlaylistAction.OpenRemoveDialog) }),
                    ))
                }
            }
        }
        Box(modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(shapes.lg)).background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
            if (state.cover == null) {
                Image(modifier = Modifier.fillMaxSize(), painter = painterResource(Res.drawable.cover_default_playlist_image), contentDescription = null, contentScale = ContentScale.Crop)
            } else {
                ArtworkImage(modifier = Modifier.fillMaxSize(), artwork = state.cover)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = state.title.ifBlank { "Playlist" }, style = MiuixTheme.textStyles.title2, color = MiuixTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis, maxLines = 2, modifier = Modifier.widthIn(max = 620.dp))
            Text(text = "${state.tracks.size} ${playlistCountLabel(state.tracks.size)}, ${state.durationLabel}", color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.footnote1, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(modifier = Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TideTextButton(text = "Play All", variant = TideTextButtonVariant.PrimaryFilled, size = TideTextButtonSize.Medium, enabled = !state.tracks.isEmpty(), onClick = { onAction(PlaylistAction.PlayAll) })
            TideTextButton(text = "Import", variant = TideTextButtonVariant.Default, size = TideTextButtonSize.Medium, onClick = { onAction(PlaylistAction.ImportTracks) })
            TideTextButton(text = "Edit", variant = TideTextButtonVariant.Default, size = TideTextButtonSize.Medium, onClick = { onAction(PlaylistAction.EditPlaylist) })
        }
    }
}

@Composable
private fun EmptyPlaylist(modifier: Modifier = Modifier) {
    TideCardSurface(modifier = modifier, cornerRadius = TideTunesTokens.shapes.lg, contentPadding = PaddingValues(24.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(Res.string.playlist_empty_list), color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.body1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PlaylistItem(
    item: PlaylistTrackItem, index: Int, playing: Boolean, currentSwipingTrackId: Long?, onSwipe: () -> Unit, onAction: (PlaylistAction) -> Unit,
) {
    val density = LocalDensity.current
    val panelWidthDp = 48.dp
    val rowShape = RoundedCornerShape(TideTunesTokens.shapes.lg)
    val anchoredDraggableState = with(density) {
        rememberCustomAnchoredDraggableState(initialValue = 0f, anchors = mapOf(0.dp.toPx() to "START", -(panelWidthDp * 2 + 8.dp).toPx() to "END"), animationSpec = tween(200))
    }
    val textColor = if (playing) TideTunesBrand.Primary else MiuixTheme.colorScheme.onSurface
    val rowBackground = if (playing) MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f) else MiuixTheme.colorScheme.surfaceContainer
    val borderColor = if (playing) TideTunesBrand.Primary.copy(alpha = 0.34f) else MiuixTheme.colorScheme.outline
    LaunchedEffect(currentSwipingTrackId) { if (currentSwipingTrackId != item.id) anchoredDraggableState.animateTo(0f) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = with(density) { anchoredDraggableState.value.toDp() }).clip(rowShape).background(rowBackground).border(1.dp, borderColor, rowShape)
                .customAnchoredDraggable(state = anchoredDraggableState, orientation = Orientation.Horizontal, onDragStarted = { onSwipe() })
                .clickable { onAction(PlaylistAction.PlayTrack(item.id)); onSwipe() }
                .padding(horizontal = 14.dp, vertical = 12.dp).heightIn(min = 64.dp).fillMaxWidth(),
        ) {
            TideTrackNumberBadge(label = (index + 1).toString().padStart(2, '0'), active = playing, modifier = Modifier.draggableHandle())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (playing) { Text(text = "Now Playing", color = TideTunesBrand.Primary, style = MiuixTheme.textStyles.footnote1, fontWeight = FontWeight.Bold, maxLines = 1) }
                Text(text = item.title, color = textColor, style = MiuixTheme.textStyles.body1, fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = item.durationLabel, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, maxLines = 1, modifier = Modifier.wrapContentWidth(), style = MiuixTheme.textStyles.footnote1)
        }
        Box(modifier = Modifier.clipToBounds().fillMaxSize().align(alignment = Alignment.CenterEnd)) {
            Row(modifier = Modifier.offset(x = panelWidthDp + with(density) { anchoredDraggableState.value.toDp() }).fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(8.dp))
                TideIconButton(size = TideIconButtonSize.Medium, variant = TideIconButtonVariant.Default, painter = painterResource(Res.drawable.icon_download), onClick = { onAction(PlaylistAction.DownloadTrack(item)) })
                Box(modifier = Modifier.width(8.dp))
                TideIconButton(size = TideIconButtonSize.Medium, variant = TideIconButtonVariant.ErrorFilled, painter = painterResource(Res.drawable.icon_deleteseep), onClick = { onAction(PlaylistAction.RemoveTrack(item.id)) })
            }
        }
    }
}

internal fun PlaylistTrackItem.lazyListKey(index: Int): String = "playlist-track-$sortOrder-$index-$id"

@Composable
private fun playlistCountLabel(count: Int): String {
    val countSuffixStringRes = if (count <= 1) Res.string.playlist_list_count_suffix else Res.string.playlist_list_count_suffixes
    return stringResource(countSuffixStringRes)
}

package com.github.tidetunes.feature.playlist.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.tidetunes.core.presentation.components.TideTunesIconButton
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonSize
import com.github.tidetunes.core.presentation.components.TideTunesIconButtonType
import org.jetbrains.compose.resources.painterResource
import com.github.tidetunes.core.presentation.media.ArtworkImage
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ScrollMoveMode
import sh.calvin.reorderable.rememberReorderableLazyGridState
import tidetunes.feature.playlist.generated.resources.Res
import tidetunes.feature.playlist.generated.resources.cover_default_image
import tidetunes.feature.playlist.generated.resources.empty_playlists
import tidetunes.feature.playlist.generated.resources.icon_adjust
import tidetunes.feature.playlist.generated.resources.icon_drag
import tidetunes.feature.playlist.generated.resources.icon_plus
import tidetunes.feature.playlist.generated.resources.icon_yes
import tidetunes.feature.playlist.generated.resources.music_count_unit
import tidetunes.feature.playlist.generated.resources.playlist_empty

@Composable
fun PlaylistsListScreen(
    state: PlaylistsListState,
    onAction: (PlaylistsListAction) -> Unit,
) {
    if (state.isEmpty) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onAction(PlaylistsListAction.CreatePlaylist) }
                    .clip(RoundedCornerShape(16.dp))
                    .padding(24.dp, 24.dp),
            ) {
                Image(painter = painterResource(Res.drawable.empty_playlists), contentDescription = null)
                Box(modifier = Modifier.height(20.dp))
                Text(text = stringResource(Res.string.playlist_empty))
            }
        }
    } else {
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .padding(24.dp, 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_adjust),
                        disabled = state.mode == PlaylistsListMode.Adjust,
                        onClick = { onAction(PlaylistsListAction.ToggleMode) },
                    )
                    TideTunesIconButton(
                        sizeType = TideTunesIconButtonSize.Medium,
                        buttonType = TideTunesIconButtonType.Default,
                        painter = painterResource(Res.drawable.icon_plus),
                        disabled = state.mode == PlaylistsListMode.Adjust,
                        onClick = { onAction(PlaylistsListAction.CreatePlaylist) },
                    )
                }
                GridPlaylists(
                    playlists = state.playlists,
                    mode = state.mode,
                    onAction = onAction,
                )
            }
            if (state.mode == PlaylistsListMode.Adjust) {
                FloatingActionButton(
                    containerColor = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp),
                    onClick = { onAction(PlaylistsListAction.SetModeNormal) },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_yes),
                        tint = Color.White,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridPlaylists(
    playlists: List<PlaylistListItem>,
    mode: PlaylistsListMode,
    onAction: (PlaylistsListAction) -> Unit,
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyListState = rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        scrollMoveMode = ScrollMoveMode.INSERT,
    ) { from, to ->
        onAction(PlaylistsListAction.MovePlaylist(from.index, to.index))
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.FixedSize(172.dp),
        horizontalArrangement = Arrangement.Center,
        state = lazyGridState,
    ) {
        itemsIndexed(playlists, key = { index, playlist -> playlist.lazyListKey(index) }) { index, playlist ->
            val playlistKey = playlist.lazyListKey(index)
            ReorderableItem(reorderableLazyListState, key = playlistKey) { _ ->
                PlaylistItem(playlist = playlist, mode = mode, onAction = onAction)
            }
        }
    }
}

internal fun PlaylistListItem.lazyListKey(index: Int): String =
    "playlist-list-$index-$id"

@Composable
private fun ReorderableCollectionItemScope.PlaylistItem(
    playlist: PlaylistListItem,
    mode: PlaylistsListMode,
    onAction: (PlaylistsListAction) -> Unit,
) {
    Box(
        Modifier.then(
            if (mode == PlaylistsListMode.Adjust) {
                Modifier.draggableHandle()
            } else {
                Modifier.clickable { onAction(PlaylistsListAction.NavigateToPlaylist(playlist.id)) }
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    .size(136.dp),
            ) {
                if (playlist.cover == null) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(Res.drawable.cover_default_image),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                } else {
                    ArtworkImage(
                        modifier = Modifier.fillMaxSize(),
                        artwork = playlist.cover,
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = playlist.title,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${playlist.musicCount} ${stringResource(Res.string.music_count_unit)}  ·  ${playlist.durationLabel}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
            )
        }
        if (mode == PlaylistsListMode.Adjust) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MiuixTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(Res.drawable.icon_drag),
                    tint = Color.White,
                    contentDescription = null,
                )
            }
        }
    }
}

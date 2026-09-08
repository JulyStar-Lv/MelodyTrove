package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import io.github.julystar.musicapp.car.presentation.component.CarSongRow
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.search.SearchRepository
import io.github.julystar.musicapp.core.domain.search.SearchTrackItem
import io.github.julystar.musicapp.service.playback.domain.PlayableItem
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CarSearchScreen(
    metrics: CarLayoutMetrics,
    repository: SearchRepository,
    playbackController: PlaybackController,
    currentTrackId: Long?,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchTrackItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(query) {
        error = null
        val normalized = query.trim()
        if (normalized.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        runCatching { repository.searchLocalLibrary(normalized).tracks }
            .onSuccess { results = it }
            .onFailure { error = it.message ?: "搜索失败" }
    }
    val playable = results.mapNotNull(SearchTrackItem::toPlayableItemOrNull)
    Column(
        verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.section),
        modifier = modifier.padding(
            start = metrics.contentHorizontalPadding,
            top = metrics.contentTop,
            end = metrics.contentHorizontalPadding,
        ),
    ) {
        CarSectionTitle("搜索音乐")
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = LocalCarTypography.current.bodyLarge.copy(color = LocalCarColors.current.textPrimary),
            cursorBrush = SolidColor(LocalCarColors.current.accentPrimary),
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = LocalCarSpacing.current.section)) {
                    if (query.isBlank()) {
                        BasicText("输入歌曲、专辑或歌手", style = LocalCarTypography.current.bodyLarge.copy(color = LocalCarColors.current.textSummary))
                    }
                    inner()
                }
            },
            modifier = Modifier
                .carFocusTarget(CarFocusIds.SearchField, left = CarFocusIds.Home)
                .fillMaxWidth()
                .height(metrics.navigationItemHeight)
                .clip(LocalCarShapes.current.navigationItem)
                .background(LocalCarColors.current.panel),
        )
        when {
            error != null -> CarPageState(error.orEmpty(), Modifier.weight(1f))
            query.isNotBlank() && results.isEmpty() -> CarPageState("没有找到结果", Modifier.weight(1f))
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
                contentPadding = PaddingValues(bottom = LocalCarSpacing.current.wide),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(results, key = { index, item -> item.id?.toString() ?: "${item.sourceLabel}:${item.title}:$index" }) { index, item ->
                    val row = item.toLibraryTrackItem(index)
                    CarSongRow(
                        track = row,
                        index = index,
                        playing = item.id != null && currentTrackId == item.id,
                        height = metrics.compactCardHeight,
                        enabled = item.toPlayableItemOrNull() != null,
                        onClick = {
                            val selected = item.toPlayableItemOrNull() ?: return@CarSongRow
                            val startIndex = playable.indexOf(selected)
                            if (startIndex >= 0) scope.launch { playbackController.play(playable, startIndex) }
                        },
                        modifier = Modifier
                            .carFocusTarget(
                                CarFocusIds.item("search_result", item.id ?: "$index:${item.title}"),
                                up = if (index == 0) CarFocusIds.SearchField else null,
                                left = CarFocusIds.Home,
                            )
                            .height(metrics.compactCardHeight),
                    )
                }
            }
        }
    }
}

private fun SearchTrackItem.toPlayableItemOrNull(): PlayableItem? {
    if (mediaId == null && id == null) return null
    return PlayableItem(
        mediaId = mediaId,
        title = title,
        artist = artist,
        durationMs = durationMs,
        libraryTrackId = id,
    )
}

private fun SearchTrackItem.toLibraryTrackItem(index: Int) = LibraryTrackItem(
    id = id ?: -(index + 1L),
    title = title,
    artist = artist,
    durationMs = durationMs,
    mediaId = mediaId,
)

package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.julystar.musicapp.car.presentation.component.CarAlbumCard
import io.github.julystar.musicapp.car.presentation.component.CarArtistCard
import io.github.julystar.musicapp.car.presentation.component.CarArtwork
import io.github.julystar.musicapp.car.presentation.component.CarSongRow
import io.github.julystar.musicapp.car.presentation.component.carInteractiveSurface
import io.github.julystar.musicapp.car.presentation.focus.CarFocusIds
import io.github.julystar.musicapp.car.presentation.focus.carFocusTarget
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutMetrics
import io.github.julystar.musicapp.car.presentation.layout.CarLayoutProfile
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTouchTargets
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.LIBRARY_PLAYBACK_PLAYLIST_ID
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
import io.github.julystar.musicapp.core.domain.model.RepositoryState
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository
import io.github.julystar.musicapp.core.domain.repository.FavoritesRepository
import io.github.julystar.musicapp.core.domain.repository.PlaylistRepository
import io.github.julystar.musicapp.service.playback.domain.PlaybackController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CarSongsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    tracks: List<LibraryTrackItem>,
    currentTrackId: Long?,
    artworkRepository: ArtworkRepository,
    playbackController: PlaybackController,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入歌曲…", modifier)
    val favoritesRepository = koinInject<FavoritesRepository>()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Recent) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.RecentAdded) }
    val visibleTracks = remember(tracks, favoriteTrackIds, filter, sort) {
        val filtered = when (filter) {
            LibraryFilter.RecentAdded -> tracks
            LibraryFilter.RecentlyPlayed -> tracks.filter { it.lastPlayedAt != null }
            LibraryFilter.Favorites -> tracks.filter { it.id in favoriteTrackIds }
            LibraryFilter.HiRes -> tracks.filter(LibraryTrackItem::isHiRes)
        }
        when (sort) {
            LibrarySort.Recent -> when (filter) {
                LibraryFilter.RecentlyPlayed -> filtered.sortedByDescending { it.lastPlayedAt }
                else -> filtered.sortedByDescending { it.createdAt }
            }
            LibrarySort.Name -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.Artist -> filtered.sortedBy { it.artist.orEmpty().lowercase() }
        }
    }
    LibraryScaffold(
        metrics, LibraryTab.Songs, tracks.size, sort, { sort = it }, filter,
        mapOf(
            LibraryFilter.RecentAdded to tracks.size,
            LibraryFilter.RecentlyPlayed to tracks.count { it.lastPlayedAt != null },
            LibraryFilter.Favorites to tracks.count { it.id in favoriteTrackIds },
            LibraryFilter.HiRes to tracks.count(LibraryTrackItem::isHiRes),
        ),
        { filter = it },
        { if (it == LibraryTab.Albums) onOpenAlbums() else if (it == LibraryTab.Artists) onOpenArtists() },
        {
            if (visibleTracks.isNotEmpty()) {
                val request = visibleTracks.toCarPlaybackRequest(0)
                scope.launch { playbackController.play(request.items, request.startIndex) }
            }
        },
        modifier,
    ) {
        if (visibleTracks.isEmpty()) CarPageState("音乐库中没有歌曲", Modifier.fillMaxSize())
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(visibleTracks, key = { _, track -> track.id }) { index, track ->
                CarSongRow(
                    track = track,
                    index = index,
                    playing = currentTrackId == track.id,
                    height = metrics.libraryRowHeight,
                    artworkRepository = artworkRepository,
                    showArtwork = true,
                    showAlbum = true,
                    showActions = true,
                    favorite = track.id in favoriteTrackIds,
                    onToggleFavorite = {
                        scope.launch { favoritesRepository.toggleFavorite(track.id) }
                    },
                    onClick = {
                        val request = visibleTracks.toCarPlaybackRequest(index)
                        scope.launch { playbackController.play(request.items, request.startIndex) }
                    },
                    modifier = Modifier
                        .carFocusTarget(
                            if (index == 0) CarFocusIds.content("Songs") else CarFocusIds.item("song", track.id),
                            left = CarFocusIds.Songs,
                        )
                        .height(metrics.libraryRowHeight),
                )
            }
        }
    }
}

@Composable
fun CarAlbumsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    albums: List<LibraryAlbumItem>,
    tracks: List<LibraryTrackItem>,
    artworkRepository: ArtworkRepository,
    onOpenSongs: () -> Unit,
    onOpenArtists: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入专辑…", modifier)
    val playbackController = koinInject<PlaybackController>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Recent) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.RecentAdded) }
    val albumIdsByFilter = remember(tracks, favoriteTrackIds) {
        mapOf(
            LibraryFilter.RecentlyPlayed to tracks.filter { it.lastPlayedAt != null }.mapNotNull { it.albumId }.toSet(),
            LibraryFilter.Favorites to tracks.filter { it.id in favoriteTrackIds }.mapNotNull { it.albumId }.toSet(),
            LibraryFilter.HiRes to tracks.filter(LibraryTrackItem::isHiRes).mapNotNull { it.albumId }.toSet(),
        )
    }
    val visibleAlbums = remember(albums, albumIdsByFilter, filter, sort) {
        val filtered = if (filter == LibraryFilter.RecentAdded) albums else {
            val ids = albumIdsByFilter[filter].orEmpty()
            albums.filter { it.id in ids }
        }
        when (sort) {
            LibrarySort.Recent -> filtered
            LibrarySort.Name -> filtered.sortedBy { it.name.lowercase() }
            LibrarySort.Artist -> filtered.sortedByDescending { it.year ?: Int.MIN_VALUE }
        }
    }
    val scope = rememberCoroutineScope()
    val visibleAlbumIds = remember(visibleAlbums) { visibleAlbums.mapTo(mutableSetOf()) { it.id } }
    val shuffledTracks = remember(tracks, visibleAlbumIds) {
        tracks.filter { track -> track.albumId?.let(visibleAlbumIds::contains) == true }.shuffled()
    }
    LibraryScaffold(
        metrics, LibraryTab.Albums, albums.size, sort, { sort = it }, filter,
        mapOf(
            LibraryFilter.RecentAdded to albums.size,
            LibraryFilter.RecentlyPlayed to albumIdsByFilter[LibraryFilter.RecentlyPlayed].orEmpty().size,
            LibraryFilter.Favorites to albumIdsByFilter[LibraryFilter.Favorites].orEmpty().size,
            LibraryFilter.HiRes to albumIdsByFilter[LibraryFilter.HiRes].orEmpty().size,
        ),
        { filter = it },
        { if (it == LibraryTab.Songs) onOpenSongs() else if (it == LibraryTab.Artists) onOpenArtists() },
        {
            if (shuffledTracks.isNotEmpty()) {
                val request = shuffledTracks.toCarPlaybackRequest(0)
                scope.launch { playbackController.play(request.items, request.startIndex) }
            }
        },
        modifier,
    ) {
        if (visibleAlbums.isEmpty()) CarPageState("音乐库中没有专辑", Modifier.fillMaxSize())
        else LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.mediaGridColumns),
            contentPadding = PaddingValues(
                start = metrics.mediaGridPadding,
                top = LocalCarSpacing.current.pane,
                end = metrics.mediaGridPadding,
                bottom = LocalCarSpacing.current.pane,
            ),
            horizontalArrangement = Arrangement.spacedBy(metrics.mediaGridHorizontalGap),
            verticalArrangement = Arrangement.spacedBy(metrics.libraryGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItemsIndexed(visibleAlbums, key = { _, album -> album.id }) { index, album ->
                CarAlbumCard(
                    album,
                    artworkRepository,
                    metrics.recommendationCardWidth - if (metrics.profile == CarLayoutProfile.Expanded) 32.dp else 24.dp,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            if (index == 0) CarFocusIds.content("Albums") else CarFocusIds.item("album", album.id),
                            left = CarFocusIds.Albums,
                        )
                        .height(metrics.recommendationCardHeight),
                )
            }
        }
    }
}

@Composable
fun CarArtistsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    error: String?,
    artists: List<LibraryArtistItem>,
    albums: List<LibraryAlbumItem>,
    tracks: List<LibraryTrackItem>,
    artworkRepository: ArtworkRepository,
    onOpenSongs: () -> Unit,
    onOpenAlbums: () -> Unit,
    onArtistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (error != null) return CarPageState("音乐库载入失败：$error", modifier)
    if (loading) return CarPageState("正在载入歌手…", modifier)
    val playbackController = koinInject<PlaybackController>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Recent) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.RecentlyPlayed) }
    val artistNamesByFilter = remember(tracks, favoriteTrackIds) {
        mapOf(
            LibraryFilter.RecentlyPlayed to tracks.filter { it.lastPlayedAt != null }.mapNotNull { it.artist }.toSet(),
            LibraryFilter.Favorites to tracks.filter { it.id in favoriteTrackIds }.mapNotNull { it.artist }.toSet(),
            LibraryFilter.HiRes to tracks.filter(LibraryTrackItem::isHiRes).mapNotNull { it.artist }.toSet(),
        )
    }
    val artistTrackCounts = remember(tracks) { tracks.groupingBy { it.artist.orEmpty() }.eachCount() }
    val visibleArtists = remember(artists, artistNamesByFilter, artistTrackCounts, filter, sort) {
        val filtered = if (filter == LibraryFilter.RecentAdded) artists else {
            val names = artistNamesByFilter[filter].orEmpty()
            artists.filter { it.name in names }
        }
        when (sort) {
            LibrarySort.Recent -> filtered
            LibrarySort.Name -> filtered.sortedBy { it.name.lowercase() }
            LibrarySort.Artist -> filtered.sortedByDescending { artistTrackCounts[it.name] ?: 0 }
        }
    }
    val scope = rememberCoroutineScope()
    val visibleArtistNames = remember(visibleArtists) { visibleArtists.mapTo(mutableSetOf()) { it.name } }
    val shuffledTracks = remember(tracks, visibleArtistNames) {
        tracks.filter { track -> track.artist?.let(visibleArtistNames::contains) == true }.shuffled()
    }
    LibraryScaffold(
        metrics, LibraryTab.Artists, artists.size, sort, { sort = it }, filter,
        mapOf(
            LibraryFilter.RecentAdded to artists.size,
            LibraryFilter.RecentlyPlayed to artistNamesByFilter[LibraryFilter.RecentlyPlayed].orEmpty().size,
            LibraryFilter.Favorites to artistNamesByFilter[LibraryFilter.Favorites].orEmpty().size,
            LibraryFilter.HiRes to artistNamesByFilter[LibraryFilter.HiRes].orEmpty().size,
        ),
        { filter = it },
        { if (it == LibraryTab.Songs) onOpenSongs() else if (it == LibraryTab.Albums) onOpenAlbums() },
        {
            if (shuffledTracks.isNotEmpty()) {
                val request = shuffledTracks.toCarPlaybackRequest(0)
                scope.launch { playbackController.play(request.items, request.startIndex) }
            }
        },
        modifier,
    ) {
        if (visibleArtists.isEmpty()) CarPageState("音乐库中没有歌手", Modifier.fillMaxSize())
        else LazyVerticalGrid(
            columns = GridCells.Fixed(metrics.mediaGridColumns),
            contentPadding = PaddingValues(
                start = metrics.mediaGridPadding,
                top = LocalCarSpacing.current.pane,
                end = metrics.mediaGridPadding,
                bottom = LocalCarSpacing.current.pane,
            ),
            horizontalArrangement = Arrangement.spacedBy(metrics.mediaGridHorizontalGap),
            verticalArrangement = Arrangement.spacedBy(metrics.libraryGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItemsIndexed(visibleArtists, key = { _, artist -> artist.id }) { index, artist ->
                CarArtistCard(
                    artist,
                    albums.firstOrNull { it.artist == artist.name }?.let { Artwork.LibraryAlbum(it.id) },
                    artworkRepository,
                    metrics.recommendationCardWidth - 72.dp,
                    onClick = { onArtistClick(artist.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            if (index == 0) CarFocusIds.content("Artists") else CarFocusIds.item("artist", artist.id),
                            left = CarFocusIds.Artists,
                        )
                        .height(metrics.artistCardHeight),
                )
            }
        }
    }
}

@Composable
fun CarPlaylistsScreen(
    metrics: CarLayoutMetrics,
    loading: Boolean,
    playlists: List<PlaylistSummary>,
    modifier: Modifier = Modifier,
) {
    if (loading) return CarPageState("正在载入歌单…", modifier)
    val repository = koinInject<PlaylistRepository>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val playbackController = koinInject<PlaybackController>()
    val artworkRepository = koinInject<ArtworkRepository>()
    val scope = rememberCoroutineScope()
    val favoriteTracksState by favoritesRepository.favoriteTracks().collectAsState(RepositoryState.Loading)
    val favoriteTracks = favoriteTracksState.dataOrNull.orEmpty()
    val favoritePlaylist = remember(favoriteTracks) {
        PlaylistSummary(
            id = LIBRARY_PLAYBACK_PLAYLIST_ID,
            title = "我的收藏",
            musicCount = favoriteTracks.size.toLong(),
            durationMs = favoriteTracks.sumOf { it.durationMs ?: 0L },
            coverArtwork = favoriteTracks.firstOrNull()?.let { track ->
                track.albumId?.let(Artwork::LibraryAlbum) ?: Artwork.LibraryCover(track.id)
            },
        )
    }
    val visiblePlaylists = remember(playlists, favoritePlaylist) {
        listOf(favoritePlaylist) + playlists.filterNot { it.id == LIBRARY_PLAYBACK_PLAYLIST_ID }
    }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(LIBRARY_PLAYBACK_PLAYLIST_ID) }
    LaunchedEffect(visiblePlaylists) {
        if (visiblePlaylists.none { it.id == selectedId }) selectedId = LIBRARY_PLAYBACK_PLAYLIST_ID
    }
    val selected = visiblePlaylists.firstOrNull { it.id == selectedId }
    val selectedTracks by produceState(emptyList<LibraryTrackItem>(), selectedId, favoriteTracks) {
        val id = selectedId ?: return@produceState
        if (id == LIBRARY_PLAYBACK_PLAYLIST_ID) {
            value = favoriteTracks
        } else {
            repository.observePlaylistTracks(id).collect { rows ->
                value = rows.map(DomainPlaylistTrack::toCarLibraryTrackItem)
            }
        }
    }
    val tracks = selectedTracks
    val queue = if (tracks.isEmpty()) null else tracks.toCarPlaybackRequest(0, selectedId)
    val otherPlaylists = remember(visiblePlaylists, selectedId) { visiblePlaylists.filterNot { it.id == selectedId } }
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier.fillMaxSize().padding(
            start = metrics.detailContentMargin,
            top = metrics.contentTop,
            end = metrics.detailContentEndMargin,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        Column(
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(LocalCarColors.current.backgroundSubtle).padding(metrics.libraryPanePadding),
        ) {
            BasicText("所有歌单", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            if (selected != null) {
                SelectedPlaylistCard(
                    playlist = selected,
                    artworkRepository = artworkRepository,
                    onClick = {},
                    modifier = Modifier
                        .carFocusTarget(CarFocusIds.content("Playlists"), left = CarFocusIds.Playlists)
                        .fillMaxWidth()
                        .height(metrics.playlistSelectedCardHeight),
                )
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(metrics.playlistGridColumns),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (metrics.profile == CarLayoutProfile.Expanded) 16.dp else 0.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    gridItemsIndexed(otherPlaylists, key = { _, item -> item.id }) { _, item ->
                        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth()) {
                            PlaylistThumbnailCard(
                                playlist = item,
                                artworkRepository = artworkRepository,
                                artworkSize = metrics.playlistArtworkSize,
                                onClick = { selectedId = item.id },
                                modifier = Modifier
                                    .carFocusTarget(CarFocusIds.item("playlist", item.id), left = CarFocusIds.Playlists)
                                    .width(metrics.playlistCardWidth)
                                    .height(metrics.playlistArtworkSize + 76.dp),
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(LocalCarColors.current.backgroundSubtle).padding(metrics.playlistContentPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    BasicText(selected?.title ?: "歌单", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
                    BasicText("${tracks.size} 首歌曲", style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary))
                }
                PlaylistHeaderAction("播放全部", CarIcon.Play, queue != null) {
                    queue?.let { request -> scope.launch { playbackController.play(request.items, request.startIndex) } }
                }
                Spacer(Modifier.width(LocalCarSpacing.current.small))
                PlaylistHeaderAction("编辑", CarIcon.Edit, enabled = false) {}
            }
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            if (selected == null) CarPageState("还没有歌单", Modifier.fillMaxSize())
            else if (tracks.isEmpty()) CarPageState("这个歌单还没有歌曲", Modifier.fillMaxSize())
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                    PlaylistTrackRow(
                        track = track,
                        index = index,
                        height = metrics.libraryRowHeight,
                        artworkRepository = artworkRepository,
                        onEnqueueNext = {
                            queue?.items?.getOrNull(index)?.let(playbackController::enqueueNext)
                        },
                        onClick = {
                            val request = tracks.toCarPlaybackRequest(index, selectedId)
                            scope.launch { playbackController.play(request.items, request.startIndex) }
                        },
                        modifier = Modifier.height(metrics.libraryRowHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScaffold(
    metrics: CarLayoutMetrics,
    selectedTab: LibraryTab,
    itemCount: Int,
    sort: LibrarySort,
    onSort: (LibrarySort) -> Unit,
    filter: LibraryFilter,
    filterCounts: Map<LibraryFilter, Int>,
    onFilter: (LibraryFilter) -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onPlayAll: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalCarColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier.fillMaxSize().padding(
            start = metrics.detailContentMargin,
            top = metrics.contentTop,
            end = metrics.detailContentEndMargin,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        Column(
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle).padding(metrics.libraryPanePadding),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.panel, RoundedCornerShape(16.dp))
                    .padding(4.dp),
            ) {
                LibraryTab.entries.forEach { tab ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .carInteractiveSurface(
                                RoundedCornerShape(12.dp),
                                defaultColor = if (tab == selectedTab) colors.surfacePressed else Color.Transparent,
                                onClick = { onSelectTab(tab) },
                            ),
                    ) {
                        BasicText(
                            tab.label,
                            style = LocalCarTypography.current.body.copy(
                                color = if (tab == selectedTab) colors.accentPrimary else colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(LocalCarSpacing.current.content))
            BasicText("$itemCount ${selectedTab.unit}", style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(LocalCarSpacing.current.content))
            LibraryActionButton(
                if (selectedTab == LibraryTab.Songs) "播放全部" else "随机播放",
                if (selectedTab == LibraryTab.Songs) CarIcon.Play else CarIcon.Shuffle,
                itemCount > 0,
                onPlayAll ?: {},
            )
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            BasicText("快速筛选", style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            val filterOrder = if (selectedTab == LibraryTab.Artists) {
                listOf(LibraryFilter.RecentlyPlayed, LibraryFilter.RecentAdded, LibraryFilter.Favorites, LibraryFilter.HiRes)
            } else {
                LibraryFilter.entries
            }
            filterOrder.forEach { item ->
                val label = if (selectedTab == LibraryTab.Artists && item == LibraryFilter.HiRes) "Hi-Res 作品" else item.label
                FilterRow(label, filterCounts[item].orEmptyCount(), filter == item) { onFilter(item) }
            }
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            BasicText("排序方式", style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            FilterRow(
                if (filter == LibraryFilter.RecentlyPlayed) "最近播放" else "最近添加",
                if (sort == LibrarySort.Recent) "当前" else "",
                sort == LibrarySort.Recent,
            ) { onSort(LibrarySort.Recent) }
            FilterRow("${selectedTab.label}名称", "A–Z", sort == LibrarySort.Name) { onSort(LibrarySort.Name) }
            val tertiarySortTitle = when (selectedTab) {
                LibraryTab.Songs -> "歌手名称"
                LibraryTab.Albums -> "发行年份"
                LibraryTab.Artists -> "歌曲数量"
            }
            val tertiarySortValue = when (selectedTab) {
                LibraryTab.Songs -> "A–Z"
                LibraryTab.Albums -> "新→旧"
                LibraryTab.Artists -> "多→少"
            }
            FilterRow(tertiarySortTitle, tertiarySortValue, sort == LibrarySort.Artist) { onSort(LibrarySort.Artist) }
            Spacer(Modifier.weight(1f))
            BasicText(
                "筛选和排序仅作用于右侧内容",
                style = LocalCarTypography.current.supporting.copy(color = colors.textSummary),
            )
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle),
        ) { content() }
    }
}

@Composable
private fun FilterRow(
    title: String,
    value: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalCarColors.current
    val textColor = when {
        !enabled -> colors.textDisabled
        selected -> colors.accentPrimary
        else -> colors.textPrimary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(56.dp).carInteractiveSurface(
            LocalCarShapes.current.control,
            selected = selected,
            enabled = enabled,
            defaultColor = colors.panel,
            onClick = onClick,
        ).padding(horizontal = LocalCarSpacing.current.content),
    ) {
        BasicText(title, style = LocalCarTypography.current.body.copy(color = textColor), modifier = Modifier.weight(1f))
        BasicText(
            value,
            style = LocalCarTypography.current.supporting.copy(
                color = if (!enabled) colors.textDisabled else if (selected) colors.accentPrimary else colors.textSecondary,
            ),
        )
    }
}

@Composable
private fun LibraryActionButton(title: String, icon: CarIcon, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalCarColors.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(72.dp).carInteractiveSurface(
            LocalCarShapes.current.control, enabled = enabled, defaultColor = colors.surfaceContainerHigh, onClick = onClick,
        ),
    ) {
        CarIconView(icon, null, if (enabled) colors.accentPrimary else colors.textDisabled, Modifier.size(28.dp))
        Spacer(Modifier.width(LocalCarSpacing.current.small))
        BasicText(title, style = LocalCarTypography.current.body.copy(color = if (enabled) colors.accentPrimary else colors.textDisabled, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun SelectedPlaylistCard(
    playlist: PlaylistSummary,
    artworkRepository: ArtworkRepository,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCarColors.current
    val cardShape = RoundedCornerShape(18.dp)
    val darkTheme = colors.backgroundBase == Color.Black
    Box(
        modifier = modifier.fillMaxWidth().carInteractiveSurface(
            cardShape,
            defaultColor = if (darkTheme) Color(0xFF1A1A1A) else colors.panel,
            onClick = onClick,
        ),
    ) {
        CarArtwork(
            artwork = playlist.coverArtwork,
            repository = artworkRepository,
            size = 104.dp,
            shape = RoundedCornerShape(28.dp),
            fillBounds = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.2f
                    scaleY = 1.6f
                    alpha = 0.72f
                }
                .blur(21.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF050505).copy(alpha = 0.18f),
                        Color(0xFF050505).copy(alpha = 0.38f),
                        Color(0xFF050505).copy(alpha = 0.58f),
                    ),
                ),
            ),
        )
        Box(
            Modifier
                .offset(x = 0.dp, y = 16.dp)
                .size(4.dp, 104.dp)
                .background(colors.accentPrimary, RoundedCornerShape(2.dp)),
        )
        CarArtwork(
            playlist.coverArtwork,
            artworkRepository,
            104.dp,
            RoundedCornerShape(14.dp),
            modifier = Modifier.offset(x = 16.dp, y = 16.dp),
        )
        Column(
            modifier = Modifier
                .offset(x = 136.dp, y = 28.dp)
                .fillMaxWidth()
                .padding(end = 160.dp),
        ) {
            BasicText(
                playlist.title,
                style = LocalCarTypography.current.bodyLarge.copy(
                    color = Color(0xFFF2F2F2),
                    fontSize = 22.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            BasicText(
                playlist.summaryLabel(),
                style = LocalCarTypography.current.supporting.copy(
                    color = Color(0xFFA8ADB3),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaylistThumbnailCard(
    playlist: PlaylistSummary,
    artworkRepository: ArtworkRepository,
    artworkSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCarColors.current
    val darkTheme = colors.backgroundBase == Color.Black
    Column(
        modifier = modifier
            .carInteractiveSurface(
                RoundedCornerShape(16.dp),
                defaultColor = if (darkTheme) Color(0xFF1E1E1E) else colors.panel,
                onClick = onClick,
            )
            .padding(12.dp),
    ) {
        CarArtwork(playlist.coverArtwork, artworkRepository, artworkSize, RoundedCornerShape(14.dp))
        Spacer(Modifier.height(8.dp))
        BasicText(
            playlist.title,
            style = LocalCarTypography.current.body.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            "${playlist.musicCount} 首歌曲",
            style = LocalCarTypography.current.supporting.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun PlaylistHeaderAction(
    title: String,
    icon: CarIcon,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalCarColors.current
    val compact = title == "编辑"
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(if (compact) 64.dp else 156.dp).height(64.dp).carInteractiveSurface(
            LocalCarShapes.current.control,
            enabled = enabled,
            defaultColor = colors.surfaceContainerHigh,
            onClick = onClick,
        ).padding(horizontal = LocalCarSpacing.current.content),
    ) {
        CarIconView(icon, null, if (enabled) colors.accentPrimary else colors.textDisabled, Modifier.size(28.dp))
        if (!compact) {
            Spacer(Modifier.width(LocalCarSpacing.current.small))
            BasicText(
                title,
                style = LocalCarTypography.current.body.copy(
                    color = if (enabled) colors.accentPrimary else colors.textDisabled,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: LibraryTrackItem,
    index: Int,
    height: androidx.compose.ui.unit.Dp,
    artworkRepository: ArtworkRepository,
    onEnqueueNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val actionSize = maxOf(LocalCarTouchTargets.current.minimum, height * 0.56f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .carInteractiveSurface(LocalCarShapes.current.navigationItem, defaultColor = colors.backgroundSubtle, onClick = onClick)
            .padding(horizontal = LocalCarSpacing.current.compact),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(44.dp)) {
            BasicText((index + 1).toString(), style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
        }
        CarArtwork(
            track.albumId?.let(Artwork::LibraryAlbum) ?: Artwork.LibraryCover(track.id),
            artworkRepository,
            height * 0.58f,
            LocalCarShapes.current.artwork,
        )
        Spacer(Modifier.width(LocalCarSpacing.current.content))
        Column(Modifier.weight(1f)) {
            BasicText(
                track.title,
                style = LocalCarTypography.current.bodyLarge.copy(color = colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                track.artist.orEmpty(),
                style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(actionSize).carInteractiveSurface(
                LocalCarShapes.current.control,
                onClick = onEnqueueNext,
            ),
        ) {
            CarIconView(CarIcon.AddToQueue, "下一首播放", colors.textSecondary, Modifier.size(actionSize * 0.57f))
        }
        Spacer(Modifier.width(LocalCarSpacing.current.small))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(actionSize)) {
            CarIconView(CarIcon.More, null, colors.textDisabled, Modifier.size(actionSize * 0.57f))
        }
    }
}

private val CarLayoutMetrics.libraryRowHeight
    get() = if (profile == CarLayoutProfile.VehiclePanel) compactCardHeight else navigationItemHeight

private fun DomainPlaylistTrack.toCarLibraryTrackItem() =
    LibraryTrackItem(trackId, title, artist, durationMs, mediaId, albumName)

private enum class LibraryTab(val label: String, val unit: String) {
    Songs("歌曲", "首歌曲"), Albums("专辑", "张专辑"), Artists("歌手", "位歌手")
}

private enum class LibrarySort { Recent, Name, Artist }

private enum class LibraryFilter(val label: String) {
    RecentAdded("最近添加"),
    RecentlyPlayed("最近播放"),
    Favorites("我的收藏"),
    HiRes("Hi-Res"),
}

private fun LibraryTrackItem.isHiRes(): Boolean =
    (sampleRateHz ?: 0) > 48_000 || (bitDepth ?: 0) > 16

private fun Int?.orEmptyCount(): String = (this ?: 0).toString()

private fun PlaylistSummary.summaryLabel(): String {
    val totalMinutes = durationMs.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val duration = if (hours > 0L) "$hours 小时 $minutes 分钟" else "$minutes 分钟"
    return "$musicCount 首歌曲 · $duration"
}

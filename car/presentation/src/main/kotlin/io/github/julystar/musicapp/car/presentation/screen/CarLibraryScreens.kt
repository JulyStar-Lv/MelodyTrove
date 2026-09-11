package io.github.julystar.musicapp.car.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.DomainPlaylistTrack
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.PlaylistSummary
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
    val visibleTracks = remember(tracks, sort) {
        when (sort) {
            LibrarySort.Recent -> tracks
            LibrarySort.Name -> tracks.sortedBy { it.title.lowercase() }
            LibrarySort.Artist -> tracks.sortedBy { it.artist.orEmpty().lowercase() }
        }
    }
    LibraryScaffold(
        metrics, LibraryTab.Songs, tracks.size, sort, { sort = it },
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
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Recent) }
    val visibleAlbums = remember(albums, sort) {
        when (sort) {
            LibrarySort.Recent -> albums
            LibrarySort.Name -> albums.sortedBy { it.name.lowercase() }
            LibrarySort.Artist -> albums.sortedBy { it.artist.orEmpty().lowercase() }
        }
    }
    val scope = rememberCoroutineScope()
    val shuffledTracks = remember(tracks) { tracks.shuffled() }
    LibraryScaffold(
        metrics, LibraryTab.Albums, albums.size, sort, { sort = it },
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
            horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
            verticalArrangement = Arrangement.spacedBy(metrics.libraryGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItemsIndexed(visibleAlbums, key = { _, album -> album.id }) { index, album ->
                CarAlbumCard(
                    album, artworkRepository, metrics.recommendationCardWidth - LocalCarSpacing.current.wide,
                    onClick = { onAlbumClick(album.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            if (index == 0) CarFocusIds.content("Albums") else CarFocusIds.item("album", album.id),
                            left = CarFocusIds.Songs,
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
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Recent) }
    val visibleArtists = remember(artists, sort) {
        if (sort == LibrarySort.Recent) artists else artists.sortedBy { it.name.lowercase() }
    }
    val scope = rememberCoroutineScope()
    val shuffledTracks = remember(tracks) { tracks.shuffled() }
    LibraryScaffold(
        metrics, LibraryTab.Artists, artists.size, sort, { sort = it },
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
            horizontalArrangement = Arrangement.spacedBy(metrics.cardGap),
            verticalArrangement = Arrangement.spacedBy(metrics.libraryGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItemsIndexed(visibleArtists, key = { _, artist -> artist.id }) { index, artist ->
                CarArtistCard(
                    artist,
                    albums.firstOrNull { it.artist == artist.name }?.let { Artwork.LibraryAlbum(it.id) },
                    artworkRepository,
                    metrics.recommendationCardWidth * 0.66f,
                    onClick = { onArtistClick(artist.id) },
                    modifier = Modifier
                        .carFocusTarget(
                            if (index == 0) CarFocusIds.content("Artists") else CarFocusIds.item("artist", artist.id),
                            left = CarFocusIds.Songs,
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
    val playbackController = koinInject<PlaybackController>()
    val artworkRepository = koinInject<ArtworkRepository>()
    val favoritesRepository = koinInject<FavoritesRepository>()
    val favoriteTrackIds by favoritesRepository.favoriteTrackIds.collectAsState(emptySet())
    val scope = rememberCoroutineScope()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(playlists) {
        if (playlists.none { it.id == selectedId }) selectedId = playlists.firstOrNull()?.id
    }
    val selected = playlists.firstOrNull { it.id == selectedId }
    val selectedTracks by produceState(emptyList<DomainPlaylistTrack>(), selectedId) {
        val id = selectedId ?: return@produceState
        repository.observePlaylistTracks(id).collect { value = it }
    }
    val tracks = remember(selectedTracks) { selectedTracks.map(DomainPlaylistTrack::toCarLibraryTrackItem) }
    val queue = if (tracks.isEmpty()) null else tracks.toCarPlaybackRequest(0, selectedId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.detailPaneGap),
        modifier = modifier.fillMaxSize().padding(
            start = metrics.detailContentMargin,
            top = metrics.contentTop,
            end = metrics.contentHorizontalPadding,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact),
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(LocalCarColors.current.backgroundSubtle).padding(LocalCarSpacing.current.section),
        ) {
            BasicText("所有歌单", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
            if (playlists.isEmpty()) {
                BasicText("还没有歌单", style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary))
            }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.compact)) {
                itemsIndexed(playlists, key = { _, item -> item.id }) { index, item ->
                    PlaylistCard(
                        item, item.id == selectedId, artworkRepository, { selectedId = item.id },
                        Modifier
                            .carFocusTarget(
                                if (index == 0) CarFocusIds.content("Playlists") else CarFocusIds.item("playlist", item.id),
                                left = CarFocusIds.Playlists,
                            )
                            .height(metrics.compactCardHeight),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(LocalCarColors.current.backgroundSubtle).padding(LocalCarSpacing.current.section),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    BasicText(selected?.title ?: "歌单", style = LocalCarTypography.current.titleLarge.copy(color = LocalCarColors.current.textPrimary))
                    BasicText("${tracks.size} 首歌曲", style = LocalCarTypography.current.body.copy(color = LocalCarColors.current.textSecondary))
                }
                LibraryActionButton("播放全部", CarIcon.Play, queue != null) {
                    queue?.let { request -> scope.launch { playbackController.play(request.items, request.startIndex) } }
                }
            }
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            if (selected == null) CarPageState("还没有歌单", Modifier.fillMaxSize())
            else if (tracks.isEmpty()) CarPageState("这个歌单还没有歌曲", Modifier.fillMaxSize())
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                    CarSongRow(
                        track, index, false,
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
            end = metrics.contentHorizontalPadding,
            bottom = metrics.navigationRailBottom,
        ),
    ) {
        Column(
            modifier = Modifier.width(metrics.detailHeroWidth).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle).padding(LocalCarSpacing.current.section),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LocalCarSpacing.current.small), modifier = Modifier.fillMaxWidth()) {
                LibraryTab.entries.forEach { tab ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f).height(56.dp).carInteractiveSurface(
                            LocalCarShapes.current.control,
                            selected = tab == selectedTab,
                            defaultColor = colors.panel,
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
                if (onPlayAll == null) "随机播放" else "播放全部",
                if (onPlayAll == null) CarIcon.Shuffle else CarIcon.Play,
                itemCount > 0,
                onPlayAll ?: {},
            )
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            BasicText("快速筛选", style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            FilterRow("最近添加", itemCount.toString(), sort == LibrarySort.Recent) { onSort(LibrarySort.Recent) }
            FilterRow("最近播放", "—", false, enabled = false) {}
            FilterRow("我的收藏", "—", false, enabled = false) {}
            FilterRow("Hi-Res", "—", false, enabled = false) {}
            Spacer(Modifier.height(LocalCarSpacing.current.section))
            BasicText("排序方式", style = LocalCarTypography.current.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(LocalCarSpacing.current.small))
            FilterRow("最近添加", if (sort == LibrarySort.Recent) "当前" else "", sort == LibrarySort.Recent) { onSort(LibrarySort.Recent) }
            FilterRow("${selectedTab.label}名称", "A–Z", sort == LibrarySort.Name) { onSort(LibrarySort.Name) }
            FilterRow(if (selectedTab == LibraryTab.Artists) "歌曲数量" else "歌手名称", "A–Z", sort == LibrarySort.Artist) { onSort(LibrarySort.Artist) }
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(LocalCarShapes.current.panel)
                .background(colors.backgroundSubtle).padding(LocalCarSpacing.current.section),
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
private fun PlaylistCard(
    playlist: PlaylistSummary,
    selected: Boolean,
    artworkRepository: ArtworkRepository,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCarColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().carInteractiveSurface(
            LocalCarShapes.current.card, selected = selected, defaultColor = colors.panel, onClick = onClick,
        ).padding(LocalCarSpacing.current.small),
    ) {
        CarArtwork(playlist.coverArtwork, artworkRepository, 72.dp, LocalCarShapes.current.artwork)
        Spacer(Modifier.width(LocalCarSpacing.current.content))
        Column(Modifier.weight(1f)) {
            BasicText(playlist.title, style = LocalCarTypography.current.bodyLarge.copy(color = colors.textPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText("${playlist.musicCount} 首歌曲", style = LocalCarTypography.current.supporting.copy(color = colors.textSecondary))
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

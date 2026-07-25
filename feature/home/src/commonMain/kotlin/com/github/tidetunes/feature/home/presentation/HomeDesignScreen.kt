package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.components.TideGlassScene
import com.github.tidetunes.core.presentation.components.TideStickyGlassActionBar
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tidetunes.core.presentation.generated.resources.Res as CoreRes
import tidetunes.core.presentation.generated.resources.icon_chevron_right
import tidetunes.core.presentation.generated.resources.icon_play
import tidetunes.feature.home.generated.resources.Res
import tidetunes.feature.home.generated.resources.icon_activity
import tidetunes.feature.home.generated.resources.icon_bookmark
import tidetunes.feature.home.generated.resources.icon_clock
import tidetunes.feature.home.generated.resources.icon_headphones
import tidetunes.feature.home.generated.resources.icon_mic_vocal
import tidetunes.feature.home.generated.resources.icon_music_note
import tidetunes.feature.home.generated.resources.icon_sparkles
import tidetunes.feature.home.generated.resources.home_good_evening
import tidetunes.feature.home.generated.resources.home_play
import tidetunes.feature.home.generated.resources.home_albums
import tidetunes.feature.home.generated.resources.home_artists
import tidetunes.feature.home.generated.resources.home_empty_message
import tidetunes.feature.home.generated.resources.home_empty_title
import tidetunes.feature.home.generated.resources.home_from_library
import tidetunes.feature.home.generated.resources.home_open_library
import tidetunes.feature.home.generated.resources.home_now_playing
import tidetunes.feature.home.generated.resources.home_playlists
import tidetunes.feature.home.generated.resources.home_songs
import tidetunes.feature.home.generated.resources.home_subtitle
import tidetunes.feature.home.generated.resources.home_title
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Production Compose mapping of Design/src/app/App.tsx Home page.
 * The section order and compact/desktop behavior intentionally mirror the Design bundle.
 */
@Composable
fun HomeDesignScreen(
    scaffoldPadding: PaddingValues,
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    TideGlassScene(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            val compact = maxWidth < 840.dp
            val pagePadding = if (compact) TideTunesTokens.spacing.pageCompact else TideTunesTokens.spacing.pageExpanded
            val playlistCardWidth = if (compact) 160.dp else 178.dp
            val albumCardWidth = if (compact) 120.dp else 178.dp
            val artistSize = 128.dp
            val listState = rememberLazyListState()
            val collapseDistance = with(LocalDensity.current) {
                TideTunesTokens.adaptive.compactHeaderCollapseDistance.roundToPx()
            }
            val actionBarProgress by remember(listState, collapseDistance) {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (listState.firstVisibleItemScrollOffset / collapseDistance.toFloat())
                            .coerceIn(0f, 1f)
                    }
                }
            }
            val pageTitleAlpha = (1f - actionBarProgress / 0.70f).coerceIn(0f, 1f)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .widthIn(max = TideTunesTokens.adaptive.contentMaxWidth),
                contentPadding = PaddingValues(
                    start = pagePadding,
                    top = 8.dp,
                    end = pagePadding,
                    bottom = scaffoldPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item {
                    TidePageHeader(
                        title = stringResource(
                            if (compact) Res.string.home_title else Res.string.home_good_evening
                        ),
                        subtitle = if (compact) null else stringResource(Res.string.home_subtitle),
                        modifier = Modifier.alpha(pageTitleAlpha),
                    )
                }
                if (state.isEmpty) {
                    item {
                        HomeEmptyState(
                            onOpenLibrary = { onAction(HomeAction.NavigateToLibrary) },
                        )
                    }
                }
                state.recentTracks.firstOrNull()?.let { track ->
                    item {
                        DailyPicksHero(
                            compact = compact,
                            track = track,
                            onPlay = { onAction(HomeAction.PlayTrack(track.id)) },
                        )
                    }
                }
                if (state.pinnedPlaylists.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = stringResource(Res.string.home_playlists),
                            icon = Res.drawable.icon_bookmark,
                            compact = compact,
                            onClick = { onAction(HomeAction.NavigateToLibrary) },
                        ) {
                            PlaylistRow(
                                playlists = state.pinnedPlaylists,
                                cardWidth = playlistCardWidth,
                                showMeta = true,
                                onClick = { onAction(HomeAction.NavigateToLibrary) },
                            )
                        }
                    }
                }
                if (state.recentTracks.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = stringResource(Res.string.home_songs),
                            icon = Res.drawable.icon_clock,
                            compact = compact,
                            onClick = { onAction(HomeAction.NavigateToLibrary) },
                        ) {
                            LibraryTrackList(
                                tracks = state.recentTracks,
                                onPlay = { track -> onAction(HomeAction.PlayTrack(track.id)) },
                            )
                        }
                    }
                }
                if (state.recentlyAddedAlbums.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = stringResource(Res.string.home_albums),
                            icon = Res.drawable.icon_sparkles,
                            compact = compact,
                            onClick = { onAction(HomeAction.NavigateToLibrary) },
                        ) {
                            AlbumRow(
                                albums = state.recentlyAddedAlbums,
                                cardWidth = albumCardWidth,
                                onClick = { onAction(HomeAction.NavigateToLibrary) },
                            )
                        }
                    }
                }
                if (state.artists.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = stringResource(Res.string.home_artists),
                            icon = Res.drawable.icon_mic_vocal,
                            compact = compact,
                            onClick = { onAction(HomeAction.NavigateToLibrary) },
                        ) {
                            ArtistRow(
                                artists = state.artists,
                                size = artistSize,
                                onOpen = { onAction(HomeAction.NavigateToLibrary) },
                            )
                        }
                    }
                }
                if (state.statistics != null) {
                    item {
                        HomeStatisticsCard(
                            statistics = state.statistics,
                            compact = compact,
                        )
                    }
                }
            }
            TideStickyGlassActionBar(
                title = stringResource(if (compact) Res.string.home_title else Res.string.home_good_evening),
                collapseFraction = actionBarProgress,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun DailyPicksHero(
    compact: Boolean,
    track: HomeRecentTrack,
    onPlay: () -> Unit,
) {
    if (compact) {
        CompactDailyPicksHero(track = track, onPlay = onPlay)
        return
    }

    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .shadow(TideTunesTokens.elevation.card, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        TideTunesBrand.Primary,
                        TideTunesBrand.Secondary,
                        TideTunesBrand.SupportBlue,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(230.dp)
                .clip(CircleShape)
                .background(TideTunesBrand.SupportOrange.copy(alpha = 0.38f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(210.dp)
                .clip(CircleShape)
                .background(TideTunesBrand.SupportGreen.copy(alpha = 0.30f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.66f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f)),
            )
            Text(
                text = stringResource(Res.string.home_from_library).uppercase(),
                color = Color.White.copy(alpha = 0.86f),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = track.title,
                color = Color.White,
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.subtitle.isNotBlank()) {
                Text(
                    text = track.subtitle,
                    color = Color.White.copy(alpha = 0.76f),
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroAction(
                    text = stringResource(Res.string.home_play),
                    primary = true,
                    onClick = onPlay,
                )
            }
        }
    }
}

@Composable
private fun CompactDailyPicksHero(
    track: HomeRecentTrack,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .shadow(TideTunesTokens.elevation.card, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF263F93),
                        Color(0xFF5940C7),
                        Color(0xFF1E397E),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(190.dp)
                .padding(start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.home_from_library),
                color = Color.White,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(
                    Res.string.home_now_playing,
                    track.title,
                ),
                color = Color.White.copy(alpha = 0.72f),
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            CompactHeroAction(onClick = onPlay)
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(width = 140.dp, height = 112.dp),
        ) {
            DecorativeCover(
                colors = track.color to TideTunesBrand.Secondary,
                modifier = Modifier
                    .offset(x = 68.dp)
                    .size(72.dp),
            )
            DecorativeCover(
                colors = TideTunesBrand.SupportBlue to track.color,
                modifier = Modifier
                    .offset(x = 55.dp, y = 47.dp)
                    .size(65.dp),
            )
            DecorativeCover(
                colors = TideTunesBrand.SupportGreen to TideTunesBrand.Primary,
                modifier = Modifier
                    .offset(y = 22.dp)
                    .size(60.dp),
            )
        }
    }
}

@Composable
private fun DecorativeCover(
    colors: Pair<Color, Color>,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.first, colors.second))),
    )
}

@Composable
private fun CompactHeroAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(TideTunesTokens.adaptive.minimumTouchTarget)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(TideTunesBrand.Primary.copy(alpha = 0.88f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(CoreRes.drawable.icon_play),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(Res.string.home_play),
            color = Color.White,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HeroAction(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(TideTunesTokens.adaptive.minimumTouchTarget)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (primary) {
            Icon(
                painter = painterResource(Res.drawable.icon_music_note),
                contentDescription = null,
                tint = Color(0xFF0D0B18),
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            color = if (primary) Color(0xFF0D0B18) else Color.White,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeEmptyState(onOpenLibrary: () -> Unit) {
    val shape = RoundedCornerShape(TideTunesTokens.shapes.card)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.home_empty_title),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title3,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.home_empty_message),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Row(
            modifier = Modifier
                .height(TideTunesTokens.adaptive.minimumTouchTarget)
                .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                .background(MiuixTheme.colorScheme.primary)
                .clickable(onClick = onOpenLibrary)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.home_open_library),
                color = MiuixTheme.colorScheme.onPrimary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    icon: DrawableResource,
    compact: Boolean,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget)
                .clip(RoundedCornerShape(14.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MiuixTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    painter = painterResource(CoreRes.drawable.icon_chevron_right),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.size(if (compact) 24.dp else 22.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun PlaylistRow(
    playlists: List<HomePlaylist>,
    cardWidth: Dp,
    showMeta: Boolean,
    onClick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TideTunesTokens.adaptive.minimumTouchTarget),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(playlists) { playlist ->
            PlaylistCard(
                playlist = playlist,
                width = cardWidth,
                showMeta = showMeta,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: HomePlaylist,
    width: Dp,
    showMeta: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        val artworkShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .size(width)
                .shadow(TideTunesTokens.elevation.card, artworkShape, clip = false)
                .clip(artworkShape)
                .background(Brush.linearGradient(playlist.colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.size(width * 0.34f),
            )
            if (showMeta) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
                            ),
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(
                        text = playlist.meta,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = playlist.title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = playlist.description,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryTrackList(
    tracks: List<HomeRecentTrack>,
    onPlay: (HomeRecentTrack) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tracks.take(6).forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPlay(track) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    style = MiuixTheme.textStyles.footnote1,
                    modifier = Modifier.width(18.dp),
                )
                ArtworkTile(track, 46.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (track.subtitle.isNotBlank()) {
                        Text(
                            text = track.subtitle,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            style = MiuixTheme.textStyles.footnote1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    albums: List<HomeFeaturedAlbum>,
    cardWidth: Dp,
    onClick: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onClick),
            ) {
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .size(cardWidth)
                        .shadow(TideTunesTokens.elevation.card, shape, clip = false)
                        .clip(shape)
                        .background(Brush.linearGradient(album.colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_music_note),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.size(cardWidth * 0.34f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = album.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (album.subtitle.isNotBlank()) {
                    Text(
                        text = album.subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(
    artists: List<HomeArtist>,
    size: Dp,
    onOpen: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(artists) { artist ->
            Column(
                modifier = Modifier
                    .width(size)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .shadow(TideTunesTokens.elevation.card, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(artist.colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = artist.initials,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = artist.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist.followers.isNotBlank()) {
                    Text(
                        text = artist.followers,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkTile(track: HomeRecentTrack, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    listOf(track.color, TideTunesBrand.Secondary),
                ),
            ),
    ) {
        Icon(
            painter = painterResource(Res.drawable.icon_music_note),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.48f),
            modifier = Modifier.align(Alignment.Center).size(size * 0.42f),
        )
    }
}


@Composable
private fun HomeStatisticsCard(
    statistics: com.github.tidetunes.feature.home.domain.HomeStatistics,
    compact: Boolean,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_headphones),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Your Listening",
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    label = "Tracks played",
                    value = statistics.totalTracksEverPlayed.toString(),
                )
                StatItem(
                    label = "Today",
                    value = statistics.tracksPlayedToday.toString(),
                )
                StatItem(
                    label = "Total time",
                    value = formatListeningDuration(statistics.totalListeningDurationMs),
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
        )
    }
}

private fun formatListeningDuration(totalMs: Long): String {
    val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) hours.toString() + "h " + minutes.toString() + "m" else minutes.toString() + "m"
}
private val HomeState.isEmpty: Boolean
    get() {
        val hasContent = featuredAlbums.isNotEmpty() ||
            recentlyAddedAlbums.isNotEmpty() ||
            artists.isNotEmpty() ||
            pinnedPlaylists.isNotEmpty() ||
            recentTracks.isNotEmpty()
        return !hasContent && statistics == null
    }

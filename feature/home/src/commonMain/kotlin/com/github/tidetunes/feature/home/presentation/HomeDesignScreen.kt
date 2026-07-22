package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.QualityBadge
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
import tidetunes.feature.home.generated.resources.home_cover_1
import tidetunes.feature.home.generated.resources.home_cover_2
import tidetunes.feature.home.generated.resources.home_cover_3
import tidetunes.feature.home.generated.resources.home_cover_4
import tidetunes.feature.home.generated.resources.home_cover_5
import tidetunes.feature.home.generated.resources.home_cover_6
import tidetunes.feature.home.generated.resources.home_cover_7
import tidetunes.feature.home.generated.resources.home_cover_8
import tidetunes.feature.home.generated.resources.icon_heart
import tidetunes.feature.home.generated.resources.icon_music_note
import tidetunes.feature.home.generated.resources.home_continue_playing
import tidetunes.feature.home.generated.resources.home_daily_picks
import tidetunes.feature.home.generated.resources.home_favorite
import tidetunes.feature.home.generated.resources.home_good_evening
import tidetunes.feature.home.generated.resources.home_music_all_sources
import tidetunes.feature.home.generated.resources.home_no_track
import tidetunes.feature.home.generated.resources.home_now_playing
import tidetunes.feature.home.generated.resources.home_pinned_playlists
import tidetunes.feature.home.generated.resources.home_play
import tidetunes.feature.home.generated.resources.home_recently_added
import tidetunes.feature.home.generated.resources.home_recently_played
import tidetunes.feature.home.generated.resources.home_recommended_artists
import tidetunes.feature.home.generated.resources.home_save
import tidetunes.feature.home.generated.resources.home_subtitle
import tidetunes.feature.home.generated.resources.home_this_month
import tidetunes.feature.home.generated.resources.home_title
import tidetunes.feature.home.generated.resources.home_top_tracks
import tidetunes.feature.home.generated.resources.home_your_listening
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
            val collapseDistance = with(LocalDensity.current) { 88.dp.roundToPx() }
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
                    )
                }
                item {
                    DailyPicksHero(
                        compact = compact,
                        track = state.recentTracks.firstOrNull(),
                        onPlay = { onAction(HomeAction.OpenNowPlaying) },
                    )
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_pinned_playlists),
                        compact = compact,
                        onClick = { onAction(HomeAction.NavigateToLibrary) },
                    ) {
                        PlaylistRow(
                            playlists = state.pinnedPlaylists,
                            cardWidth = playlistCardWidth,
                            showMeta = true,
                            onPlay = { onAction(HomeAction.OpenNowPlaying) },
                        )
                    }
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_your_listening),
                        compact = compact,
                        onClick = { onAction(HomeAction.NavigateToLibrary) },
                    ) {
                        ListeningPreview(
                            tracks = state.recentTracks,
                            onPlay = { onAction(HomeAction.OpenNowPlaying) },
                        )
                    }
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_continue_playing),
                        compact = compact,
                        onClick = { onAction(HomeAction.NavigateToLibrary) },
                    ) {
                        PlaylistRow(
                            playlists = state.pinnedPlaylists,
                            cardWidth = playlistCardWidth,
                            showMeta = false,
                            onPlay = { onAction(HomeAction.OpenNowPlaying) },
                        )
                    }
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_recently_played),
                        compact = compact,
                        onClick = { onAction(HomeAction.NavigateToLibrary) },
                    ) {
                        RecentlyPlayedList(
                            tracks = state.recentTracks,
                            onPlay = { onAction(HomeAction.OpenNowPlaying) },
                        )
                    }
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_recently_added),
                        compact = compact,
                        onClick = { onAction(HomeAction.NavigateToLibrary) },
                    ) {
                        AlbumRow(
                            albums = state.recentlyAddedAlbums,
                            cardWidth = albumCardWidth,
                            onPlay = { onAction(HomeAction.OpenNowPlaying) },
                        )
                    }
                }
                item {
                    HomeSection(
                        title = stringResource(Res.string.home_recommended_artists),
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
    track: HomeRecentTrack?,
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
                text = stringResource(Res.string.home_daily_picks).uppercase(),
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
                text = track?.title ?: "Midnight Cascade",
                color = Color.White,
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track?.subtitle ?: "Luna Waves · Tidal Drift",
                color = Color.White.copy(alpha = 0.76f),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroAction(
                    text = stringResource(Res.string.home_play),
                    primary = true,
                    onClick = onPlay,
                )
                HeroAction(
                    text = stringResource(Res.string.home_save),
                    primary = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun CompactDailyPicksHero(
    track: HomeRecentTrack?,
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
                text = stringResource(Res.string.home_daily_picks),
                color = Color.White,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(
                    Res.string.home_now_playing,
                    track?.title ?: stringResource(Res.string.home_no_track),
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
                artworkIndex = 4,
                modifier = Modifier
                    .offset(x = 68.dp)
                    .size(72.dp),
            )
            DecorativeCover(
                artworkIndex = 6,
                modifier = Modifier
                    .offset(x = 55.dp, y = 47.dp)
                    .size(65.dp),
            )
            DecorativeCover(
                artworkIndex = 8,
                modifier = Modifier
                    .offset(y = 22.dp)
                    .size(60.dp),
            )
        }
    }
}

@Composable
private fun DecorativeCover(
    artworkIndex: Int,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(homeCoverResource(artworkIndex)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape)
            .clip(CircleShape),
    )
}

@Composable
private fun CompactHeroAction(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
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
            .height(42.dp)
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
private fun HomeSection(
    title: String,
    compact: Boolean,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    painter = painterResource(Res.drawable.icon_music_note),
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
                modifier = Modifier.weight(1f),
            )
            if (onClick != null) {
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
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        playlists.forEach { playlist ->
            PlaylistCard(
                playlist = playlist,
                width = cardWidth,
                showMeta = showMeta,
                onClick = onPlay,
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
        ) {
            Image(
                painter = painterResource(homeCoverResource(playlist.artworkIndex)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
private fun ListeningPreview(
    tracks: List<HomeRecentTrack>,
    onPlay: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 720.dp
        if (horizontal) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ListeningSummaryCard(modifier = Modifier.weight(0.82f))
                Column(
                    modifier = Modifier.weight(1.18f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    tracks.take(3).forEachIndexed { index, track ->
                        ListeningTrackRow(index + 1, track, onPlay)
                    }
                }
            }
        } else {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(Res.string.home_top_tracks),
                        color = MiuixTheme.colorScheme.onBackground,
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.home_this_month),
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
                listOf(
                    ListeningRanking(0, 1, "4h 28m", "32 plays"),
                    ListeningRanking(3, 2, "3h 31m", "25 plays"),
                    ListeningRanking(1, 3, "3h 4m", "21 plays"),
                ).forEach { ranking ->
                    tracks.getOrNull(ranking.trackIndex)?.let { track ->
                        ListeningTrackRow(
                            rank = ranking.rank,
                            track = track,
                            onPlay = onPlay,
                            playedAt = ranking.playedAt,
                            detail = ranking.detail,
                        )
                    }
                }
            }
        }
    }
}

private data class ListeningRanking(
    val trackIndex: Int,
    val rank: Int,
    val playedAt: String,
    val detail: String,
)

@Composable
private fun ListeningSummaryCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.card))
            .background(
                Brush.linearGradient(
                    listOf(
                        MiuixTheme.colorScheme.surfaceContainer,
                        MiuixTheme.colorScheme.tertiaryContainer.copy(alpha = 0.82f),
                    ),
                ),
            )
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.home_this_month).uppercase(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "1,284 min",
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.headline1,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.home_music_all_sources),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun ListeningTrackRow(
    rank: Int,
    track: HomeRecentTrack,
    onPlay: () -> Unit,
    playedAt: String? = null,
    detail: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = rank.toString().padStart(2, '0'),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            style = MiuixTheme.textStyles.footnote2,
        )
        ArtworkTile(track, 42.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.subtitle,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (playedAt != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = playedAt,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = MiuixTheme.textStyles.footnote2,
                    fontWeight = FontWeight.Medium,
                )
                detail?.let {
                    Text(
                        text = it,
                        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.70f),
                        style = MiuixTheme.textStyles.footnote2,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedList(
    tracks: List<HomeRecentTrack>,
    onPlay: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        tracks.take(6).forEachIndexed { index, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onPlay)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = track.title,
                            color = MiuixTheme.colorScheme.onBackground,
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        track.qualityBadge?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            QualityBadge(type = it)
                        }
                    }
                    Text(
                        text = track.subtitle,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (track.liked) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_heart),
                        contentDescription = stringResource(Res.string.home_favorite),
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Text(
                    text = recentLabel(index),
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
        }
    }
}

@Composable
private fun AlbumRow(
    albums: List<HomeFeaturedAlbum>,
    cardWidth: Dp,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        albums.forEach { album ->
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPlay),
            ) {
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .size(cardWidth)
                        .shadow(TideTunesTokens.elevation.card, shape, clip = false)
                        .clip(shape)
                        .background(Brush.linearGradient(album.colors)),
                ) {
                    Image(
                        painter = painterResource(homeCoverResource(album.artworkIndex)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
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

@Composable
private fun ArtistRow(
    artists: List<HomeArtist>,
    size: Dp,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        artists.forEach { artist ->
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
        Image(
            painter = painterResource(homeCoverResource(track.artworkIndex)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun homeCoverResource(index: Int): DrawableResource = when (index) {
    1 -> Res.drawable.home_cover_1
    2 -> Res.drawable.home_cover_2
    3 -> Res.drawable.home_cover_3
    4 -> Res.drawable.home_cover_4
    5 -> Res.drawable.home_cover_5
    6 -> Res.drawable.home_cover_6
    7 -> Res.drawable.home_cover_7
    else -> Res.drawable.home_cover_8
}

private fun recentLabel(index: Int): String = when (index) {
    0 -> "12m"
    1 -> "28m"
    2 -> "1h"
    3 -> "2h"
    else -> "Yesterday"
}

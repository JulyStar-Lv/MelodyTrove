package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.QualityBadge
import com.github.tidetunes.core.presentation.components.TidePageHeader
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesGradients
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.painterResource
import tidetunes.feature.home.generated.resources.Res
import tidetunes.feature.home.generated.resources.icon_heart
import tidetunes.feature.home.generated.resources.icon_music_note
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        val compact = maxWidth < 840.dp
        val pagePadding = if (compact) TideTunesTokens.spacing.pageCompact else TideTunesTokens.spacing.pageExpanded
        val cardWidth = if (compact) 156.dp else 178.dp
        val artistSize = if (compact) 116.dp else 128.dp

        LazyColumn(
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
                    title = if (compact) "Home" else "Good Evening",
                    subtitle = if (compact) null else "One library, every source.",
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
                    title = "Pinned Playlists",
                    onClick = { onAction(HomeAction.NavigateToLibrary) },
                ) {
                    PlaylistRow(
                        playlists = state.pinnedPlaylists,
                        cardWidth = cardWidth,
                        showMeta = true,
                        onPlay = { onAction(HomeAction.OpenNowPlaying) },
                    )
                }
            }
            item {
                HomeSection(
                    title = "Your Listening",
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
                    title = "Continue Playing",
                    onClick = { onAction(HomeAction.NavigateToLibrary) },
                ) {
                    PlaylistRow(
                        playlists = state.pinnedPlaylists,
                        cardWidth = cardWidth,
                        showMeta = false,
                        onPlay = { onAction(HomeAction.OpenNowPlaying) },
                    )
                }
            }
            item {
                HomeSection(
                    title = "Recently Played",
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
                    title = "Recently Added",
                    onClick = { onAction(HomeAction.NavigateToLibrary) },
                ) {
                    AlbumRow(
                        albums = state.recentlyAddedAlbums,
                        cardWidth = cardWidth,
                        onPlay = { onAction(HomeAction.OpenNowPlaying) },
                    )
                }
            }
            item {
                HomeSection(
                    title = "Recommended Artists",
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
    }
}

@Composable
private fun DailyPicksHero(
    compact: Boolean,
    track: HomeRecentTrack?,
    onPlay: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 30.dp else 36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 250.dp else 310.dp)
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
                .size(if (compact) 170.dp else 230.dp)
                .clip(CircleShape)
                .background(TideTunesBrand.SupportOrange.copy(alpha = 0.38f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(if (compact) 150.dp else 210.dp)
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
                text = "DAILY PICKS",
                color = Color.White.copy(alpha = 0.86f),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(if (compact) 22.dp else 28.dp),
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
                    text = "Play",
                    primary = true,
                    onClick = onPlay,
                )
                HeroAction(
                    text = "Save",
                    primary = false,
                    onClick = {},
                )
            }
        }
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
                Text(
                    text = "See All  ›",
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
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
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        val artworkShape = RoundedCornerShape(24.dp)
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
                tint = Color.White.copy(alpha = 0.24f),
                modifier = Modifier.size(width * 0.34f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = playlist.title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body1,
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
        if (showMeta) {
            Text(
                text = playlist.meta,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                style = MiuixTheme.textStyles.footnote2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ListeningSummaryCard()
                tracks.take(3).forEachIndexed { index, track ->
                    ListeningTrackRow(index + 1, track, onPlay)
                }
            }
        }
    }
}

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
                text = "THIS MONTH",
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
                text = "Music across all sources",
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
        ArtworkTile(track.color, 42.dp)
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
                ArtworkTile(track.color, 46.dp)
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
                        contentDescription = "Favorite",
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
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onPlay),
            ) {
                val shape = RoundedCornerShape(24.dp)
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
                        tint = Color.White.copy(alpha = 0.24f),
                        modifier = Modifier.size(cardWidth * 0.34f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = album.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
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
private fun ArtworkTile(color: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    listOf(color, TideTunesBrand.Secondary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.icon_music_note),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.size(size * 0.38f),
        )
    }
}

private fun recentLabel(index: Int): String = when (index) {
    0 -> "12m"
    1 -> "28m"
    2 -> "1h"
    3 -> "2h"
    else -> "Yesterday"
}

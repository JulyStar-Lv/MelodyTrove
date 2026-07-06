package com.github.tidetunes.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.theme.TideTunesBrand
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tidetunes.feature.home.generated.resources.Res
import tidetunes.feature.home.generated.resources.icon_adjust
import tidetunes.feature.home.generated.resources.icon_download
import tidetunes.feature.home.generated.resources.icon_music_note
import tidetunes.feature.home.generated.resources.icon_search
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeOverviewScreen(
    scaffoldPadding: PaddingValues,
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = spacing.pageMedium,
            top = 18.dp,
            end = spacing.pageMedium,
            bottom = scaffoldPadding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        item {
            HomeHero(
                onNavigateToSearch = { onAction(HomeAction.NavigateToSearch) },
            )
        }
        item {
            FeaturedSection(albums = state.featuredAlbums)
        }
        item {
            RecentSection(
                tracks = state.recentTracks,
                onOpenNowPlaying = { onAction(HomeAction.OpenNowPlaying) },
            )
        }
        item {
            QuickActions(
                onNavigateToLibrary = { onAction(HomeAction.NavigateToLibrary) },
                onNavigateToDownloads = { onAction(HomeAction.NavigateToDownloads) },
                onOpenSleepTimer = { onAction(HomeAction.OpenSleepTimer) },
            )
        }
    }
}

@Composable
private fun HomeHero(
    onNavigateToSearch: () -> Unit,
) {
    val spacing = TideTunesTokens.spacing
    val shapes = TideTunesTokens.shapes

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TideTunes",
                    style = MiuixTheme.textStyles.title1,
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "One Library. Every Source.",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(shapes.md))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(shapes.md))
                    .clickable(onClick = onNavigateToSearch),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_search),
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(shapes.xxl))
                .background(
                    Brush.linearGradient(
                        listOf(
                            TideTunesBrand.Primary,
                            TideTunesBrand.Secondary,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(56.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "聚合你的全部音乐",
                    style = MiuixTheme.textStyles.title2,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "WebDAV、本地、云盘与服务器来源，统一播放体验。",
                    style = MiuixTheme.textStyles.body2,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FeaturedSection(albums: List<HomeFeaturedAlbum>) {
    SectionTitle(title = "最近播放")
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        albums.forEach { album ->
            FeaturedAlbumCard(album = album)
        }
    }
}

@Composable
private fun FeaturedAlbumCard(album: HomeFeaturedAlbum) {
    val shapes = TideTunesTokens.shapes

    Column(
        modifier = Modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(shapes.xl))
                .background(Brush.linearGradient(album.colors))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(shapes.xl))
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_music_note),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = album.title,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.subtitle,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentSection(
    tracks: List<HomeRecentTrack>,
    onOpenNowPlaying: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    SectionTitle(title = "推荐曲目")
    Spacer(modifier = Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(shapes.lg))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(shapes.lg))
            .padding(vertical = 6.dp),
    ) {
        tracks.forEach { track ->
            RecentTrackRow(
                track = track,
                onClick = onOpenNowPlaying,
            )
        }
    }
}

@Composable
private fun RecentTrackRow(
    track: HomeRecentTrack,
    onClick: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(shapes.md))
                .background(
                    Brush.linearGradient(
                        listOf(
                            track.color,
                            track.color.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.subtitle,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToLibrary: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onOpenSleepTimer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickActionPill(
            text = "资料库",
            icon = Res.drawable.icon_music_note,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToLibrary,
        )
        QuickActionPill(
            text = "下载",
            icon = Res.drawable.icon_download,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToDownloads,
        )
        QuickActionPill(
            text = "睡眠",
            icon = Res.drawable.icon_adjust,
            modifier = Modifier.weight(1f),
            onClick = onOpenSleepTimer,
        )
    }
}

@Composable
private fun QuickActionPill(
    text: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shapes = TideTunesTokens.shapes

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(shapes.full))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(shapes.full))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MiuixTheme.textStyles.title3,
        color = MiuixTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

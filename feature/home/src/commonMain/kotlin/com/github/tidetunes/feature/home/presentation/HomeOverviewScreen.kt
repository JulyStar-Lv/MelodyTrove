package com.github.tidetunes.feature.home.presentation

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.QualityBadge
import com.github.tidetunes.core.presentation.components.TideActionPill
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideIconButton
import com.github.tidetunes.core.presentation.components.TideIconButtonSize
import com.github.tidetunes.core.presentation.components.TideIconButtonVariant
import com.github.tidetunes.core.presentation.components.TideMusicArtworkTile
import com.github.tidetunes.core.presentation.components.TideSectionHeader
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
import com.github.tidetunes.core.presentation.theme.TideTunesGradients
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = rememberWindowSizeClass(DpSize(maxWidth, maxHeight))
        val layout = homeOverviewLayout(windowSizeClass)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = layout.horizontalPadding,
                top = layout.topPadding,
                end = layout.horizontalPadding,
                bottom = scaffoldPadding.calculateBottomPadding() + layout.bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing),
        ) {
            item {
                HomeHero(
                    heroHeight = layout.heroHeight,
                    onNavigateToSearch = { onAction(HomeAction.NavigateToSearch) },
                )
            }
            item {
                FeaturedSection(
                    albums = state.featuredAlbums,
                    cardSize = layout.albumCardSize,
                )
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
}

@Composable
private fun HomeHero(
    heroHeight: Dp,
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
            TideIconButton(
                size = TideIconButtonSize.Touch,
                variant = TideIconButtonVariant.Surface,
                painter = painterResource(Res.drawable.icon_search),
                onClick = onNavigateToSearch,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clip(RoundedCornerShape(shapes.xxl))
                .background(
                    Brush.horizontalGradient(TideTunesGradients.Brand.colors),
                )
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(shapes.full))
                    .background(Color.White.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(TideTunesTokens.shapes.lg))
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
private fun FeaturedSection(
    albums: List<HomeFeaturedAlbum>,
    cardSize: Dp,
) {
    SectionTitle(title = "最近播放")
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        albums.forEach { album ->
            FeaturedAlbumCard(
                album = album,
                size = cardSize,
            )
        }
    }
}

@Composable
private fun FeaturedAlbumCard(
    album: HomeFeaturedAlbum,
    size: Dp,
) {
    val shapes = TideTunesTokens.shapes

    Column(
        modifier = Modifier.width(size),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(size)
                .clip(RoundedCornerShape(shapes.xl))
                .background(Brush.horizontalGradient(album.colors))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(shapes.xl))
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(shapes.full))
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
    SectionTitle(title = "推荐曲目")
    Spacer(modifier = Modifier.height(10.dp))
    TideCardSurface(contentPadding = PaddingValues(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            tracks.forEach { track ->
                RecentTrackRow(
                    track = track,
                    onClick = onOpenNowPlaying,
                )
            }
        }
    }
}

@Composable
private fun RecentTrackRow(
    track: HomeRecentTrack,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TideMusicArtworkTile(accentColor = track.color)
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
        track.qualityBadge?.let { badge ->
            Spacer(modifier = Modifier.width(10.dp))
            QualityBadge(type = badge)
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
        TideActionPill(
            text = "资料库",
            painter = painterResource(Res.drawable.icon_music_note),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToLibrary,
        )
        TideActionPill(
            text = "下载",
            painter = painterResource(Res.drawable.icon_download),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToDownloads,
        )
        TideActionPill(
            text = "睡眠",
            painter = painterResource(Res.drawable.icon_adjust),
            modifier = Modifier.weight(1f),
            onClick = onOpenSleepTimer,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    TideSectionHeader(title = title)
}

@Composable
private fun homeOverviewLayout(windowSizeClass: WindowSizeClass): HomeOverviewLayout {
    val spacing = TideTunesTokens.spacing
    return when (windowSizeClass) {
        WindowSizeClass.Compact -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 16.dp,
            bottomPadding = 32.dp,
            sectionSpacing = spacing.lg,
            heroHeight = 150.dp,
            albumCardSize = 140.dp,
        )
        WindowSizeClass.Medium -> HomeOverviewLayout(
            horizontalPadding = spacing.pageMedium,
            topPadding = 18.dp,
            bottomPadding = 36.dp,
            sectionSpacing = spacing.lg,
            heroHeight = 164.dp,
            albumCardSize = 156.dp,
        )
        WindowSizeClass.Expanded -> HomeOverviewLayout(
            horizontalPadding = spacing.pageExpanded,
            topPadding = 20.dp,
            bottomPadding = 40.dp,
            sectionSpacing = spacing.lg,
            heroHeight = 176.dp,
            albumCardSize = 168.dp,
        )
        WindowSizeClass.Large -> HomeOverviewLayout(
            horizontalPadding = spacing.xl,
            topPadding = 24.dp,
            bottomPadding = 48.dp,
            sectionSpacing = spacing.xl,
            heroHeight = 188.dp,
            albumCardSize = 184.dp,
        )
        WindowSizeClass.XL -> HomeOverviewLayout(
            horizontalPadding = 40.dp,
            topPadding = 28.dp,
            bottomPadding = 56.dp,
            sectionSpacing = spacing.xl,
            heroHeight = 204.dp,
            albumCardSize = 200.dp,
        )
    }
}

private data class HomeOverviewLayout(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val sectionSpacing: Dp,
    val heroHeight: Dp,
    val albumCardSize: Dp,
)

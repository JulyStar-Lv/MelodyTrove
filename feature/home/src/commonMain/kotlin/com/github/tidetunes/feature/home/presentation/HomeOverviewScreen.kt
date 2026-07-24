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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.tidetunes.core.presentation.components.QualityBadge
import com.github.tidetunes.core.presentation.layout.WindowSizeClass
import com.github.tidetunes.core.presentation.layout.rememberWindowSizeClass
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

@Composable
fun HomeOverviewScreen(
    scaffoldPadding: PaddingValues,
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = rememberWindowSizeClass(DpSize(maxWidth, maxHeight))
        val layout = homeOverviewLayout(windowSizeClass)
        val showMobileHeader = windowSizeClass != WindowSizeClass.Large &&
            windowSizeClass != WindowSizeClass.XL

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
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (showMobileHeader) {
                item {
                    MobilePageHeader(title = "Good Evening")
                }
            }
            item {
                FeaturedHero(
                    playlist = state.pinnedPlaylists.firstOrNull(),
                    height = layout.heroHeight,
                    onPlay = { onAction(HomeAction.NavigateToLibrary) },
                )
            }
            item {
                HomeMediaSection(title = "Continue Listening", action = "See All") {
                    MediaCardRow(
                        albums = state.featuredAlbums,
                        cardSize = layout.albumCardSize,
                    )
                }
            }
            item {
                HomeMediaSection(title = "Recently Added", action = "See All") {
                    MediaCardRow(
                        albums = state.recentlyAddedAlbums,
                        cardSize = layout.smallAlbumCardSize,
                    )
                }
            }
            item {
                HomeMediaSection(title = "Recommended Artists", action = "See All") {
                    ArtistRow(artists = state.artists)
                }
            }
            item {
                HomeMediaSection(title = "Pinned Playlists", action = "See All") {
                    PlaylistRow(playlists = state.pinnedPlaylists)
                }
            }
            item {
                HomeMediaSection(title = "Recently Played") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.recentTracks.forEach { track ->
                            RecentTrackRow(
                                track = track,
                            onClick = { onAction(HomeAction.NavigateToLibrary) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobilePageHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderAction(label = "◐")
            HeaderAction(label = "•")
        }
    }
}

@Composable
private fun HeaderAction(label: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeaturedHero(
    playlist: HomePlaylist?,
    height: Dp,
    onPlay: () -> Unit,
) {
    val colors = playlist?.colors ?: TideTunesGradients.Brand.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(colors)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == 0) 20.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
                        .background(
                            if (index == 0) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.40f)
                            },
                        ),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "FEATURED PLAYLIST",
                color = Color.White.copy(alpha = 0.72f),
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = playlist?.title ?: "Evening Frequencies",
                color = Color.White,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlist?.description ?: "Deep electronic for golden hour",
                color = Color.White.copy(alpha = 0.72f),
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroButton(
                    text = "▶  Play",
                    background = Color.White,
                    foreground = Color(0xFF0D0B18),
                    onClick = onPlay,
                )
                HeroButton(
                    text = "◇  Save",
                    background = Color.White.copy(alpha = 0.20f),
                    foreground = Color.White,
                )
            }
        }
    }
}

@Composable
private fun HeroButton(
    text: String,
    background: Color,
    foreground: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(TideTunesTokens.shapes.full))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = foreground,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeMediaSection(
    title: String,
    action: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onBackground,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )
            if (action != null) {
                Text(
                    text = "$action  ›",
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
private fun MediaCardRow(
    albums: List<HomeFeaturedAlbum>,
    cardSize: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        albums.forEach { album ->
            AlbumCard(album = album, size = cardSize)
        }
    }
}

@Composable
private fun AlbumCard(
    album: HomeFeaturedAlbum,
    size: Dp,
) {
    Column(
        modifier = Modifier.width(size),
    ) {
        val artworkShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .size(size)
                .shadow(8.dp, artworkShape, clip = false)
                .clip(artworkShape)
                .background(Brush.linearGradient(album.colors)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.24f),
                modifier = Modifier.size(size * 0.36f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.title,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${album.subtitle} · 2024",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistRow(artists: List<HomeArtist>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        artists.forEach { artist ->
            Column(
                modifier = Modifier.width(128.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .shadow(8.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(artist.colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = artist.initials,
                        color = Color.White.copy(alpha = 0.90f),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = artist.name,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
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
private fun PlaylistRow(playlists: List<HomePlaylist>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        playlists.forEach { playlist ->
            Column(
                modifier = Modifier.width(160.dp),
            ) {
                val artworkShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(8.dp, artworkShape, clip = false)
                        .clip(artworkShape)
                        .background(Brush.linearGradient(playlist.colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_music_note),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.30f),
                        modifier = Modifier.size(56.dp),
                    )
                    Text(
                        text = playlist.meta,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = playlist.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = playlist.description,
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
private fun RecentTrackRow(
    track: HomeRecentTrack,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(track.color, TideTunesBrand.Secondary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = track.title,
                    color = MiuixTheme.colorScheme.onBackground,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                track.qualityBadge?.let { QualityBadge(type = it) }
            }
            Text(
                text = track.subtitle,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "3:42",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
            if (track.liked) {
                Icon(
                    painter = painterResource(Res.drawable.icon_heart),
                    tint = MiuixTheme.colorScheme.primary,
                    contentDescription = "Liked",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun String.initials(): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { word -> word.first().uppercase() }
        .ifBlank { take(2).uppercase() }
}

@Composable
private fun homeOverviewLayout(windowSizeClass: WindowSizeClass): HomeOverviewLayout {
    val spacing = TideTunesTokens.spacing
    return when (windowSizeClass) {
        WindowSizeClass.Compact -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 8.dp,
            bottomPadding = 16.dp,
            heroHeight = 208.dp,
            albumCardSize = 160.dp,
            smallAlbumCardSize = 120.dp,
        )
        WindowSizeClass.Medium -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 8.dp,
            bottomPadding = 16.dp,
            heroHeight = 208.dp,
            albumCardSize = 160.dp,
            smallAlbumCardSize = 120.dp,
        )
        WindowSizeClass.Expanded -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 8.dp,
            bottomPadding = 16.dp,
            heroHeight = 208.dp,
            albumCardSize = 160.dp,
            smallAlbumCardSize = 120.dp,
        )
        WindowSizeClass.Large -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 8.dp,
            bottomPadding = 16.dp,
            heroHeight = 208.dp,
            albumCardSize = 160.dp,
            smallAlbumCardSize = 120.dp,
        )
        WindowSizeClass.XL -> HomeOverviewLayout(
            horizontalPadding = spacing.pageCompact,
            topPadding = 8.dp,
            bottomPadding = 16.dp,
            heroHeight = 208.dp,
            albumCardSize = 160.dp,
            smallAlbumCardSize = 120.dp,
        )
    }
}

private data class HomeOverviewLayout(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val heroHeight: Dp,
    val albumCardSize: Dp,
    val smallAlbumCardSize: Dp,
)

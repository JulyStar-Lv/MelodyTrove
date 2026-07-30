package io.github.julystar.musicapp.feature.album.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.presentation.components.DesignCardSurface
import io.github.julystar.musicapp.core.presentation.components.DesignDetailHeaderSurface
import io.github.julystar.musicapp.core.presentation.components.DesignSectionHeader
import io.github.julystar.musicapp.core.presentation.components.DesignStatusCard
import io.github.julystar.musicapp.core.presentation.components.DesignTextButton
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonSize
import io.github.julystar.musicapp.core.presentation.components.DesignTextButtonVariant
import io.github.julystar.musicapp.core.presentation.components.DesignTrackNumberBadge
import io.github.julystar.musicapp.core.presentation.components.LocalDesignBottomContentInset
import io.github.julystar.musicapp.core.presentation.media.ArtworkImage
import io.github.julystar.musicapp.core.presentation.theme.DesignTokens
import musicapp.feature.album.generated.resources.Res
import musicapp.feature.album.generated.resources.album_default_title
import musicapp.feature.album.generated.resources.album_download
import musicapp.feature.album.generated.resources.album_loading
import musicapp.feature.album.generated.resources.album_no_tracks
import musicapp.feature.album.generated.resources.album_play_all
import musicapp.feature.album.generated.resources.album_retry
import musicapp.feature.album.generated.resources.album_song_count
import musicapp.feature.album.generated.resources.album_summary
import musicapp.feature.album.generated.resources.album_tracks
import musicapp.feature.album.generated.resources.album_unavailable
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AlbumScreen(
    state: AlbumState,
    onAction: (AlbumAction) -> Unit,
) {
    val spacing = DesignTokens.spacing
    val bottomContentInset = LocalDesignBottomContentInset.current
    val defaultTitle = stringResource(Res.string.album_default_title)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 600.dp) spacing.pageCompact else spacing.pageExpanded

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = horizontalPadding, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            when {
                state.isLoading -> DesignStatusCard(
                    title = stringResource(Res.string.album_loading),
                    message = state.title.ifBlank { defaultTitle },
                    loading = true,
                    modifier = Modifier.weight(1f),
                )
                state.error != null -> DesignStatusCard(
                    title = stringResource(Res.string.album_unavailable),
                    message = state.error,
                    modifier = Modifier.weight(1f),
                    actionText = stringResource(Res.string.album_retry),
                    onAction = { onAction(AlbumAction.Retry) },
                )
                state.tracks.isEmpty() -> {
                    AlbumHeader(
                        title = state.title.ifBlank { defaultTitle },
                        artist = state.artist,
                        artwork = state.artwork,
                        trackCount = 0,
                        totalDurationMs = 0L,
                        onPlayAll = null,
                    )
                    DesignStatusCard(
                        title = stringResource(Res.string.album_no_tracks),
                        message = state.title.ifBlank { defaultTitle },
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = spacing.xl + bottomContentInset),
                    ) {
                        item {
                            AlbumHeader(
                                title = state.title,
                                artist = state.artist,
                                artwork = state.artwork,
                                trackCount = state.tracks.size,
                                totalDurationMs = state.tracks.sumOf { it.durationMs ?: 0L },
                                onPlayAll = { onAction(AlbumAction.PlayAll) },
                            )
                        }
                        item {
                            DesignSectionHeader(
                                title = stringResource(Res.string.album_tracks),
                                metadata = stringResource(Res.string.album_song_count, state.tracks.size),
                                titleWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        itemsIndexed(state.tracks) { _, track ->
                            AlbumTrackRow(
                                track = track,
                                onPlay = { onAction(AlbumAction.PlayTrack(track.id)) },
                                onDownload = {
                                    if (track.canDownload) {
                                        onAction(AlbumAction.DownloadTrack(track))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    title: String,
    artist: String,
    artwork: Artwork?,
    trackCount: Int,
    totalDurationMs: Long,
    onPlayAll: (() -> Unit)?,
) {
    val shapes = DesignTokens.shapes

    DesignDetailHeaderSurface {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(shapes.lg))
                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        ) {
            ArtworkImage(artwork = artwork, modifier = Modifier.fillMaxSize())
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 620.dp),
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
            Text(
                text = albumSummary(trackCount = trackCount, totalDurationMs = totalDurationMs),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        onPlayAll?.let {
            DesignTextButton(
                text = stringResource(Res.string.album_play_all),
                variant = DesignTextButtonVariant.PrimaryFilled,
                size = DesignTextButtonSize.Medium,
                onClick = it,
            )
        }
    }
}

@Composable
private fun AlbumTrackRow(
    track: AlbumTrackItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val trackLabel = track.trackLabel()

    DesignCardSurface(
        modifier = Modifier.heightIn(min = 58.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DesignTrackNumberBadge(label = trackLabel)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = track.title,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                track.durationMs?.let {
                    Text(
                        text = durationLabel(it),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
            }
            if (track.canDownload) {
                DesignTextButton(
                    text = stringResource(Res.string.album_download),
                    variant = DesignTextButtonVariant.Default,
                    size = DesignTextButtonSize.Small,
                    onClick = onDownload,
                )
            }
        }
    }
}

private fun AlbumTrackItem.trackLabel(): String = buildString {
    if (discNumber != null && discNumber > 1) {
        append("$discNumber.")
    }
    if (trackNumber != null) {
        if (isNotEmpty()) append("-")
        append("$trackNumber")
    }
}

@Composable
private fun albumSummary(trackCount: Int, totalDurationMs: Long): String {
    return if (totalDurationMs > 0) {
        stringResource(Res.string.album_summary, trackCount, durationLabel(totalDurationMs))
    } else {
        stringResource(Res.string.album_song_count, trackCount)
    }
}

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

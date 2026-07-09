package com.github.tidetunes.feature.album.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.presentation.components.TideCardSurface
import com.github.tidetunes.core.presentation.components.TideDetailHeaderSurface
import com.github.tidetunes.core.presentation.components.TideSectionHeader
import com.github.tidetunes.core.presentation.components.TideStatusCard
import com.github.tidetunes.core.presentation.components.TideTrackNumberBadge
import com.github.tidetunes.core.presentation.components.TideTextButton
import com.github.tidetunes.core.presentation.components.TideTextButtonSize
import com.github.tidetunes.core.presentation.components.TideTextButtonVariant
import com.github.tidetunes.core.presentation.media.ArtworkImage
import com.github.tidetunes.core.presentation.theme.TideTunesTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AlbumScreen(
    state: AlbumState,
    onAction: (AlbumAction) -> Unit,
) {
    val spacing = TideTunesTokens.spacing
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
                state.isLoading -> TideStatusCard(
                    title = "Loading album",
                    message = state.title.ifBlank { "Album" },
                    loading = true,
                    modifier = Modifier.weight(1f),
                )
                state.error != null -> TideStatusCard(
                    title = "Album unavailable",
                    message = state.error,
                    modifier = Modifier.weight(1f),
                    actionText = "Retry",
                    onAction = { onAction(AlbumAction.Retry) },
                )
                state.tracks.isEmpty() -> {
                    AlbumHeader(
                        title = state.title.ifBlank { "Album" },
                        artist = state.artist,
                        artwork = state.artwork,
                        trackCount = 0,
                        totalDurationMs = 0L,
                        onPlayAll = null,
                    )
                    TideStatusCard(
                        title = "No tracks",
                        message = state.title.ifBlank { "Album" },
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = spacing.xl),
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
                            TideSectionHeader(
                                title = "Tracks",
                                metadata = "${state.tracks.size} songs",
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
    val shapes = TideTunesTokens.shapes

    TideDetailHeaderSurface {
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
            TideTextButton(
                text = "Play All",
                variant = TideTextButtonVariant.PrimaryFilled,
                size = TideTextButtonSize.Medium,
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

    TideCardSurface(
        modifier = Modifier.heightIn(min = 58.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onPlay,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TideTrackNumberBadge(label = trackLabel)
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
                TideTextButton(
                    text = "DL",
                    variant = TideTextButtonVariant.Default,
                    size = TideTextButtonSize.Small,
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

private fun albumSummary(trackCount: Int, totalDurationMs: Long): String {
    val countLabel = if (trackCount == 1) "1 song" else "$trackCount songs"
    return if (totalDurationMs > 0) {
        "$countLabel, ${durationLabel(totalDurationMs)}"
    } else {
        countLabel
    }
}

private fun durationLabel(durationMs: Long): String {
    val h = durationMs / 1000 / 60 / 60
    val m = durationMs / 1000 / 60 % 60
    val s = durationMs / 1000 % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

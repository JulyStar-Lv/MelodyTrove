package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTouchTargets
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.LibraryTrackItem
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository

@Composable
fun CarSongRow(
    track: LibraryTrackItem,
    index: Int,
    playing: Boolean,
    enabled: Boolean = true,
    height: Dp,
    artworkRepository: ArtworkRepository? = null,
    showArtwork: Boolean = false,
    showAlbum: Boolean = false,
    showQuality: Boolean = false,
    showDuration: Boolean = true,
    showActions: Boolean = false,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val spacing = LocalCarSpacing.current
    val actionSize = maxOf(LocalCarTouchTargets.current.minimum, height * 0.56f)
    val contentColor = when {
        !enabled -> colors.textDisabled
        playing -> colors.accentPrimary
        else -> colors.textPrimary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (playing) stateDescription = "正在播放" }
            .carInteractiveSurface(
                shape = LocalCarShapes.current.navigationItem,
                playing = playing,
                enabled = enabled,
                defaultColor = colors.backgroundSubtle,
                onClick = onClick,
            )
            .padding(horizontal = spacing.section),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(height * 0.5f).fillMaxHeight()) {
            if (playing) PlayingIndicator(contentColor) else BasicText(
                text = (index + 1).toString(),
                style = LocalCarTypography.current.body.copy(color = contentColor),
            )
        }
        if (showArtwork && artworkRepository != null) {
            CarArtwork(
                artwork = track.albumId?.let(Artwork::LibraryAlbum) ?: Artwork.LibraryCover(track.id),
                repository = artworkRepository,
                size = height * 0.64f,
                shape = LocalCarShapes.current.artwork,
            )
            Spacer(Modifier.width(spacing.content))
        }
        Column(Modifier.weight(if (showAlbum || showQuality) 1.3f else 1f)) {
            BasicText(
                text = track.title,
                style = LocalCarTypography.current.bodyLarge.copy(color = contentColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = track.artist.orEmpty(),
                style = LocalCarTypography.current.body.copy(color = if (enabled) colors.textSecondary else colors.textDisabled),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showAlbum) {
            BasicText(
                text = track.albumName.orEmpty(),
                style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.8f),
            )
        }
        if (showQuality) {
            BasicText(
                text = track.carAudioQuality(),
                style = LocalCarTypography.current.body.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.8f),
            )
        }
        if (showDuration) {
            BasicText(
                text = track.durationMs.formatDuration(),
                style = LocalCarTypography.current.body.copy(color = colors.textSummary),
                modifier = Modifier.width(height),
            )
        }
        if (showActions) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(actionSize).carInteractiveSurface(
                    LocalCarShapes.current.control,
                    selected = favorite,
                    enabled = onToggleFavorite != null,
                    onClick = onToggleFavorite ?: {},
                ),
            ) {
                CarIconView(
                    CarIcon.Heart,
                    if (onToggleFavorite != null) if (favorite) "取消收藏" else "收藏" else null,
                    if (favorite) colors.accentPrimary else colors.textSecondary,
                    Modifier.size(actionSize * 0.61f),
                )
            }
            Spacer(Modifier.width(spacing.small))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(actionSize)) {
                CarIconView(CarIcon.More, null, colors.textDisabled, Modifier.size(actionSize * 0.61f))
            }
        }
    }
}

@Composable
private fun PlayingIndicator(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "playing indicator")
    val initialFractions = floatArrayOf(0.35f, 0.7f, 0.5f, 0.8f)
    Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.size(22.dp, 16.dp),
    ) {
        initialFractions.forEachIndexed { index, initialFraction ->
            val fraction by transition.animateFloat(
                initialValue = initialFraction,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "playing bar $index",
            )
            Box(
                Modifier
                    .width(4.dp)
                    .height(16.dp * fraction)
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
        }
    }
}

private fun LibraryTrackItem.carAudioQuality(): String {
    val format = codec?.uppercase()?.takeIf(String::isNotBlank)
    val depth = bitDepth
    val rate = sampleRateHz
    val depthAndRate = when {
        depth != null && rate != null -> "$depth/${rate / 1000}"
        depth != null -> "$depth-bit"
        rate != null -> "${rate / 1000} kHz"
        else -> null
    }
    return listOfNotNull(format, depthAndRate).joinToString(" · ").ifBlank { "—" }
}

private fun Long?.formatDuration(): String {
    val totalSeconds = (this ?: 0L).coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

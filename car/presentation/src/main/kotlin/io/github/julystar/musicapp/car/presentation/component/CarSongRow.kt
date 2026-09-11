package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as CarIconView
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
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
    showActions: Boolean = false,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val spacing = LocalCarSpacing.current
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
            BasicText(
                text = if (playing) "♪" else (index + 1).toString(),
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
        Column(Modifier.weight(if (showAlbum) 1.3f else 1f)) {
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
        BasicText(
            text = track.durationMs.formatDuration(),
            style = LocalCarTypography.current.body.copy(color = colors.textSummary),
            modifier = Modifier.width(height),
        )
        if (showActions) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(height * 0.56f).carInteractiveSurface(
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
                    Modifier.size(height * 0.34f),
                )
            }
            Spacer(Modifier.width(spacing.small))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(height * 0.56f)) {
                CarIconView(CarIcon.More, null, colors.textDisabled, Modifier.size(height * 0.34f))
            }
        }
    }
}

private fun Long?.formatDuration(): String {
    val totalSeconds = (this ?: 0L).coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

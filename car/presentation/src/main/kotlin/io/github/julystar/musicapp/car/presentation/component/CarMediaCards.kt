package io.github.julystar.musicapp.car.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.julystar.musicapp.car.presentation.icon.CarIcon
import io.github.julystar.musicapp.car.presentation.icon.CarIcon as IconView
import io.github.julystar.musicapp.car.presentation.theme.LocalCarColors
import io.github.julystar.musicapp.car.presentation.theme.LocalCarShapes
import io.github.julystar.musicapp.car.presentation.theme.LocalCarSpacing
import io.github.julystar.musicapp.car.presentation.theme.LocalCarTypography
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.domain.model.LibraryAlbumItem
import io.github.julystar.musicapp.core.domain.model.LibraryArtistItem
import io.github.julystar.musicapp.core.domain.repository.ArtworkRepository

@Composable
fun CarAlbumCard(
    album: LibraryAlbumItem,
    artworkRepository: ArtworkRepository,
    artworkSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val shapes = LocalCarShapes.current
    val spacing = LocalCarSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shapes.card)
            .border(1.dp, colors.borderSubtle, shapes.card)
            .carInteractiveSurface(shapes.card, defaultColor = colors.backgroundSubtle, onClick = onClick)
            .padding(spacing.compact),
    ) {
        CarArtwork(
            artwork = Artwork.LibraryAlbum(album.id),
            repository = artworkRepository,
            size = artworkSize,
            shape = shapes.artwork,
        )
        Spacer(Modifier.size(spacing.compact))
        BasicText(
            text = album.name,
            style = LocalCarTypography.current.body.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        BasicText(
            text = album.artist.orEmpty(),
            style = LocalCarTypography.current.supporting.copy(color = colors.textSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun CarArtistCard(
    artist: LibraryArtistItem,
    artwork: Artwork?,
    artworkRepository: ArtworkRepository,
    artworkSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val shapes = LocalCarShapes.current
    val spacing = LocalCarSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shapes.card)
            .border(1.dp, colors.borderSubtle, shapes.card)
            .carInteractiveSurface(shapes.card, defaultColor = colors.backgroundSubtle, onClick = onClick)
            .padding(spacing.compact),
    ) {
        CarArtwork(
            artwork = artwork,
            repository = artworkRepository,
            size = artworkSize,
            shape = androidx.compose.foundation.shape.CircleShape,
        )
        Spacer(Modifier.size(spacing.compact))
        BasicText(
            text = artist.name,
            style = LocalCarTypography.current.bodyLarge.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CarQuickActionCard(
    title: String,
    summary: String,
    icon: CarIcon,
    tileSize: Dp,
    iconSize: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCarColors.current
    val spacing = LocalCarSpacing.current
    val shape = LocalCarShapes.current.card
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .carInteractiveSurface(shape, enabled = enabled, defaultColor = colors.panel, onClick = onClick)
            .padding(spacing.content),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(tileSize).clip(LocalCarShapes.current.control),
        ) {
            IconView(icon, null, if (enabled) colors.accentPrimary else colors.textDisabled, Modifier.size(iconSize))
        }
        Spacer(Modifier.width(spacing.content))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = LocalCarTypography.current.title.copy(color = colors.textPrimary))
            BasicText(
                summary,
                style = LocalCarTypography.current.supporting.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

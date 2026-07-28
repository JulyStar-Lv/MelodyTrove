package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.julystar.musicapp.core.domain.model.Artwork
import io.github.julystar.musicapp.core.presentation.media.ArtworkImage

@Composable
fun MusicCover(
    modifier: Modifier,
    artwork: Artwork?,
) {
    ArtworkImage(
        modifier = modifier,
        artwork = artwork,
    )
}

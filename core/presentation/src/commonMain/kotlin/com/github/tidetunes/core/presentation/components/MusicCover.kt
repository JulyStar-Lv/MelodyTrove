package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.tidetunes.core.domain.model.Artwork
import com.github.tidetunes.core.presentation.media.ArtworkImage

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

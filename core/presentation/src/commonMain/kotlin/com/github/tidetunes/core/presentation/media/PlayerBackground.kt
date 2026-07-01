package com.github.tidetunes.core.presentation.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Extracts a dominant background color from artwork for dynamic player backgrounds.
 * Platform implementations can use native image processing for better results.
 */
fun interface PlayerBackgroundColorExtractor {
    fun extract(bitmap: ImageBitmap): Color
}

/**
 * Default no-op extractor that returns a neutral dark background.
 */
object FallbackPlayerBackgroundColorExtractor : PlayerBackgroundColorExtractor {
    override fun extract(bitmap: ImageBitmap): Color = Color(0xFF1A1A2E)
}

/**
 * Renders a dynamic background for the Now Playing screen.
 * Uses the provided color extractor to derive a background color from the current artwork.
 */
@Composable
fun PlayerBackground(
    artworkBitmap: ImageBitmap?,
    colorExtractor: PlayerBackgroundColorExtractor = FallbackPlayerBackgroundColorExtractor,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color(0xFF1A1A2E),
) {
    val targetColor = artworkBitmap?.let { colorExtractor.extract(it) } ?: fallbackColor
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedColor),
    )
}

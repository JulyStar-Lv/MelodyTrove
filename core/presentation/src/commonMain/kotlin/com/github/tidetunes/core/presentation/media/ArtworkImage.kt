package com.github.tidetunes.core.presentation.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.github.tidetunes.core.domain.model.Artwork
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.cover_default_image
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArtworkImage(
    modifier: Modifier,
    artwork: Artwork?,
    contentScale: ContentScale = ContentScale.FillWidth,
    loader: ArtworkImageLoader = koinInject(),
) {
    Box(modifier = modifier) {
        if (artwork == null) {
            DefaultArtworkImage()
            return@Box
        }

        var bitmap: ImageBitmap? by remember(artwork) {
            mutableStateOf(loader.cachedBitmap(artwork))
        }

        LaunchedEffect(artwork) {
            if (bitmap == null) {
                bitmap = loader.loadBitmap(artwork)
            }
        }

        if (bitmap == null) {
            DefaultArtworkImage()
        } else {
            Image(
                modifier = Modifier
                    .background(MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    .fillMaxSize(),
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = contentScale,
            )
        }
    }
}

@Composable
private fun DefaultArtworkImage() {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(Res.drawable.cover_default_image),
        contentDescription = null,
    )
}

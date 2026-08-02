package io.github.julystar.musicapp.core.presentation.media

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
import io.github.julystar.musicapp.core.domain.model.Artwork
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import musicapp.core.presentation.generated.resources.Res
import musicapp.core.presentation.generated.resources.cover_default_image
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArtworkImage(
    modifier: Modifier,
    artwork: Artwork?,
    contentScale: ContentScale = ContentScale.FillWidth,
    loader: ArtworkImageLoader = koinInject(),
    fallback: @Composable () -> Unit = { DefaultArtworkImage() },
) {
    Box(modifier = modifier) {
        if (artwork == null) {
            fallback()
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
            fallback()
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

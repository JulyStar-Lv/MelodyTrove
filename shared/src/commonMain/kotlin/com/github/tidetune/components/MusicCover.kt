package com.github.tidetune.components

import com.github.tidetune.components.TideTuneImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import tidetune.shared.generated.resources.Res
import tidetune.shared.generated.resources.cover_default_image
import org.jetbrains.compose.resources.painterResource
import uniffi.tidetune_core.DataSourceKey

@Composable
fun MusicCover(
    modifier: Modifier,
    coverDataSourceKey: DataSourceKey?
) {
    Box(
        modifier = modifier,
    ) {
        if (coverDataSourceKey == null) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(Res.drawable.cover_default_image), // Replace with actual image resource
                contentDescription = null,
            )
        } else {
            TideTuneImage(
                modifier = Modifier.background(MaterialTheme.colorScheme.onSurfaceVariant).fillMaxSize(),
                dataSourceKey = coverDataSourceKey,
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

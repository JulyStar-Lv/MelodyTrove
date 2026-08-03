package io.github.julystar.musicapp.platform

import androidx.compose.runtime.Composable
import io.github.julystar.musicapp.core.presentation.platform.PlatformBackHandler

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    PlatformBackHandler(enabled = enabled, onBack = onBack)
}

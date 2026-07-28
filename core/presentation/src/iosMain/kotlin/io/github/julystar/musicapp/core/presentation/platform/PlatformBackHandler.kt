package io.github.julystar.musicapp.core.presentation.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS back navigation is owned by the host navigation gesture.
}

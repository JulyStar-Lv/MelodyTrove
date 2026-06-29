package com.github.tidetunes.core.presentation.platform

import androidx.compose.runtime.Composable

@Composable
actual fun TideTunesBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS back navigation is owned by the host navigation gesture.
}

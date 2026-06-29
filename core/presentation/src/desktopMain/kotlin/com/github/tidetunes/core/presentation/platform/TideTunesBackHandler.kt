package com.github.tidetunes.core.presentation.platform

import androidx.compose.runtime.Composable

@Composable
actual fun TideTunesBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop has no system back button.
}

package com.github.tidetunes.core.presentation.platform

import androidx.compose.runtime.Composable

@Composable
expect fun TideTunesBackHandler(enabled: Boolean = true, onBack: () -> Unit)

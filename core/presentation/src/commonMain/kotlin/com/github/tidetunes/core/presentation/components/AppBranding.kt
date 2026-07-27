package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import tidetunes.core.presentation.generated.resources.Res
import tidetunes.core.presentation.generated.resources.app_icon

@Composable
fun tideTunesAppIconPainter(): Painter = painterResource(Res.drawable.app_icon)

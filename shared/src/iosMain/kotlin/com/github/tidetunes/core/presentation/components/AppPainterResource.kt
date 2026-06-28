package com.github.tidetunes.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun appPainterResource(resource: DrawableResource): Painter = painterResource(resource)

package io.github.julystar.musicapp.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import musicapp.core.presentation.generated.resources.Res
import musicapp.core.presentation.generated.resources.app_icon

@Composable
fun appIconPainter(): Painter = painterResource(Res.drawable.app_icon)

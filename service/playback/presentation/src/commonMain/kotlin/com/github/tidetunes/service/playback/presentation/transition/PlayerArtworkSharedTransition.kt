@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.github.tidetunes.service.playback.presentation.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.github.tidetunes.core.presentation.theme.TideTunesTokens

val LocalPlayerArtworkSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalPlayerArtworkAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.playerArtworkSharedElement(): Modifier {
    val sharedTransitionScope = LocalPlayerArtworkSharedTransitionScope.current ?: return this
    val animatedVisibilityScope = LocalPlayerArtworkAnimatedVisibilityScope.current ?: return this
    val durationMillis = TideTunesTokens.motion.playerExpandMillis

    return with(sharedTransitionScope) {
        sharedElement(
            sharedContentState = rememberSharedContentState(PlayerArtworkSharedElementKey),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = BoundsTransform { _, _ ->
                tween(
                    durationMillis = durationMillis,
                    easing = CubicBezierEasing(0.32f, 0f, 0.15f, 1f),
                )
            },
        )
    }
}

private const val PlayerArtworkSharedElementKey = "player-artwork"

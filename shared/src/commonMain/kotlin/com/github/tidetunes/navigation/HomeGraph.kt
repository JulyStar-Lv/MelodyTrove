package com.github.tidetunes.navigation

import com.github.tidetunes.core.presentation.navigation.MusicGraph
import com.github.tidetunes.service.playback.presentation.transition.LocalPlayerArtworkAnimatedVisibilityScope

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.homeGraph(
    scaffoldPadding: PaddingValues,
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onOpenQueue: () -> Unit,
) {
    composable<MusicGraph.Home> {
        val animatedVisibilityScope = this
        CompositionLocalProvider(
            LocalPlayerArtworkAnimatedVisibilityScope provides animatedVisibilityScope,
        ) {
            HomePage(
                scaffoldPadding = scaffoldPadding,
                currentTab = currentTab,
                onTabSelected = onTabSelected,
                onOpenQueue = onOpenQueue,
            )
        }
    }
}

package com.github.tidetunes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.github.tidetunes.widgets.ToastFrame

@Composable
fun AppNavigation(
    navController: NavHostController,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(
                    start = scaffoldPadding.calculateLeftPadding(LayoutDirection.Ltr),
                    end = scaffoldPadding.calculateRightPadding(LayoutDirection.Ltr),
                    top = scaffoldPadding.calculateTopPadding(),
                )
                .fillMaxSize()
        ) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = MusicGraph.Home,
                enterTransition = {
                    slideIn(
                        animationSpec = tween(300),
                        initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
                    )
                },
                exitTransition = {
                    slideOut(
                        animationSpec = tween(300),
                        targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
                    )
                },
                popEnterTransition = {
                    slideIn(
                        animationSpec = tween(300),
                        initialOffset = { fullSize -> IntOffset(fullSize.width, 0) },
                    )
                },
                popExitTransition = {
                    slideOut(
                        animationSpec = tween(300),
                        targetOffset = { fullSize -> IntOffset(-fullSize.width, 0) },
                    )
                },
            ) {
                homeGraph(scaffoldPadding)
                albumGraph(navController)
                artistGraph(navController)
                browseGraph(navController)
                radioGraph(navController)
                recentlyAddedGraph(navController)
                recentlyPlayedGraph(navController)
                onboardingGraph(navController)
                lyricsGraph(navController)
                queueGraph(navController)
                sourcesGraph(navController)
                libraryGraph(navController, scaffoldPadding)
                searchGraph()
                downloadsGraph()
                playerGraph(navController)
                settingsGraph()
            }
            ToastFrame()
        }
    }
}
